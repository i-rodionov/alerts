package ua.alerts.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.model.AlertStatus

class WatchAlertRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("wear_alert_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentStatus = MutableStateFlow(loadInitialStatus())
    val currentStatus: StateFlow<AlertStatus> = _currentStatus.asStateFlow()

    private fun loadInitialStatus(): AlertStatus {
        val rawJson = prefs.getString("cached_status_json", null) ?: return AlertStatus()
        return try {
            json.decodeFromString<AlertStatus>(rawJson)
        } catch (_: Exception) {
            AlertStatus()
        }
    }

    fun updateStatus(newStatus: AlertStatus) {
        _currentStatus.value = newStatus
        val rawJson = json.encodeToString(AlertStatus.serializer(), newStatus)
        prefs.edit().putString("cached_status_json", rawJson).apply()
    }

    fun requestSyncFromPhone() {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = nodeClient.connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearConstants.PATH_REQUEST_SYNC, byteArrayOf()).await()
                }
            } catch (_: Exception) {}
        }
    }
}
