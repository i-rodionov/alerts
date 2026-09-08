package ua.alerts.wear

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.alerts.shared.model.AlertStatus

class ComplicationFormattingTest {

    @Test
    fun testComplicationTextWhenYellowAlarm() {
        val status = AlertStatus(
            isAlarm = true,
            level = "yellow",
            reasons = listOf("Дронова загроза"),
            regionKey = "kyivska",
            regionName = "Київська область",
            districtKey = "kyivska:boryspilskyi",
            districtName = "Бориспільський район",
            updatedAt = System.currentTimeMillis()
        )

        val isOffline = status.isStale()
        val textStr = when {
            isOffline -> "—"
            status.isYellow -> "🟡"
            status.isRed -> "🔴"
            else -> "🟢"
        }
        val titleStr = when {
            isOffline -> "⚠️"
            status.isYellow -> "ЖОВ"
            status.isRed -> "ТРВ"
            else -> "ОК"
        }

        assertEquals("🟡", textStr)
        assertEquals("ЖОВ", titleStr)
        assertEquals("Бориспільський район", status.displayName)
    }

    @Test
    fun testComplicationTextWhenRedAlarm() {
        val status = AlertStatus(
            isAlarm = true,
            level = "red",
            reasons = listOf("Ракетна загроза"),
            regionKey = "kyivska",
            regionName = "Київська область",
            districtKey = "kyivska:boryspilskyi",
            districtName = "Бориспільський район",
            updatedAt = System.currentTimeMillis()
        )

        val isOffline = status.isStale()
        val textStr = when {
            isOffline -> "—"
            status.isYellow -> "🟡"
            status.isRed -> "🔴"
            else -> "🟢"
        }
        val titleStr = when {
            isOffline -> "⚠️"
            status.isYellow -> "ЖОВ"
            status.isRed -> "ТРВ"
            else -> "ОК"
        }

        assertEquals("🔴", textStr)
        assertEquals("ТРВ", titleStr)
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
            status.isYellow -> "🟡"
            status.isRed -> "🔴"
            else -> "🟢"
        }
        val titleStr = when {
            isOffline -> "⚠️"
            status.isYellow -> "ЖОВ"
            status.isRed -> "ТРВ"
            else -> "ОК"
        }

        assertEquals("🟢", textStr)
        assertEquals("ОК", titleStr)
        assertEquals("Київська область", status.displayName)
    }

    @Test
    fun testComplicationTextWhenStale() {
        val status = AlertStatus(
            isAlarm = true,
            level = "red",
            regionKey = "odeska",
            regionName = "Одеська область",
            updatedAt = System.currentTimeMillis() - (30 * 60 * 1000L) // 30 minutes ago
        )

        val isOffline = status.isStale()
        val textStr = when {
            isOffline -> "—"
            status.isYellow -> "🟡"
            status.isRed -> "🔴"
            else -> "🟢"
        }
        val titleStr = when {
            isOffline -> "⚠️"
            status.isYellow -> "ЖОВ"
            status.isRed -> "ТРВ"
            else -> "ОК"
        }

        assertEquals("—", textStr)
        assertEquals("⚠️", titleStr)
    }
}
