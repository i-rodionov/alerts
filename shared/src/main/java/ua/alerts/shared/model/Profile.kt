package ua.alerts.shared.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val regionId: String = "",
    val regionName: String = "",
    val districtId: String? = null,
    val districtName: String? = null,
    val backgroundMonitoring: Boolean = true,
    val activeOnWatch: Boolean = false,
    val soundOnAlarm: Boolean = true,
    val vibrateOnAlarm: Boolean = true,
    val soundOnClear: Boolean = true,
    val vibrateOnClear: Boolean = true,
    val alertSoundUri: String? = null,
    val clearSoundUri: String? = null
) {
    val displayName: String
        get() = districtName ?: regionName
}
