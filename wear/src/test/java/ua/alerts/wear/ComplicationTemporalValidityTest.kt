package ua.alerts.wear

import androidx.wear.watchface.complications.data.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.model.AlertStatus
import java.time.Instant

class ComplicationTemporalValidityTest {

    @Test
    fun testTimeRangeEnforcesValidityAtTtlBoundary() {
        val updatedAt = 1700000000000L
        val ttl = WearConstants.DEFAULT_TIMEOUT_MS // 15 mins (900_000 ms)
        val expiryTime = updatedAt + ttl
        val validUntil = Instant.ofEpochMilli(expiryTime)

        val timeRange = TimeRange.before(validUntil)

        assertEquals(validUntil, timeRange.endDateTimeMillis)

        // 1 millisecond before expiry -> valid
        val justBefore = Instant.ofEpochMilli(expiryTime - 1)
        assertTrue("Complication must be valid before expiry", timeRange.contains(justBefore))

        // At updatedAt timestamp -> valid
        val atUpdated = Instant.ofEpochMilli(updatedAt)
        assertTrue("Complication must be valid at updatedAt", timeRange.contains(atUpdated))

        // 1 millisecond after expiry -> strictly invalid
        val justAfter = Instant.ofEpochMilli(expiryTime + 1)
        assertFalse("Complication must be invalid after updatedAt + TTL", timeRange.contains(justAfter))

        // 1 hour after expiry -> strictly invalid
        val oneHourLater = Instant.ofEpochMilli(expiryTime + 3600000L)
        assertFalse("Complication must be invalid 1 hour after expiry", timeRange.contains(oneHourLater))
    }

    @Test
    fun testDefaultAlertStatus_ProducesStaleExpiryImmediately() {
        val defaultStatus = AlertStatus()
        assertEquals(0L, defaultStatus.updatedAt)

        val ttl = WearConstants.DEFAULT_TIMEOUT_MS
        val expiryTime = defaultStatus.updatedAt + ttl // 0 + 15 mins = 900_000 ms (~Jan 1 1970)
        val validUntil = Instant.ofEpochMilli(expiryTime)
        val timeRange = TimeRange.before(validUntil)

        // Current real time is vastly beyond 1970
        val currentNow = Instant.now()
        assertFalse("Default AlertStatus must be expired relative to current real time", timeRange.contains(currentNow))
    }
}
