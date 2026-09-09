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

    @Test
    fun testMultiProfileIndependentAlertEvaluation() {
        val response = NeptunAlertsResponse(
            oblasts = listOf(
                NeptunOblastAlert(key = "київська", name = "Київська область", since = "19:00", level = "red")
            ),
            raions = emptyList()
        )

        // Profile 1: Kyiv oblast (in alarm)
        val statusKyiv = DefaultRegions.computeAlertStatus(response, "kyivska", "Київська область", null, null)
        assertTrue(statusKyiv.isAlarm)
        assertEquals("red", statusKyiv.level)

        // Profile 2: Lviv oblast (calm)
        val statusLviv = DefaultRegions.computeAlertStatus(response, "lvivska", "Львівська область", null, null)
        assertFalse(statusLviv.isAlarm)

        // Profiles remain completely independent
        assertTrue(statusKyiv.isAlarm != statusLviv.isAlarm)
    }

    @Test
    fun testAlertTransitionEvaluatorLifecycleAndDeduplication() {
        val notifications = mutableListOf<String>()
        val profileAlertLevels = mutableMapOf<String, ua.alerts.shared.model.AlertLevel>()

        fun processUpdate(profileId: String, level: ua.alerts.shared.model.AlertLevel) {
            val prev = profileAlertLevels[profileId]
            if (prev == null) {
                // Baseline initialization: NEVER notify
                profileAlertLevels[profileId] = level
            } else {
                if (ua.alerts.shared.model.AlertTransitionEvaluator.shouldNotifyTransition(prev, level)) {
                    notifications.add("$profileId: $prev -> $level")
                    profileAlertLevels[profileId] = level
                }
            }
        }

        // 1. App starts up, initial snapshot has RED: baseline initialization, no notification
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.RED)
        assertTrue(notifications.isEmpty())

        // 2. Repeated server messages with RED: snapshot, alerts, reconnect
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.RED)
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.RED)
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.RED)
        assertTrue(notifications.isEmpty())

        // 3. De-escalation: RED -> YELLOW produces exactly 1 notification
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.YELLOW)
        assertEquals(1, notifications.size)
        assertEquals("profile-kyiv: RED -> YELLOW", notifications.last())

        // 4. Repeated updates with YELLOW: no extra notification
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.YELLOW)
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.YELLOW)
        assertEquals(1, notifications.size)

        // 5. Escalation: YELLOW -> RED produces exactly 1 notification
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.RED)
        assertEquals(2, notifications.size)
        assertEquals("profile-kyiv: YELLOW -> RED", notifications.last())

        // 6. All clear: RED -> NO_ALERT produces exactly 1 notification
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.NO_ALERT)
        assertEquals(3, notifications.size)
        assertEquals("profile-kyiv: RED -> NO_ALERT", notifications.last())

        // 7. Repeated calm updates: no notification
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.NO_ALERT)
        processUpdate("profile-kyiv", ua.alerts.shared.model.AlertLevel.NO_ALERT)
        assertEquals(3, notifications.size)

        // 8. Newly added profile while active alert in that region: baseline initialized silently
        processUpdate("profile-kharkiv", ua.alerts.shared.model.AlertLevel.RED)
        assertEquals(3, notifications.size)

        // 9. Subsequent change in Kharkiv notifies independently
        processUpdate("profile-kharkiv", ua.alerts.shared.model.AlertLevel.NO_ALERT)
        assertEquals(4, notifications.size)
        assertEquals("profile-kharkiv: RED -> NO_ALERT", notifications.last())
    }
}
