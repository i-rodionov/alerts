package ua.alerts.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.alerts.shared.model.Profile

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_PROFILES_JSON = stringPreferencesKey("profiles_json")
        val KEY_GLOBAL_MONITORING = booleanPreferencesKey("global_monitoring")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val profiles: Flow<List<Profile>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_PROFILES_JSON]
        if (jsonStr.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<Profile>>(jsonStr)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val globalMonitoring: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLOBAL_MONITORING] ?: prefs[KEY_SERVICE_ENABLED] ?: true
    }

    val serviceEnabled: Flow<Boolean> = globalMonitoring

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "system"
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language
        }
    }

    suspend fun setGlobalMonitoring(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLOBAL_MONITORING] = enabled
            prefs[KEY_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) = setGlobalMonitoring(enabled)

    suspend fun saveProfile(profile: Profile) {
        context.dataStore.edit { prefs ->
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

    suspend fun updateProfile(profile: Profile) = saveProfile(profile)

    suspend fun deleteProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val currentList = try {
                val jsonStr = prefs[KEY_PROFILES_JSON]
                if (!jsonStr.isNullOrBlank()) json.decodeFromString<List<Profile>>(jsonStr) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val filtered = currentList.filter { it.id != profileId }
            prefs[KEY_PROFILES_JSON] = json.encodeToString(filtered)
        }
    }

    suspend fun setProfileWatchSync(profileId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val currentList = try {
                val jsonStr = prefs[KEY_PROFILES_JSON]
                if (!jsonStr.isNullOrBlank()) json.decodeFromString<List<Profile>>(jsonStr) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val updatedList = currentList.map { p ->
                if (p.id == profileId) p.copy(activeOnWatch = enabled) else p
            }
            prefs[KEY_PROFILES_JSON] = json.encodeToString(updatedList)
        }
    }

    suspend fun setActiveWatchProfile(profileId: String) = setProfileWatchSync(profileId, true)
}