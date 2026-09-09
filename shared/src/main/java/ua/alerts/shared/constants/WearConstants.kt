package ua.alerts.shared.constants

@Suppress("unused")
object WearConstants {
    const val PATH_ALERT_STATUS = "/alert_status"
    const val PATH_REQUEST_SYNC = "/request_sync"
    const val KEY_ALERT_DATA = "alert_data_json"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_SYNC_DATA = "watch_sync_data_json"
    const val DEFAULT_TIMEOUT_MS = 15 * 60 * 1000L

    const val ACTION_CONFIG_COMPLICATION = "ua.alerts.wear.action.CONFIG_COMPLICATION"
    const val EXTRA_PROFILE_ID = "ua.alerts.wear.extra.PROFILE_ID"
}
