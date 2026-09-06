package ua.alerts.mobile.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.data.NeptunClient
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.data.WearSyncManager
import ua.alerts.mobile.notification.NotificationHelper
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.NeptunAlertsResponse

class AlertMonitoringService : Service() {

    companion object {
        const val ACTION_START = "ua.alerts.mobile.action.START"
        const val ACTION_STOP = "ua.alerts.mobile.action.STOP"
        const val ACTION_REFRESH = "ua.alerts.mobile.action.REFRESH"

        fun startService(context: Context) {
            val intent = Intent(context, AlertMonitoringService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AlertMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun refresh(context: Context) {
            val intent = Intent(context, AlertMonitoringService::class.java).apply {
                action = ACTION_REFRESH
            }
            context.startService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        val service: AlertMonitoringService get() = this@AlertMonitoringService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var neptunClient: NeptunClient
    private lateinit var wearSyncManager: WearSyncManager

    private val _currentStatus = MutableStateFlow(AlertStatus())
    val currentStatus: StateFlow<AlertStatus> = _currentStatus.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus>
        get() = neptunClient.connectionStatus

    val connectedWatchCount: StateFlow<Int>
        get() = wearSyncManager.connectedWatchCount

    private var monitoringJob: Job? = null
    private var hasInitialized = false

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        neptunClient = NeptunClient()
        wearSyncManager = WearSyncManager(applicationContext, serviceScope) {
            _currentStatus.value
        }

        wearSyncManager.start()

        // Start foreground notification immediately
        startForeground(
            NotificationHelper.NOTIFICATION_ID_SERVICE,
            notificationHelper.buildServiceNotification(_currentStatus.value)
        )

        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
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

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        monitoringJob?.cancel()
        neptunClient.stop()
        wearSyncManager.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        neptunClient.start()

        monitoringJob = serviceScope.launch {
            // Combine active alerts from Neptun and user selected region
            combine(
                neptunClient.alertsFlow,
                settingsRepo.regionId,
                settingsRepo.regionName,
                settingsRepo.districtId,
                settingsRepo.districtName
            ) { alerts, regId, regName, distId, distName ->
                computeAlertStatus(alerts, regId, regName, distId, distName)
            }.collect { newStatus ->
                val prevStatus = _currentStatus.value
                _currentStatus.value = newStatus

                // Update ongoing service notification
                val notification = notificationHelper.buildServiceNotification(newStatus)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

                // Sync status with Galaxy Watch
                wearSyncManager.syncAlertStatus(newStatus)

                // If state transitioned and not initial run, trigger sound/vibration
                if (hasInitialized && prevStatus.isAlarm != newStatus.isAlarm) {
                    val soundAlarm = settingsRepo.soundOnAlarm.first()
                    val vibrateAlarm = settingsRepo.vibrateOnAlarm.first()
                    val soundClear = settingsRepo.soundOnClear.first()
                    val vibrateClear = settingsRepo.vibrateOnClear.first()

                    val sound = if (newStatus.isAlarm) soundAlarm else soundClear
                    val vibrate = if (newStatus.isAlarm) vibrateAlarm else vibrateClear

                    notificationHelper.notifyAlarmTransition(newStatus, sound, vibrate)
                }

                hasInitialized = true
            }
        }
    }

    private fun computeAlertStatus(
        alerts: NeptunAlertsResponse,
        regionId: String,
        regionName: String,
        districtId: String?,
        districtName: String?
    ): AlertStatus {
        var isAlarm = false
        var sinceTime: String? = null

        if (districtId != null) {
            // Check specific district alert first
            val matchingRaion = alerts.raions.firstOrNull { it.key == districtId }
            if (matchingRaion != null) {
                isAlarm = true
                sinceTime = matchingRaion.since
            } else {
                // Check whole oblast alert
                val matchingOblast = alerts.oblasts.firstOrNull { it.key == regionId }
                if (matchingOblast != null) {
                    isAlarm = true
                    sinceTime = matchingOblast.since
                }
            }
        } else {
            // Entire oblast selected
            val matchingOblast = alerts.oblasts.firstOrNull { it.key == regionId }
            if (matchingOblast != null) {
                isAlarm = true
                sinceTime = matchingOblast.since
            } else {
                // Check if any raion in this oblast has an alert
                val matchingRaion = alerts.raions.firstOrNull {
                    it.key.startsWith("$regionId:") ||
                    it.oblast.contains(regionName, ignoreCase = true)
                }
                if (matchingRaion != null) {
                    isAlarm = true
                    sinceTime = matchingRaion.since
                }
            }
        }

        return AlertStatus(
            isAlarm = isAlarm,
            regionKey = regionId,
            regionName = regionName,
            districtKey = districtId,
            districtName = districtName,
            since = sinceTime,
            updatedAt = System.currentTimeMillis()
        )
    }
}
