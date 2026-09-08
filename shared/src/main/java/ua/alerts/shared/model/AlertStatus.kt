package ua.alerts.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class AlertStatus(
    val isAlarm: Boolean = false,
    val regionKey: String = "",
    val regionName: String = "",
    val districtKey: String? = null,
    val districtName: String? = null,
    val since: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val level: String? = null,
    val reasons: List<String> = emptyList()
) {
    fun isStale(timeoutMs: Long = 15 * 60 * 1000L): Boolean {
        if (updatedAt <= 0) return true
        return (System.currentTimeMillis() - updatedAt) > timeoutMs
    }

    val isYellow: Boolean
        get() = isAlarm && level?.equals("yellow", ignoreCase = true) == true

    val isRed: Boolean
        get() = isAlarm && !isYellow

    val displayName: String
        get() = districtName ?: regionName
}
