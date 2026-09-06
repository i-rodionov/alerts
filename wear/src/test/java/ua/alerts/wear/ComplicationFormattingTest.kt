package ua.alerts.wear

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.alerts.shared.model.AlertStatus

class ComplicationFormattingTest {

    @Test
    fun testComplicationTextWhenAlarm() {
        val status = AlertStatus(
            isAlarm = true,
            regionKey = "kyivska",
            regionName = "Київська область",
            districtKey = "kyivska:boryspilskyi",
            districtName = "Бориспільський район",
            updatedAt = System.currentTimeMillis()
        )

        val isOffline = status.isStale()
        val textStr = when {
            isOffline -> "—"
            status.isAlarm -> "ТРВ"
            else -> "ОК"
        }
        val titleStr = when {
            isOffline -> "?"
            status.isAlarm -> "??"
            else -> "??"
        }

        assertEquals("ТРВ", textStr)
        assertEquals("??", titleStr)
        assertEquals("Бориспільський район", status.displayName)
    }

    @Test
    fun testComplicationTextWhenClear() {
        val status = AlertStatus(
            isAlarm = false,
            regionKey = "kyivska",
            regionName = "Київська область",
            updatedAt = System.currentTimeMillis()
        )

        val isOffline = status.isStale()
        val textStr = when {
            isOffline -> "—"
            status.isAlarm -> "ТРВ"
            else -> "ОК"
        }
        val titleStr = when {
            isOffline -> "?"
            status.isAlarm -> "??"
            else -> "??"
        }

        assertEquals("ОК", textStr)
        assertEquals("??", titleStr)
        assertEquals("Київська область", status.displayName)
    }

    @Test
    fun testComplicationTextWhenStale() {
        val status = AlertStatus(
            isAlarm = true,
            regionKey = "odeska",
            regionName = "Одеська область",
            updatedAt = System.currentTimeMillis() - (30 * 60 * 1000L) // 30 minutes ago
        )

        val isOffline = status.isStale()
        val textStr = when {
            isOffline -> "—"
            status.isAlarm -> "ТРВ"
            else -> "ОК"
        }
        val titleStr = when {
            isOffline -> "?"
            status.isAlarm -> "??"
            else -> "??"
        }

        assertEquals("—", textStr)
        assertEquals("?", titleStr)
    }
}
