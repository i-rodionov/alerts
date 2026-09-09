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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.WatchSyncData

class WearSyncManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getCurrentSyncData: () -> WatchSyncData
) : MessageClient.OnMessageReceivedListener {

    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val json = Json { ignoreUnknownKeys = true }

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
            } catch (_: Exception) {
                _connectedWatchCount.value = 0
            }
        }
    }

    suspend fun syncWatchData(data: WatchSyncData): Boolean = withContext(Dispatchers.IO) {
        try {
            val syncDataJson = json.encodeToString(WatchSyncData.serializer(), data)
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_ALERT_STATUS).apply {
                dataMap.putString(WearConstants.KEY_SYNC_DATA, syncDataJson)
                // Backward compatibility for legacy clients expecting KEY_ALERT_DATA
                val primaryProfile = data.profiles.firstOrNull()
                val primaryStatus = if (primaryProfile != null) {
                    data.statuses[primaryProfile.id] ?: AlertStatus()
                } else {
                    AlertStatus()
                }
                val legacyAlertJson = json.encodeToString(AlertStatus.serializer(), primaryStatus)
                dataMap.putString(WearConstants.KEY_ALERT_DATA, legacyAlertJson)
                dataMap.putLong(WearConstants.KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            Tasks.await(dataClient.putDataItem(putDataReq))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncAlertStatus(status: AlertStatus): Boolean = syncWatchData(getCurrentSyncData())

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == WearConstants.PATH_REQUEST_SYNC) {
            scope.launch {
                syncWatchData(getCurrentSyncData())
            }
        }
    }
}
