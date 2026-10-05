package ua.alerts.wear.data

import android.content.Context
import androidx.core.content.edit
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile
import ua.alerts.shared.model.WatchSyncData

class WatchAlertRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("wear_alert_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncData = MutableStateFlow(loadInitialSyncData())
    val syncData: StateFlow<WatchSyncData> = _syncData.asStateFlow()

    private val _currentStatus = MutableStateFlow(loadInitialStatus())

    private fun loadInitialSyncData(): WatchSyncData {
        val rawJson = prefs.getString("cached_sync_data_json", null)
        if (!rawJson.isNullOrBlank()) {
            try {
                return json.decodeFromString<WatchSyncData>(rawJson)
            } catch (_: Exception) {}
        }

        // Backward compatibility fallback from cached_status_json
        val legacyJson = prefs.getString("cached_status_json", null)
        if (!legacyJson.isNullOrBlank()) {
            try {
                val legacyStatus = json.decodeFromString<AlertStatus>(legacyJson)
                val fallbackProfile = Profile(
                    id = "legacy_watch_profile",
                    regionName = legacyStatus.regionName,
                    districtName = legacyStatus.districtName,
                    activeOnWatch = true
                )
                return WatchSyncData(
                    profiles = listOf(fallbackProfile),
                    statuses = mapOf("legacy_watch_profile" to legacyStatus),
                    updatedAt = legacyStatus.updatedAt
                )
            } catch (_: Exception) {}
        }

        return WatchSyncData()
    }

    private fun loadInitialStatus(): AlertStatus {
        val sync = loadInitialSyncData()
        val primaryProfile = sync.profiles.firstOrNull()
        if (primaryProfile != null) {
            return sync.statuses[primaryProfile.id] ?: AlertStatus()
        }
        val rawJson = prefs.getString("cached_status_json", null) ?: return AlertStatus()
        return try {
            json.decodeFromString<AlertStatus>(rawJson)
        } catch (_: Exception) {
            AlertStatus()
        }
    }

    fun updateSyncData(newSyncData: WatchSyncData) {
        _syncData.value = newSyncData
        val rawJson = json.encodeToString(WatchSyncData.serializer(), newSyncData)
        prefs.edit { putString("cached_sync_data_json", rawJson) }

        val primaryProfile = newSyncData.profiles.firstOrNull()
        val primaryStatus = if (primaryProfile != null) {
            newSyncData.statuses[primaryProfile.id] ?: AlertStatus()
        } else {
            AlertStatus()
        }
        _currentStatus.value = primaryStatus
        val legacyRawJson = json.encodeToString(AlertStatus.serializer(), primaryStatus)
        prefs.edit { putString("cached_status_json", legacyRawJson) }
    }

    fun updateStatus(newStatus: AlertStatus) {
        val syntheticProfile = Profile(
            id = "default_profile",
            regionName = newStatus.regionName,
            districtName = newStatus.districtName,
            activeOnWatch = true
        )
        updateSyncData(
            WatchSyncData(
                profiles = listOf(syntheticProfile),
                statuses = mapOf(syntheticProfile.id to newStatus),
                updatedAt = newStatus.updatedAt
            )
        )
    }

    fun getProfile(profileId: String?): Profile? {
        if (profileId.isNullOrEmpty()) return null
        return _syncData.value.profiles.find { it.id == profileId }
    }

    fun getStatus(profileId: String?): AlertStatus? {
        if (profileId.isNullOrEmpty()) return null
        return _syncData.value.statuses[profileId]
    }

    fun getComplicationProfileId(instanceId: Int): String? {
        return prefs.getString("complication_${instanceId}_profile_id", null)
    }

    fun setComplicationProfileId(instanceId: Int, profileId: String) {
        prefs.edit { putString("complication_${instanceId}_profile_id", profileId) }
    }

    fun requestSyncFromPhone() {
        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = nodeClient.connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearConstants.PATH_REQUEST_SYNC, byteArrayOf()).await()
                }
            } catch (_: Exception) {}
        }
    }
}
