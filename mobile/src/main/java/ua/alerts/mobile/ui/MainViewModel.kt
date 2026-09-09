package ua.alerts.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.AlertRepository
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.service.AlertForegroundService
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile

sealed interface Screen {
    data object Dashboard : Screen
    data object GlobalSettings : Screen
    data class ProfileConfig(val profileId: String?) : Screen
    data class RegionPicker(val profileId: String?) : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)

    val profiles: StateFlow<List<Profile>> = settingsRepo.profiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val globalMonitoring: StateFlow<Boolean> = settingsRepo.globalMonitoring
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val profileAlerts: StateFlow<Map<String, AlertStatus>> = AlertRepository.profileAlerts
    val connectionStatus: StateFlow<ConnectionStatus> = AlertRepository.connectionStatus
    val connectedWatchCount: StateFlow<Int> = AlertRepository.connectedWatchCount
    val isServiceRunning: StateFlow<Boolean> = AlertRepository.isServiceRunning

    private val screenBackstack = mutableListOf<Screen>(Screen.Dashboard)
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _draftProfile = MutableStateFlow<Profile?>(null)
    val draftProfile: StateFlow<Profile?> = _draftProfile.asStateFlow()

    fun navigateTo(screen: Screen) {
        screenBackstack.add(screen)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (screenBackstack.size > 1) {
            screenBackstack.removeAt(screenBackstack.lastIndex)
            _currentScreen.value = screenBackstack.last()
        } else {
            _currentScreen.value = Screen.Dashboard
        }
    }

    fun initDraftProfile(profileId: String?) {
        if (profileId == null) {
            _draftProfile.value = Profile(
                backgroundMonitoring = true,
                activeOnWatch = profiles.value.isEmpty(),
                soundOnAlarm = true,
                vibrateOnAlarm = true,
                soundOnClear = true,
                vibrateOnClear = true
            )
        } else {
            _draftProfile.value = profiles.value.find { it.id == profileId }
        }
    }

    fun updateDraft(updater: (Profile) -> Profile) {
        val current = _draftProfile.value ?: return
        val updated = updater(current)
        _draftProfile.value = updated
        // Auto-save in Edit mode (profile already exists in repository)
        if (profiles.value.any { it.id == updated.id }) {
            updateProfile(updated)
        }
    }

    fun setDraftRegion(regionId: String, regionName: String, districtId: String?, districtName: String?) {
        val current = _draftProfile.value ?: return
        val updated = current.copy(
            regionId = regionId,
            regionName = regionName,
            districtId = districtId,
            districtName = districtName
        )
        _draftProfile.value = updated
        // Auto-save in Edit mode
        if (profiles.value.any { it.id == updated.id }) {
            updateProfile(updated)
        }
    }

    fun saveDraftProfile() {
        val draft = _draftProfile.value ?: return
        if (draft.regionId.isBlank()) return
        saveProfile(draft)
    }

    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            settingsRepo.saveProfile(profile)
            navigateBack()
            refreshData()
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            settingsRepo.updateProfile(profile)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            settingsRepo.deleteProfile(profileId)
            navigateBack()
        }
    }

    fun setActiveWatchProfile(profileId: String) {
        viewModelScope.launch {
            settingsRepo.setActiveWatchProfile(profileId)
        }
    }

    fun setGlobalMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setGlobalMonitoring(enabled)
            if (enabled) {
                AlertForegroundService.startService(getApplication())
            } else {
                AlertForegroundService.stopService(getApplication())
            }
        }
    }

    fun refreshData() {
        AlertForegroundService.refresh(getApplication())
    }
}