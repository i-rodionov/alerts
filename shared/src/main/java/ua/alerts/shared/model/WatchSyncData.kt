package ua.alerts.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchSyncData(
    val profiles: List<Profile> = emptyList(),
    val statuses: Map<String, AlertStatus> = emptyMap(),
    val updatedAt: Long = 0L,
    val language: String = "system",
    val monitoringActive: Boolean = false
)
