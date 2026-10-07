package ua.alerts.shared

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile
import ua.alerts.shared.model.WatchSyncData

class WatchSyncDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testSerializationAndDeserialization() {
        val profileA = Profile(
            id = "profile-a",
            regionId = "kyivska",
            regionName = "Київська область",
            activeOnWatch = true
        )
        val profileB = Profile(
            id = "profile-b",
            regionId = "poltavska",
            regionName = "Полтавська область",
            districtId = "poltava-district",
            districtName = "Полтавський район",
            activeOnWatch = true
        )
        val statusA = AlertStatus(
            isAlarm = true,
            regionKey = "kyivska",
            regionName = "Київська область",
            level = "red",
            reasons = listOf("Загроза БпЛА")
        )
        val statusB = AlertStatus(
            isAlarm = false,
            regionKey = "poltavska",
            regionName = "Полтавська область",
            districtKey = "poltava-district",
            districtName = "Полтавський район"
        )

        val syncData = WatchSyncData(
            profiles = listOf(profileA, profileB),
            statuses = mapOf("profile-a" to statusA, "profile-b" to statusB),
            updatedAt = 123456789L,
            language = "uk",
            monitoringActive = true
        )

        val encoded = json.encodeToString(WatchSyncData.serializer(), syncData)
        val decoded = json.decodeFromString<WatchSyncData>(encoded)

        assertEquals(2, decoded.profiles.size)
        assertEquals("profile-a", decoded.profiles[0].id)
        assertEquals("profile-b", decoded.profiles[1].id)
        assertEquals(123456789L, decoded.updatedAt)
        assertEquals("uk", decoded.language)
        assertTrue(decoded.monitoringActive)

        val decodedStatusA = decoded.statuses["profile-a"]
        assertNotNull(decodedStatusA)
        assertTrue(decodedStatusA!!.isAlarm)
        assertTrue(decodedStatusA.isRed)
        assertEquals("Київська область", decodedStatusA.regionName)

        val decodedStatusB = decoded.statuses["profile-b"]
        assertNotNull(decodedStatusB)
        assertFalse(decodedStatusB!!.isAlarm)
        assertEquals("Полтавський район", decodedStatusB.displayName)
    }

    @Test
    fun testEmptySyncData() {
        val empty = WatchSyncData()
        val encoded = json.encodeToString(WatchSyncData.serializer(), empty)
        val decoded = json.decodeFromString<WatchSyncData>(encoded)

        assertTrue(decoded.profiles.isEmpty())
        assertTrue(decoded.statuses.isEmpty())
        assertFalse(decoded.monitoringActive)
        assertEquals(0L, decoded.updatedAt)
    }

    @Test
    fun testMonitoringActiveSerialization() {
        val activeSync = WatchSyncData(monitoringActive = true, updatedAt = 1000L)
        val encodedActive = json.encodeToString(WatchSyncData.serializer(), activeSync)
        val decodedActive = json.decodeFromString<WatchSyncData>(encodedActive)
        assertTrue(decodedActive.monitoringActive)
        assertEquals(1000L, decodedActive.updatedAt)

        val inactiveSync = WatchSyncData(monitoringActive = false, updatedAt = 2000L)
        val encodedInactive = json.encodeToString(WatchSyncData.serializer(), inactiveSync)
        val decodedInactive = json.decodeFromString<WatchSyncData>(encodedInactive)
        assertFalse(decodedInactive.monitoringActive)
        assertEquals(2000L, decodedInactive.updatedAt)
    }
}
