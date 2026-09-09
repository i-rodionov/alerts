package ua.alerts.mobile.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.AlertRepository
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.data.NeptunClient
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.data.WearSyncManager
import ua.alerts.mobile.notification.NotificationHelper
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.AlertLevel
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.AlertTransitionEvaluator
import ua.alerts.shared.model.toAlertLevel
import ua.alerts.shared.model.NeptunAlertsResponse
import ua.alerts.shared.model.Profile
import ua.alerts.shared.model.WatchSyncData

private data class MonitoringState(
    val alerts: NeptunAlertsResponse,
    val profiles: List<Profile>,
    val isGlobalEnabled: Boolean,
    val appLanguage: String
)

class AlertForegroundService : Service() {

    companion object {
        const val ACTION_START = "ua.alerts.mobile.action.START"
        const val ACTION_STOP = "ua.alerts.mobile.action.STOP"
        const val ACTION_REFRESH = "ua.alerts.mobile.action.REFRESH"

        fun startService(context: Context) {
            val intent = Intent(context, AlertForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AlertForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun refresh(context: Context) {
            val intent = Intent(context, AlertForegroundService::class.java).apply {
                action = ACTION_REFRESH
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var neptunClient: NeptunClient
    private lateinit var wearSyncManager: WearSyncManager

    private var monitoringJob: Job? = null
    private var connectionStatusJob: Job? = null
    private var watchCountJob: Job? = null
    private val profileAlertLevels = mutableMapOf<String, AlertLevel>()

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        neptunClient = NeptunClient()
        wearSyncManager = WearSyncManager(applicationContext, serviceScope) {
            AlertRepository.watchSyncData.value
        }

        wearSyncManager.start()

        // Promptly start foreground notification to satisfy Android 14+ / SDK 35 requirements
        val initialNotification = notificationHelper.buildServiceNotification(
            status = AlertRepository.alertStatus.value,
            connectionStatus = ConnectionStatus.CONNECTING
        )

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_SERVICE,
            initialNotification,
            fgsType
        )

        AlertRepository.setServiceRunning(true)
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                serviceScope.launch {
                    neptunClient.fetchAlertsSnapshot()
                    wearSyncManager.refreshConnectedNodes()
                }
            }
            else -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        AlertRepository.setServiceRunning(false)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationHelper.NOTIFICATION_ID_SERVICE)
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true && neptunClient.isRunning) return

        neptunClient.start()

        // Observe connection status changes
        if (connectionStatusJob == null || !connectionStatusJob!!.isActive) {
            connectionStatusJob = serviceScope.launch {
                neptunClient.connectionStatus.collect { status ->
                    AlertRepository.setConnectionStatus(status, neptunClient.lastError.value)
                    val notification = notificationHelper.buildServiceNotification(
                        status = AlertRepository.alertStatus.value,
                        connectionStatus = status
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
                }
            }
        }

        // Observe connected watch count
        if (watchCountJob == null || !watchCountJob!!.isActive) {
            watchCountJob = serviceScope.launch {
                wearSyncManager.connectedWatchCount.collect { count ->
                    AlertRepository.setConnectedWatchCount(count)
                }
            }
        }

        // Observe incoming alerts and compute status for all profiles
        monitoringJob = serviceScope.launch {
            combine(
                neptunClient.alertsFlow,
                settingsRepo.profiles,
                settingsRepo.globalMonitoring,
                settingsRepo.appLanguage
            ) { alerts, profiles, isGlobalEnabled, appLanguage ->
                MonitoringState(alerts, profiles, isGlobalEnabled, appLanguage)
            }.collect { state ->
                val (alerts, profiles, isGlobalEnabled, appLanguage) = state
                val effectiveLanguage = if (appLanguage == "system") java.util.Locale.getDefault().language else appLanguage
                val statusMap = mutableMapOf<String, AlertStatus>()
                for (profile in profiles) {
                    val status = computeAlertStatus(alerts, profile)
                    statusMap[profile.id] = status
                }
                AlertRepository.setProfileAlerts(statusMap)

                // Synchronized watch profiles and statuses
                val synchronizedProfiles = profiles.filter { it.activeOnWatch }
                val synchronizedStatuses = synchronizedProfiles.associate { it.id to (statusMap[it.id] ?: AlertStatus()) }
                val watchSyncData = WatchSyncData(
                    profiles = synchronizedProfiles,
                    statuses = synchronizedStatuses,
                    updatedAt = System.currentTimeMillis(),
                    language = appLanguage
                )
                AlertRepository.setWatchSyncData(watchSyncData)

                // Active watch profile status (for ongoing mobile status notification)
                val watchProfile = synchronizedProfiles.firstOrNull() ?: profiles.firstOrNull()
                val watchStatus = if (watchProfile != null) {
                    statusMap[watchProfile.id] ?: AlertStatus()
                } else {
                    AlertStatus()
                }
                AlertRepository.setAlertStatus(watchStatus)

                // Update ongoing service notification
                val notification = notificationHelper.buildServiceNotification(
                    status = watchStatus,
                    connectionStatus = neptunClient.connectionStatus.value,
                    language = effectiveLanguage
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

                // Sync all synchronized profiles to Wear OS
                wearSyncManager.syncWatchData(watchSyncData)

                // Trigger sound/vibration notifications ONLY on meaningful status transitions per profile
                val currentProfileIds = profiles.map { it.id }.toSet()
                profileAlertLevels.keys.retainAll(currentProfileIds)

                for (profile in profiles) {
                    val status = statusMap[profile.id] ?: continue
                    val currentLevel = status.toAlertLevel()
                    val prevLevel = profileAlertLevels[profile.id]
                    val isMonitored = isGlobalEnabled && profile.backgroundMonitoring

                    if (prevLevel == null) {
                        // Baseline initialization (cold start, reconnect, or newly added profile).
                        // Must NEVER trigger false notification!
                        profileAlertLevels[profile.id] = currentLevel
                    } else {
                        if (AlertTransitionEvaluator.shouldNotifyTransition(prevLevel, currentLevel)) {
                            if (isMonitored) {
                                notificationHelper.notifyAlarmTransition(profile, status, effectiveLanguage)
                            }
                            profileAlertLevels[profile.id] = currentLevel
                        }
                    }
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        connectionStatusJob?.cancel()
        connectionStatusJob = null
        watchCountJob?.cancel()
        watchCountJob = null
        neptunClient.stop()
        wearSyncManager.stop()
    }

    private fun computeAlertStatus(
        alerts: NeptunAlertsResponse,
        profile: Profile
    ): AlertStatus {
        return DefaultRegions.computeAlertStatus(
            alerts,
            profile.regionId,
            profile.regionName,
            profile.districtId,
            profile.districtName
        )
    }
}