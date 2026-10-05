package ua.alerts.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class AlertTargetKey(
    val profileId: String,
    val regionId: String,
    val districtId: String? = null
) {
    val storageKey: String
        get() = "$profileId:$regionId:${districtId.orEmpty()}"

    companion object {
        fun fromProfile(profile: Profile): AlertTargetKey =
            AlertTargetKey(
                profileId = profile.id,
                regionId = profile.regionId,
                districtId = profile.districtId
            )

        fun fromStorageKey(storageKey: String): AlertTargetKey {
            val parts = storageKey.split(":", limit = 3)
            val profileId = parts.getOrNull(0).orEmpty()
            val regionId = parts.getOrNull(1).orEmpty()
            val districtIdRaw = parts.getOrNull(2)
            val districtId = if (districtIdRaw.isNullOrEmpty()) null else districtIdRaw
            return AlertTargetKey(
                profileId = profileId,
                regionId = regionId,
                districtId = districtId
            )
        }
    }
}
