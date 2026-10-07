package ua.alerts.mobile.data

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.logging.AppLog
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.WatchSyncData

class WearSyncManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getCurrentSyncData: (() -> WatchSyncData)? = null
) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "WearSyncManager"
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun syncCurrentState(context: Context): Boolean = withContext(Dispatchers.IO) {
            try {
                val settingsRepo = SettingsRepository(context.applicationContext)
                val profiles = settingsRepo.profiles.first()
                val language = settingsRepo.appLanguage.first()
                val isRunning = AlertRepository.isServiceRunning.value

                val synchronizedProfiles = profiles.filter { it.activeOnWatch }
                val synchronizedStatuses = if (isRunning) {
                    val liveStatuses = AlertRepository.profileAlerts.value
                    synchronizedProfiles.mapNotNull { p -> liveStatuses[p.id]?.let { p.id to it } }.toMap()
                } else {
                    emptyMap()
                }

                val syncData = WatchSyncData(
                    profiles = synchronizedProfiles,
                    statuses = synchronizedStatuses,
                    updatedAt = System.currentTimeMillis(),
                    language = language,
                    monitoringActive = isRunning
                )
                AlertRepository.setWatchSyncData(syncData)

                syncWatchData(context, syncData)
            } catch (t: Throwable) {
                AppLog.e(TAG, t) { "Failed to sync current state to Wear" }
                false
            }
        }

        suspend fun syncWatchData(context: Context, data: WatchSyncData): Boolean = withContext(Dispatchers.IO) {
            try {
                val dataClient = Wearable.getDataClient(context.applicationContext)
                val syncDataJson = json.encodeToString(WatchSyncData.serializer(), data)
                val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_ALERT_STATUS).apply {
                    dataMap.putString(WearConstants.KEY_SYNC_DATA, syncDataJson)

                    // Backward compatibility for legacy clients expecting KEY_ALERT_DATA
                    val primaryProfile = data.profiles.firstOrNull()
                    val primaryStatus = if (primaryProfile != null && data.monitoringActive) {
                        data.statuses[primaryProfile.id]
                    } else {
                        null
                    }
                    val legacyStatus = primaryStatus ?: AlertStatus(
                        regionName = primaryProfile?.regionName ?: "",
                        districtName = primaryProfile?.districtName,
                        updatedAt = 0L // Never synthesize fresh CALM!
                    )
                    val legacyAlertJson = json.encodeToString(AlertStatus.serializer(), legacyStatus)
                    dataMap.putString(WearConstants.KEY_ALERT_DATA, legacyAlertJson)
                    dataMap.putLong(WearConstants.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                Tasks.await(dataClient.putDataItem(putDataReq))
                true
            } catch (t: Throwable) {
                AppLog.e(TAG, t) { "Failed to putDataItem for Wear sync" }
                false
            }
        }
    }

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val _connectedWatchCount = MutableStateFlow(0)
    val connectedWatchCount: StateFlow<Int> = _connectedWatchCount.asStateFlow()

    fun start() {
        messageClient.addListener(this)
        refreshConnectedNodes()
    }

    fun stop() {
        messageClient.removeListener(this)
    }

    fun refreshConnectedNodes() {
        scope.launch(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                _connectedWatchCount.value = nodes.size
                AlertRepository.setConnectedWatchCount(nodes.size)
            } catch (_: Exception) {
                _connectedWatchCount.value = 0
                AlertRepository.setConnectedWatchCount(0)
            }
        }
    }

    suspend fun syncWatchData(data: WatchSyncData): Boolean {
        return syncWatchData(context, data)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == WearConstants.PATH_REQUEST_SYNC) {
            scope.launch {
                val data = getCurrentSyncData?.invoke()
                if (data != null) {
                    syncWatchData(data)
                } else {
                    syncCurrentState(context)
                }
            }
        }
    }
}
