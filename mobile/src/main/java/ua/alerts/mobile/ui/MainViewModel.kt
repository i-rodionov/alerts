package ua.alerts.mobile.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.service.AlertMonitoringService
import ua.alerts.shared.model.AlertStatus

data class MainUiState(
    val status: AlertStatus = AlertStatus(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectedWatchCount: Int = 0,
    val isRegionPickerOpen: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val soundAlarm = settingsRepo.soundOnAlarm.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val vibrateAlarm = settingsRepo.vibrateOnAlarm.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val soundClear = settingsRepo.soundOnClear.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val vibrateClear = settingsRepo.vibrateOnClear.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val serviceEnabled = settingsRepo.serviceEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var boundService: AlertMonitoringService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? AlertMonitoringService.LocalBinder)?.service
            boundService = service

            service?.let { s ->
                viewModelScope.launch {
                    s.currentStatus.collect { status ->
                        _uiState.value = _uiState.value.copy(status = status)
                    }
                }
                viewModelScope.launch {
                    s.connectionStatus.collect { status ->
                        _uiState.value = _uiState.value.copy(connectionStatus = status)
                    }
                }
                viewModelScope.launch {
                    s.connectedWatchCount.collect { count ->
                        _uiState.value = _uiState.value.copy(connectedWatchCount = count)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    init {
        bindService()
    }

    fun bindService() {
        val intent = Intent(getApplication(), AlertMonitoringService::class.java)
        getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun openRegionPicker() {
        _uiState.value = _uiState.value.copy(isRegionPickerOpen = true)
    }

    fun closeRegionPicker() {
        _uiState.value = _uiState.value.copy(isRegionPickerOpen = false)
    }

    fun selectRegion(regionId: String, regionName: String, districtId: String?, districtName: String?) {
        viewModelScope.launch {
            settingsRepo.setSelectedRegion(regionId, regionName, districtId, districtName)
            closeRegionPicker()
            refreshData()
        }
    }

    fun refreshData() {
        AlertMonitoringService.refresh(getApplication())
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setServiceEnabled(enabled)
            if (enabled) {
                AlertMonitoringService.startService(getApplication())
            } else {
                AlertMonitoringService.stopService(getApplication())
            }
        }
    }

    fun setSoundAlarm(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSoundOnAlarm(enabled) }
    fun setVibrateAlarm(enabled: Boolean) = viewModelScope.launch { settingsRepo.setVibrateOnAlarm(enabled) }
    fun setSoundClear(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSoundOnClear(enabled) }
    fun setVibrateClear(enabled: Boolean) = viewModelScope.launch { settingsRepo.setVibrateOnClear(enabled) }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(connection)
        } catch (_: Exception) {}
        super.onCleared()
    }
}
