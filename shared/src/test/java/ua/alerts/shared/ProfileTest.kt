package ua.alerts.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.model.Profile

class ProfileTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testProfileDefaults() {
        val profile = Profile()
        assertTrue(profile.id.isNotEmpty())
        assertEquals("", profile.regionId)
        assertEquals("", profile.regionName)
        assertEquals(null, profile.districtId)
        assertEquals(null, profile.districtName)
        assertTrue(profile.backgroundMonitoring)
        assertFalse(profile.activeOnWatch)
        assertTrue(profile.soundOnAlarm)
        assertTrue(profile.vibrateOnAlarm)
        assertTrue(profile.soundOnClear)
        assertTrue(profile.vibrateOnClear)
        assertEquals("", profile.displayName)
    }

    @Test
    fun testProfileDisplayName() {
        val oblastProfile = Profile(regionName = "Київська область")
        assertEquals("Київська область", oblastProfile.displayName)

        val districtProfile = Profile(
            regionName = "Київська область",
            districtName = "Білоцерківський район"
        )
        assertEquals("Білоцерківський район", districtProfile.displayName)
    }

    @Test
    fun testProfileSerialization() {
        val profile = Profile(
            id = "test-uuid-1234",
            regionId = "kyivska",
            regionName = "Київська область",
            districtId = "kyivska:bilotserkivskyi",
            districtName = "Білоцерківський район",
            backgroundMonitoring = true,
            activeOnWatch = true,
            soundOnAlarm = true,
            vibrateOnAlarm = false,
            soundOnClear = false,
            vibrateOnClear = true
        )

        val list = listOf(profile)
        val serialized = json.encodeToString(list)
        val deserialized = json.decodeFromString<List<Profile>>(serialized)

        assertEquals(1, deserialized.size)
        val p = deserialized[0]
        assertEquals("test-uuid-1234", p.id)
        assertEquals("kyivska", p.regionId)
        assertEquals("Київська область", p.regionName)
        assertEquals("kyivska:bilotserkivskyi", p.districtId)
        assertEquals("Білоцерківський район", p.districtName)
        assertTrue(p.backgroundMonitoring)
        assertTrue(p.activeOnWatch)
        assertTrue(p.soundOnAlarm)
        assertFalse(p.vibrateOnAlarm)
        assertFalse(p.soundOnClear)
        assertTrue(p.vibrateOnClear)
        assertEquals("Білоцерківський район", p.displayName)
    }
}
