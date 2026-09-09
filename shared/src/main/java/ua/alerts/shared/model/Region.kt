package ua.alerts.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Region(
    val id: String,
    val nameUk: String,
    val nameEn: String,
    val neptunKey: String? = null,
    val raions: List<District> = emptyList()
) {
    fun getLocalizedName(language: String): String =
        if (language.startsWith("en", ignoreCase = true)) nameEn else nameUk
}

@Serializable
data class District(
    val id: String,
    val nameUk: String,
    val nameEn: String,
    val oblastId: String,
    val neptunKey: String? = null
) {
    fun getLocalizedName(language: String): String =
        if (language.startsWith("en", ignoreCase = true)) nameEn else nameUk
}
