package ua.alerts.mobile.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.alerts.shared.model.AlertStatus

object AlertRepository {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.STOPPED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _alertStatus = MutableStateFlow(AlertStatus())
    val alertStatus: StateFlow<AlertStatus> = _alertStatus.asStateFlow()

    private val _profileAlerts = MutableStateFlow<Map<String, AlertStatus>>(emptyMap())
    val profileAlerts: StateFlow<Map<String, AlertStatus>> = _profileAlerts.asStateFlow()

    private val _connectedWatchCount = MutableStateFlow(0)
    val connectedWatchCount: StateFlow<Int> = _connectedWatchCount.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        if (!running) {
            _connectionStatus.value = ConnectionStatus.STOPPED
        }
    }

    fun setConnectionStatus(status: ConnectionStatus, error: String? = null) {
        _connectionStatus.value = status
        _lastError.value = error
    }

    fun setAlertStatus(status: AlertStatus) {
        _alertStatus.value = status
    }

    fun setProfileAlerts(alerts: Map<String, AlertStatus>) {
        _profileAlerts.value = alerts
    }

    fun setConnectedWatchCount(count: Int) {
        _connectedWatchCount.value = count
    }

    fun reset() {
        _isServiceRunning.value = false
        _connectionStatus.value = ConnectionStatus.STOPPED
        _alertStatus.value = AlertStatus()
        _profileAlerts.value = emptyMap()
        _connectedWatchCount.value = 0
        _lastError.value = null
    }
}