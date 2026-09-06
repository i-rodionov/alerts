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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alert_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_REGION_ID = stringPreferencesKey("region_id")
        val KEY_REGION_NAME = stringPreferencesKey("region_name")
        val KEY_DISTRICT_ID = stringPreferencesKey("district_id")
        val KEY_DISTRICT_NAME = stringPreferencesKey("district_name")

        val KEY_SOUND_ALARM = booleanPreferencesKey("sound_alarm")
        val KEY_VIBRATE_ALARM = booleanPreferencesKey("vibrate_alarm")
        val KEY_SOUND_CLEAR = booleanPreferencesKey("sound_clear")
        val KEY_VIBRATE_CLEAR = booleanPreferencesKey("vibrate_clear")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    }

    val regionId: Flow<String> = context.dataStore.data.map { it[KEY_REGION_ID] ?: "kyivska" }
    val regionName: Flow<String> = context.dataStore.data.map { it[KEY_REGION_NAME] ?: "Київська область" }
    val districtId: Flow<String?> = context.dataStore.data.map { it[KEY_DISTRICT_ID] }
    val districtName: Flow<String?> = context.dataStore.data.map { it[KEY_DISTRICT_NAME] }

    val soundOnAlarm: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND_ALARM] ?: true }
    val vibrateOnAlarm: Flow<Boolean> = context.dataStore.data.map { it[KEY_VIBRATE_ALARM] ?: true }
    val soundOnClear: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND_CLEAR] ?: true }
    val vibrateOnClear: Flow<Boolean> = context.dataStore.data.map { it[KEY_VIBRATE_CLEAR] ?: true }
    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SERVICE_ENABLED] ?: true }

    suspend fun setSelectedRegion(
        regionId: String,
        regionName: String,
        districtId: String?,
        districtName: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REGION_ID] = regionId
            prefs[KEY_REGION_NAME] = regionName
            if (districtId != null && districtName != null) {
                prefs[KEY_DISTRICT_ID] = districtId
                prefs[KEY_DISTRICT_NAME] = districtName
            } else {
                prefs.remove(KEY_DISTRICT_ID)
                prefs.remove(KEY_DISTRICT_NAME)
            }
        }
    }

    suspend fun setSoundOnAlarm(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ALARM] = enabled }
    }

    suspend fun setVibrateOnAlarm(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATE_ALARM] = enabled }
    }

    suspend fun setSoundOnClear(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_CLEAR] = enabled }
    }

    suspend fun setVibrateOnClear(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATE_CLEAR] = enabled }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }
}
