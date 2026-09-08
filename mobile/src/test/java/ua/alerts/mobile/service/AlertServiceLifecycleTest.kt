package ua.alerts.mobile.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.mobile.data.AlertRepository
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.NeptunAlertsResponse
import ua.alerts.shared.model.NeptunOblastAlert

class AlertServiceLifecycleTest {

    @Test
    fun testServiceRunningDistinctFromWebSocketConnected() {
        AlertRepository.reset()

        // 1. Service can be running while WebSocket is connecting
        AlertRepository.setServiceRunning(true)
        AlertRepository.setConnectionStatus(ConnectionStatus.CONNECTING)
        assertTrue(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.CONNECTING, AlertRepository.connectionStatus.value)

        // 2. Service can be running while WebSocket is reconnecting
        AlertRepository.setConnectionStatus(ConnectionStatus.RECONNECTING)
        assertTrue(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.RECONNECTING, AlertRepository.connectionStatus.value)

        // 3. Service can be running while WebSocket is in error
        AlertRepository.setConnectionStatus(ConnectionStatus.ERROR, "Host unreachable")
        assertTrue(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.ERROR, AlertRepository.connectionStatus.value)

        // 4. Service can be running while WebSocket is connected
        AlertRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
        assertTrue(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.CONNECTED, AlertRepository.connectionStatus.value)

        // 5. When service is stopped, status is STOPPED
        AlertRepository.setServiceRunning(false)
        assertFalse(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.STOPPED, AlertRepository.connectionStatus.value)
    }

    @Test
    fun testDuplicateAlertEventsDeduplication() {
        val regionId = "kyivska"
        val regionName = "Київська область"

        val responseWithAlert = NeptunAlertsResponse(
            oblasts = listOf(
                NeptunOblastAlert(key = "київська", name = "Київська область", since = "19:00", level = "red")
            )
        )

        // Compute status first time
        val status1 = DefaultRegions.computeAlertStatus(responseWithAlert, regionId, regionName, null, null)
        assertTrue(status1.isAlarm)

        // Same response arrived again (e.g. duplicate WebSocket frame or snapshot fetch during reconnect)
        val status2 = DefaultRegions.computeAlertStatus(responseWithAlert, regionId, regionName, null, null)
        assertTrue(status2.isAlarm)

        // The alarm state did not change
        assertEquals(status1.isAlarm, status2.isAlarm)

        // Now clear response arrives
        val responseClear = NeptunAlertsResponse(oblasts = emptyList(), raions = emptyList())
        val status3 = DefaultRegions.computeAlertStatus(responseClear, regionId, regionName, null, null)
        assertFalse(status3.isAlarm)
        assertTrue(status2.isAlarm != status3.isAlarm)
    }
}
