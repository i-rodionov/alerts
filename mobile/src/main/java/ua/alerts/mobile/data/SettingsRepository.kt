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
                    if (existing.id == profile.id) {
                        profile
                    } else if (profile.activeOnWatch) {
                        existing.copy(activeOnWatch = false)
                    } else {
                        existing
                    }
                }
            } else {
                val base = if (profile.activeOnWatch) {
                    currentList.map { it.copy(activeOnWatch = false) }
                } else {
                    currentList
                }
                base + profile
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
            val wasActiveOnWatch = currentList.find { it.id == profileId }?.activeOnWatch == true
            val finalList = if (wasActiveOnWatch && filtered.isNotEmpty() && filtered.none { it.activeOnWatch }) {
                filtered.mapIndexed { index, p -> if (index == 0) p.copy(activeOnWatch = true) else p }
            } else {
                filtered
            }

            prefs[KEY_PROFILES_JSON] = json.encodeToString(finalList)
        }
    }

    suspend fun setActiveWatchProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val currentList = try {
                val jsonStr = prefs[KEY_PROFILES_JSON]
                if (!jsonStr.isNullOrBlank()) json.decodeFromString<List<Profile>>(jsonStr) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val updatedList = currentList.map { p ->
                p.copy(activeOnWatch = (p.id == profileId))
            }
            prefs[KEY_PROFILES_JSON] = json.encodeToString(updatedList)
        }
    }
}