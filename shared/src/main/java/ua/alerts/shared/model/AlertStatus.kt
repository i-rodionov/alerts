package ua.alerts.shared.model

import kotlinx.serialization.Serializable
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.data.DefaultRegions

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
    fun isStale(timeoutMs: Long = WearConstants.DEFAULT_TIMEOUT_MS): Boolean {
        if (updatedAt <= 0) return true
        return (System.currentTimeMillis() - updatedAt) > timeoutMs
    }

    val isYellow: Boolean
        get() = isAlarm && level?.equals("yellow", ignoreCase = true) == true

    val isRed: Boolean
        get() = isAlarm && !isYellow

    val displayName: String
        get() = districtName ?: regionName

    fun getLocalizedDisplayName(language: String): String =
        DefaultRegions.getDisplayName(regionKey, districtKey, language, displayName)

    @Suppress("unused")
    fun getLocalizedRegionName(language: String): String =
        DefaultRegions.getRegionName(regionKey, language, regionName)

    @Suppress("unused")
    fun getLocalizedDistrictName(language: String): String? =
        districtKey?.let { DefaultRegions.getDistrictName(it, language, districtName ?: "") }
}
