package ua.alerts.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ua.alerts.shared.logging.AppLog
import ua.alerts.shared.model.AlertLevel
import ua.alerts.shared.model.Profile
import kotlin.time.Duration.Companion.milliseconds

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {

    companion object {
        private const val TAG = "SettingsRepository"
        val KEY_PROFILES_JSON = stringPreferencesKey("profiles_json")
        val KEY_GLOBAL_MONITORING = booleanPreferencesKey("global_monitoring")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_ALERT_BASELINES_JSON = stringPreferencesKey("alert_baselines_json")

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val profiles: Flow<List<Profile>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_PROFILES_JSON]
        if (jsonStr.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<Profile>>(jsonStr)
            } catch (t: Throwable) {
                AppLog.e(TAG, t) { "Failed to decode profiles from DataStore" }
                emptyList()
            }
        }
    }

    val globalMonitoring: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_GLOBAL_MONITORING] ?: prefs[KEY_SERVICE_ENABLED] ?: true
    }

    val appLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "system"
    }

    val alertBaselines: Flow<Map<String, AlertLevel>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_ALERT_BASELINES_JSON]
        if (jsonStr.isNullOrBlank()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<String, AlertLevel>>(jsonStr)
            } catch (t: Throwable) {
                AppLog.e(TAG, t) { "Failed to decode alert baselines from DataStore" }
                emptyMap()
            }
        }
    }

    suspend fun getAlertBaselines(): Map<String, AlertLevel> {
        return try {
            alertBaselines.first()
        } catch (t: Throwable) {
            AppLog.e(TAG, t) { "Failed to read alert baselines from DataStore" }
            emptyMap()
        }
    }

    suspend fun setAlertBaseline(targetKey: String, level: AlertLevel) {
        setAlertBaselines(mapOf(targetKey to level))
    }

    private suspend fun <T> retryIO(times: Int = 3, initialDelayMs: Long = 20, block: suspend () -> T): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) {
            try {
                return block()
            } catch (_: Exception) {
                kotlinx.coroutines.delay(currentDelay.milliseconds)
                currentDelay *= 2
            }
        }
        return block()
    }

    suspend fun setAlertBaselines(newBaselines: Map<String, AlertLevel>) {
        if (newBaselines.isEmpty()) return
        try {
            retryIO {
                dataStore.edit { prefs ->
                    val currentMap = try {
                        val jsonStr = prefs[KEY_ALERT_BASELINES_JSON]
                        if (!jsonStr.isNullOrBlank()) json.decodeFromString<Map<String, AlertLevel>>(jsonStr) else emptyMap()
                    } catch (t: Throwable) {
                        AppLog.e(TAG, t) { "Failed to decode alert baselines during batch edit" }
                        emptyMap()
                    }
                    val updatedMap = currentMap + newBaselines
                    prefs[KEY_ALERT_BASELINES_JSON] = json.encodeToString(updatedMap)
                }
            }
        } catch (t: Throwable) {
            AppLog.e(TAG, t) { "Failed to persist alert baselines to DataStore" }
        }
    }

    suspend fun setAppLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language
        }
        triggerWearSync()
    }

    suspend fun setGlobalMonitoring(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_GLOBAL_MONITORING] = enabled
            prefs[KEY_SERVICE_ENABLED] = enabled
        }
        triggerWearSync()
    }

    suspend fun saveProfile(profile: Profile) {
        try {
            retryIO {
                dataStore.edit { prefs ->
                    val currentList = try {
                        val jsonStr = prefs[KEY_PROFILES_JSON]
                        if (!jsonStr.isNullOrBlank()) json.decodeFromString<List<Profile>>(jsonStr) else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val updatedList = if (currentList.any { it.id == profile.id }) {
                        currentList.map { existing ->
                            if (existing.id == profile.id) profile else existing
                        }
                    } else {
                        currentList + profile
                    }

                    prefs[KEY_PROFILES_JSON] = json.encodeToString(updatedList)
                }
            }
            triggerWearSync()
        } catch (t: Throwable) {
            AppLog.e(TAG, t) { "Failed to save profile ${profile.id}" }
        }
    }

    suspend fun updateProfile(profile: Profile) = saveProfile(profile)

    suspend fun deleteProfile(profileId: String) {
        try {
            retryIO {
                dataStore.edit { prefs ->
                    val currentList = try {
                        val jsonStr = prefs[KEY_PROFILES_JSON]
                        if (!jsonStr.isNullOrBlank()) json.decodeFromString<List<Profile>>(jsonStr) else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val filtered = currentList.filter { it.id != profileId }
                    prefs[KEY_PROFILES_JSON] = json.encodeToString(filtered)

                    // Also clean up persisted baselines for the deleted profile
                    val currentBaselines = try {
                        val jsonStr = prefs[KEY_ALERT_BASELINES_JSON]
                        if (!jsonStr.isNullOrBlank()) json.decodeFromString<Map<String, AlertLevel>>(jsonStr) else emptyMap()
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    val prefix = "$profileId:"
                    val filteredBaselines = currentBaselines.filterKeys { !it.startsWith(prefix) }
                    if (filteredBaselines.size != currentBaselines.size) {
                        prefs[KEY_ALERT_BASELINES_JSON] = json.encodeToString(filteredBaselines)
                    }
                }
            }
            triggerWearSync()
        } catch (t: Throwable) {
            AppLog.e(TAG, t) { "Failed to delete profile $profileId" }
        }
    }

    private fun triggerWearSync() {
        try {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                WearSyncManager.syncCurrentState(context)
            }
        } catch (_: Throwable) {}
    }
}