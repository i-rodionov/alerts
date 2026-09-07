package ua.alerts.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.AlertRepository
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.service.AlertForegroundService
import ua.alerts.shared.model.AlertStatus

data class MainUiState(
    val status: AlertStatus = AlertStatus(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.STOPPED,
    val connectedWatchCount: Int = 0,
    val isRegionPickerOpen: Boolean = false,
    val isServiceRunning: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)

    val soundAlarm = settingsRepo.soundOnAlarm.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val vibrateAlarm = settingsRepo.vibrateOnAlarm.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val soundClear = settingsRepo.soundOnClear.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val vibrateClear = settingsRepo.vibrateOnClear.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val serviceEnabled = settingsRepo.serviceEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _isRegionPickerOpen = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> = combine(
        AlertRepository.alertStatus,
        AlertRepository.connectionStatus,
        AlertRepository.connectedWatchCount,
        AlertRepository.isServiceRunning,
        _isRegionPickerOpen
    ) { status, connectionStatus, watchCount, isRunning, pickerOpen ->
        MainUiState(
            status = status,
            connectionStatus = connectionStatus,
            connectedWatchCount = watchCount,
            isRegionPickerOpen = pickerOpen,
            isServiceRunning = isRunning
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    fun openRegionPicker() {
        _isRegionPickerOpen.value = true
    }

    fun closeRegionPicker() {
        _isRegionPickerOpen.value = false
    }

    fun selectRegion(regionId: String, regionName: String, districtId: String?, districtName: String?) {
        viewModelScope.launch {
            settingsRepo.setSelectedRegion(regionId, regionName, districtId, districtName)
            closeRegionPicker()
            refreshData()
        }
    }

    fun refreshData() {
        AlertForegroundService.refresh(getApplication())
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setServiceEnabled(enabled)
            if (enabled) {
                AlertForegroundService.startService(getApplication())
            } else {
                AlertForegroundService.stopService(getApplication())
            }
        }
    }

    fun setSoundAlarm(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSoundOnAlarm(enabled) }
    fun setVibrateAlarm(enabled: Boolean) = viewModelScope.launch { settingsRepo.setVibrateOnAlarm(enabled) }
    fun setSoundClear(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSoundOnClear(enabled) }
    fun setVibrateClear(enabled: Boolean) = viewModelScope.launch { settingsRepo.setVibrateOnClear(enabled) }
}

