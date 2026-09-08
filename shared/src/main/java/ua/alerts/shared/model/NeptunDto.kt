package ua.alerts.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NeptunAlertsResponse(
    val raions: List<NeptunRaionAlert> = emptyList(),
    val oblasts: List<NeptunOblastAlert> = emptyList(),
    val version: Long? = null,
    val updatedAt: String? = null
)

@Serializable
data class NeptunRaionAlert(
    val key: String,
    val name: String,
    val oblast: String = "",
    val since: String? = null,
    val level: String? = null,
    val reasons: List<String> = emptyList()
)

@Serializable
data class NeptunOblastAlert(
    val key: String,
    val name: String,
    val oblast: String = "",
    val since: String? = null,
    val level: String? = null,
    val reasons: List<String> = emptyList()
)

@Serializable
data class NeptunWsEnvelope(
    val type: String,
    val ts: String? = null,
    val data: JsonElement? = null
)
