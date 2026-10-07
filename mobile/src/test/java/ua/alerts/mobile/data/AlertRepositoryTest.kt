package ua.alerts.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile
import ua.alerts.shared.model.WatchSyncData

class AlertRepositoryTest {

    @Before
    fun setup() {
        AlertRepository.reset()
    }

    @Test
    fun testInitialRepositoryState() {
        assertFalse(AlertRepository.isServiceRunning.value)
        assertEquals(ConnectionStatus.STOPPED, AlertRepository.connectionStatus.value)
        assertNull(AlertRepository.alertStatus.value)
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
        assertTrue(AlertRepository.alertStatus.value?.isAlarm == true)
        assertEquals("Бориспільський район", AlertRepository.alertStatus.value?.displayName)
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

    @Test
    fun testWatchSyncDataUpdatesAndReset() {
        assertTrue(AlertRepository.watchSyncData.value.profiles.isEmpty())

        val p1 = Profile(id = "p1", regionName = "Київ", activeOnWatch = true)
        val p2 = Profile(id = "p2", regionName = "Львів", activeOnWatch = true)
        val s1 = AlertStatus(isAlarm = true, regionName = "Київ")
        val s2 = AlertStatus(isAlarm = false, regionName = "Львів")
        val syncData = WatchSyncData(
            profiles = listOf(p1, p2),
            statuses = mapOf("p1" to s1, "p2" to s2)
        )

        AlertRepository.setWatchSyncData(syncData)
        assertEquals(2, AlertRepository.watchSyncData.value.profiles.size)
        assertEquals("p1", AlertRepository.watchSyncData.value.profiles[0].id)
        assertEquals("p2", AlertRepository.watchSyncData.value.profiles[1].id)
        assertTrue(AlertRepository.watchSyncData.value.statuses["p1"]!!.isAlarm)

        AlertRepository.reset()
        assertTrue(AlertRepository.watchSyncData.value.profiles.isEmpty())
        assertTrue(AlertRepository.watchSyncData.value.statuses.isEmpty())
    }


    @Test
    fun testClearRuntimeAlerts() {
        val status = AlertStatus(isAlarm = true, regionKey = "kyivska", regionName = "Київська область")
        AlertRepository.setAlertStatus(status)
        AlertRepository.setProfileAlerts(mapOf("p1" to status))

        assertEquals(status, AlertRepository.alertStatus.value)
        assertEquals(1, AlertRepository.profileAlerts.value.size)

        AlertRepository.clearRuntimeAlerts()

        assertNull(AlertRepository.alertStatus.value)
        assertTrue(AlertRepository.profileAlerts.value.isEmpty())
    }
}
