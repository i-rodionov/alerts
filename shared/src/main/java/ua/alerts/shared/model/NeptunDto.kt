package ua.alerts.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NeptunAlertsResponse(
    val raions: List<NeptunRaionAlert> = emptyList(),
    val oblasts: List<NeptunOblastAlert> = emptyList()
)

@Serializable
data class NeptunRaionAlert(
    val key: String,
    val name: String,
    val oblast: String = "",
    val since: String? = null
)

@Serializable
data class NeptunOblastAlert(
    val key: String,
    val name: String,
    val oblast: String = "",
    val since: String? = null
)

@Serializable
data class NeptunWsEnvelope(
    val type: String,
    val ts: Long? = null,
    val data: JsonElement? = null
)
