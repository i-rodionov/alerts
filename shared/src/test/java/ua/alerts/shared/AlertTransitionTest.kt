package ua.alerts.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.model.AlertLevel
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.AlertTransitionEvaluator
import ua.alerts.shared.model.toAlertLevel

class AlertTransitionTest {

    @Test
    fun testAlertStatusToAlertLevelMapping() {
        val calm = AlertStatus(isAlarm = false)
        assertEquals(AlertLevel.NO_ALERT, calm.toAlertLevel())

        val yellow = AlertStatus(isAlarm = true, level = "yellow")
        assertEquals(AlertLevel.YELLOW, yellow.toAlertLevel())

        val red = AlertStatus(isAlarm = true, level = "red")
        assertEquals(AlertLevel.RED, red.toAlertLevel())

        // Active alarm with null or unrecognized level defaults to RED
        val fallbackRed = AlertStatus(isAlarm = true, level = null)
        assertEquals(AlertLevel.RED, fallbackRed.toAlertLevel())
    }

    @Test
    fun testInitialBaselineDoesNotNotify() {
        assertFalse(AlertTransitionEvaluator.shouldNotifyTransition(null, AlertLevel.NO_ALERT))
        assertFalse(AlertTransitionEvaluator.shouldNotifyTransition(null, AlertLevel.YELLOW))
        assertFalse(AlertTransitionEvaluator.shouldNotifyTransition(null, AlertLevel.RED))
    }

    @Test
    fun testUnchangedStatusDoesNotNotify() {
        assertFalse(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.NO_ALERT, AlertLevel.NO_ALERT))
        assertFalse(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.YELLOW, AlertLevel.YELLOW))
        assertFalse(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.RED, AlertLevel.RED))
    }

    @Test
    fun testTransitionsProduceNotifications() {
        // No alert -> Yellow
        assertTrue(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.NO_ALERT, AlertLevel.YELLOW))
        // No alert -> Red
        assertTrue(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.NO_ALERT, AlertLevel.RED))
        // Yellow -> Red (escalation)
        assertTrue(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.YELLOW, AlertLevel.RED))
        // Red -> Yellow (de-escalation)
        assertTrue(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.RED, AlertLevel.YELLOW))
        // Yellow -> No alert (all-clear)
        assertTrue(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.YELLOW, AlertLevel.NO_ALERT))
        // Red -> No alert (all-clear)
        assertTrue(AlertTransitionEvaluator.shouldNotifyTransition(AlertLevel.RED, AlertLevel.NO_ALERT))
    }
}
