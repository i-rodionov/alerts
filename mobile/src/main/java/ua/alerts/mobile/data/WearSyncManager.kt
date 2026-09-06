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

class WearSyncManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getCurrentStatus: () -> AlertStatus
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

    suspend fun syncAlertStatus(status: AlertStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(AlertStatus.serializer(), status)
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_ALERT_STATUS).apply {
                dataMap.putString(WearConstants.KEY_ALERT_DATA, jsonString)
                dataMap.putLong(WearConstants.KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            Tasks.await(dataClient.putDataItem(putDataReq))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == WearConstants.PATH_REQUEST_SYNC) {
            scope.launch {
                syncAlertStatus(getCurrentStatus())
            }
        }
    }
}
