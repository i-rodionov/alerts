package ua.alerts.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.alerts.shared.model.AlertStatus

class AlertRepositoryTest {

    @Before
    fun setup() {
        AlertRepository.reset()
    }

    @Test
    fun testInitialRepositoryState() {
        assertFalse(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.STOPPED, AlertRepository.connectionStatus.value)
        assertEquals(AlertStatus(), AlertRepository.alertStatus.value)
        assertEquals(0, AlertRepository.connectedWatchCount.value)
        assertNull(AlertRepository.lastError.value)
    }

    @Test
    fun testServiceRunningUpdates() {
        AlertRepository.setServiceRunning(true)
        assertTrue(AlertRepository.isServiceRunning.value)

        // When service is stopped, connection status automatically resets to STOPPED
        AlertRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
        assertEquals(ConnectionStatus.CONNECTED, AlertRepository.connectionStatus.value)

        AlertRepository.setServiceRunning(false)
        assertFalse(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.STOPPED, AlertRepository.connectionStatus.value)
    }

    @Test
    fun testConnectionStatusAndErrorUpdates() {
        AlertRepository.setConnectionStatus(ConnectionStatus.CONNECTING)
        assertEquals(ConnectionStatus.CONNECTING, AlertRepository.connectionStatus.value)
        assertNull(AlertRepository.lastError.value)

        AlertRepository.setConnectionStatus(ConnectionStatus.ERROR, "Network timeout")
        assertEquals(ConnectionStatus.ERROR, AlertRepository.connectionStatus.value)
        assertEquals("Network timeout", AlertRepository.lastError.value)

        AlertRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
        assertEquals(ConnectionStatus.CONNECTED, AlertRepository.connectionStatus.value)
        assertNull(AlertRepository.lastError.value)
    }

    @Test
    fun testAlertStatusUpdates() {
        val newStatus = AlertStatus(
            isAlarm = true,
            regionKey = "kyivska",
            regionName = "Київська область",
            districtKey = "kyivska:boryspilskyi",
            districtName = "Бориспільський район",
            since = "2026-09-07 19:30"
        )
        AlertRepository.setAlertStatus(newStatus)
        assertEquals(newStatus, AlertRepository.alertStatus.value)
        assertTrue(AlertRepository.alertStatus.value.isAlarm)
        assertEquals("Бориспільський район", AlertRepository.alertStatus.value.displayName)
    }

    @Test
    fun testWatchCountUpdates() {
        AlertRepository.setConnectedWatchCount(2)
        assertEquals(2, AlertRepository.connectedWatchCount.value)
    }

    @Test
    fun testProfileAlertsUpdatesAndReset() {
        assertTrue(AlertRepository.profileAlerts.value.isEmpty())

        val status1 = AlertStatus(isAlarm = true, regionKey = "kyivska", regionName = "Київська область")
        val status2 = AlertStatus(isAlarm = false, regionKey = "odeska", regionName = "Одеська область")
        val map = mapOf("profile-1" to status1, "profile-2" to status2)

        AlertRepository.setProfileAlerts(map)
        assertEquals(2, AlertRepository.profileAlerts.value.size)
        assertEquals(status1, AlertRepository.profileAlerts.value["profile-1"])
        assertEquals(status2, AlertRepository.profileAlerts.value["profile-2"])

        AlertRepository.reset()
        assertTrue(AlertRepository.profileAlerts.value.isEmpty())
    }
}
