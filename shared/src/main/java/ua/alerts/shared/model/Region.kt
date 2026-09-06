package ua.alerts.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Region(
    val id: String,
    val nameUk: String,
    val nameEn: String,
    val raions: List<District> = emptyList()
)

@Serializable
data class District(
    val id: String,
    val nameUk: String,
    val nameEn: String,
    val oblastId: String
)
