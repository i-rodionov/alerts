package ua.alerts.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.util.EventTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class EventTimeFormatterTest {

    private val kyivZone = ZoneId.of("Europe/Kyiv")
    private val utcZone = ZoneId.of("UTC")
    private val newYorkZone = ZoneId.of("America/New_York")

    @Test
    fun testParseUtcTimestamps() {
        val standard = EventTimeFormatter.parseUtc("2026-09-09T12:34:56Z")
        assertNotNull(standard)
        assertEquals(Instant.parse("2026-09-09T12:34:56Z"), standard)

        val withSubseconds = EventTimeFormatter.parseUtc("2026-09-08T04:42:56.778799Z")
        assertNotNull(withSubseconds)
        assertEquals(Instant.parse("2026-09-08T04:42:56.778799Z"), withSubseconds)

        assertNull(EventTimeFormatter.parseUtc(null))
        assertNull(EventTimeFormatter.parseUtc(""))
        assertNull(EventTimeFormatter.parseUtc("invalid-date"))
    }

    @Test
    fun testLocalTimezoneConversion() {
        // 2026-09-09T12:00:00Z -> In Kyiv (UTC+3 in summer/EEST) should be 15:00
        val utcIso = "2026-09-09T12:00:00Z"
        val fixedNow = Instant.parse("2026-09-09T16:00:00Z")

        val formattedKyivUk = EventTimeFormatter.formatLocalEventTime(
            utcIsoTimestamp = utcIso,
            locale = Locale("uk"),
            zoneId = kyivZone,
            now = fixedNow
        )
        // Same day -> time only: 15:00
        assertEquals("15:00", formattedKyivUk)

        val formattedKyivEn = EventTimeFormatter.formatLocalEventTime(
            utcIsoTimestamp = utcIso,
            locale = Locale.US,
            zoneId = kyivZone,
            now = fixedNow
        )
        // Same day US English -> time only with AM/PM: 3:00 PM
        assertTrue(formattedKyivEn.contains("3:00") && formattedKyivEn.contains("PM"))
    }

    @Test
    fun testHourAndDayBoundaryCrossing() {
        // Event at 23:30 UTC on Sep 8th -> In Kyiv (UTC+3) it is 02:30 on Sep 9th
        val utcIso = "2026-09-08T23:30:00Z"
        val nowOnSep9th = Instant.parse("2026-09-09T10:00:00Z") // same local day in Kyiv (Sep 9)

        val formattedSameDayKyiv = EventTimeFormatter.formatLocalEventTime(
            utcIsoTimestamp = utcIso,
            locale = Locale("uk"),
            zoneId = kyivZone,
            now = nowOnSep9th
        )
        // In Kyiv, this crossed into Sep 9, which matches nowOnSep9th's local date (Sep 9)!
        assertEquals("02:30", formattedSameDayKyiv)

        // But in New York (UTC-4 in summer/EDT), 23:30 UTC on Sep 8th is 19:30 on Sep 8th!
        // When compared to now on Sep 9th (10:00 UTC = 06:00 EDT Sep 9th), it was on an EARLIER day!
        val formattedEarlierDayNy = EventTimeFormatter.formatLocalEventTime(
            utcIsoTimestamp = utcIso,
            locale = Locale.US,
            zoneId = newYorkZone,
            now = nowOnSep9th
        )
        // Should contain date and time
        assertTrue("Formatted: $formattedEarlierDayNy", formattedEarlierDayNy.contains("8 Sep") || formattedEarlierDayNy.contains("Sep 8"))
        assertTrue("Formatted: $formattedEarlierDayNy", formattedEarlierDayNy.contains("7:30"))
    }

    @Test
    fun testEarlierDayFormatting() {
        val utcIso = "2026-09-06T18:00:00Z"
        val now = Instant.parse("2026-09-09T12:00:00Z")

        val ukFormatted = EventTimeFormatter.formatLocalEventTime(
            utcIsoTimestamp = utcIso,
            locale = Locale("uk"),
            zoneId = kyivZone,
            now = now
        )
        // In Ukrainian: "6 вер., 21:00"
        assertTrue("Expected to contain 6 вер.: $ukFormatted", ukFormatted.contains("6 вер"))
        assertTrue("Expected to contain 21:00: $ukFormatted", ukFormatted.contains("21:00"))

        val enFormatted = EventTimeFormatter.formatLocalEventTime(
            utcIsoTimestamp = utcIso,
            locale = Locale.US,
            zoneId = kyivZone,
            now = now
        )
        // In English: "6 Sep, 9:00 PM" or "Sep 6, 9:00 PM"
        assertTrue("Expected to contain 6 Sep: $enFormatted", enFormatted.contains("6 Sep") || enFormatted.contains("Sep 6"))
        assertTrue("Expected to contain 9:00 PM: $enFormatted", enFormatted.contains("9:00") && enFormatted.contains("PM"))
    }
}
