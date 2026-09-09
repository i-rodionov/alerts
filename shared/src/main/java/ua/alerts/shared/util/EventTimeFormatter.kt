package ua.alerts.shared.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Utility for parsing UTC event timestamps received from the server
 * and formatting them into user-friendly localized date/time strings
 * in the device's local timezone.
 */
object EventTimeFormatter {

    /**
     * Parses an ISO-8601 UTC timestamp (e.g., "2026-09-09T12:34:56Z" or "2026-09-08T04:42:56.778799Z").
     * Returns null if the timestamp cannot be parsed.
     */
    fun parseUtc(isoTimestamp: String?): Instant? {
        if (isoTimestamp.isNullOrBlank()) return null
        return try {
            Instant.parse(isoTimestamp)
        } catch (_: Exception) {
            try {
                DateTimeFormatter.ISO_DATE_TIME.parse(isoTimestamp, Instant::from)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Converts a UTC ISO timestamp to the specified [zoneId] and formats it according to [locale].
     *
     * Smart formatting rule:
     * - If the event occurred on the same calendar day as [now] in [zoneId]: formats as time only (e.g. "14:25" or "2:25 PM").
     * - If the event occurred on an earlier day: formats as date + time (e.g. "9 вер., 14:25" or "Sep 9, 2:25 PM").
     */
    fun formatLocalEventTime(
        utcIsoTimestamp: String?,
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now()
    ): String {
        val instant = parseUtc(utcIsoTimestamp) ?: return utcIsoTimestamp ?: ""
        val localZdt = instant.atZone(zoneId)
        val todayZdt = now.atZone(zoneId)

        val isToday = localZdt.toLocalDate() == todayZdt.toLocalDate()

        return if (isToday) {
            val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            localZdt.format(timeFormatter)
        } else {
            val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            val timePart = localZdt.format(timeFormatter)
            val dateFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
            val datePart = localZdt.format(dateFormatter)
            "$datePart, $timePart"
        }
    }
}
