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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.AlertRepository
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.data.NeptunClient
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.data.WearSyncManager
import ua.alerts.mobile.notification.NotificationHelper
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.NeptunAlertsResponse

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
    private var hasInitialized = false
    private var lastAlarmState: Boolean? = null
    private var lastAlarmLevel: String? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        neptunClient = NeptunClient()
        wearSyncManager = WearSyncManager(applicationContext, serviceScope) {
            AlertRepository.alertStatus.value
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
                // ACTION_START or system restart with null intent
                startMonitoring()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep service running even when task is removed from Recents
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

        // Observe incoming alerts and compute status for the selected region
        monitoringJob = serviceScope.launch {
            combine(
                neptunClient.alertsFlow,
                settingsRepo.regionId,
                settingsRepo.regionName,
                settingsRepo.districtId,
                settingsRepo.districtName
            ) { alerts, regId, regName, distId, distName ->
                computeAlertStatus(alerts, regId, regName, distId, distName)
            }.collect { newStatus ->
                val prevAlarm = lastAlarmState
                val prevLevel = lastAlarmLevel
                AlertRepository.setAlertStatus(newStatus)

                // Update ongoing service notification
                val notification = notificationHelper.buildServiceNotification(
                    status = newStatus,
                    connectionStatus = neptunClient.connectionStatus.value
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

                // Sync status with Galaxy Watch
                wearSyncManager.syncAlertStatus(newStatus)

                // Trigger sound/vibration on state transition or level escalation (Yellow -> Red)
                val isEscalation = prevAlarm == true && newStatus.isAlarm &&
                        prevLevel.equals("yellow", ignoreCase = true) &&
                        newStatus.isRed
                val isStateTransition = prevAlarm != null && prevAlarm != newStatus.isAlarm

                if (hasInitialized && (isStateTransition || isEscalation)) {
                    val soundAlarm = settingsRepo.soundOnAlarm.first()
                    val vibrateAlarm = settingsRepo.vibrateOnAlarm.first()
                    val soundClear = settingsRepo.soundOnClear.first()
                    val vibrateClear = settingsRepo.vibrateOnClear.first()

                    val sound = if (newStatus.isAlarm) soundAlarm else soundClear
                    val vibrate = if (newStatus.isAlarm) vibrateAlarm else vibrateClear

                    notificationHelper.notifyAlarmTransition(newStatus, sound, vibrate)
                }

                lastAlarmState = newStatus.isAlarm
                lastAlarmLevel = newStatus.level
                hasInitialized = true
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
        regionId: String,
        regionName: String,
        districtId: String?,
        districtName: String?
    ): AlertStatus {
        return DefaultRegions.computeAlertStatus(alerts, regionId, regionName, districtId, districtName)
    }
}
