package ua.alerts.mobile.service

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.WearSyncManager
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.logging.AppLog

class PhoneWearableListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "PhoneWearListener"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearConstants.PATH_REQUEST_SYNC) {
            AppLog.d(TAG) { "Received /request_sync from watch node: ${messageEvent.sourceNodeId}" }
            serviceScope.launch {
                WearSyncManager.syncCurrentState(applicationContext)
            }
        }
    }
}
