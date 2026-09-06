package ua.alerts.wear.service

import android.content.ComponentName
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.model.AlertStatus
import ua.alerts.wear.WearAlertApp
import ua.alerts.wear.complication.AlertComplicationService

class WearDataListenerService : WearableListenerService() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == WearConstants.PATH_ALERT_STATUS) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val payloadJson = dataMap.getString(WearConstants.KEY_ALERT_DATA)

                    if (!payloadJson.isNullOrEmpty()) {
                        try {
                            val newStatus = json.decodeFromString<AlertStatus>(payloadJson)
                            WearAlertApp.instance.alertRepository.updateStatus(newStatus)

                            // Request immediate update of all active complications
                            val requester = ComplicationDataSourceUpdateRequester.create(
                                this,
                                ComponentName(this, AlertComplicationService::class.java)
                            )
                            requester.requestUpdateAll()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
