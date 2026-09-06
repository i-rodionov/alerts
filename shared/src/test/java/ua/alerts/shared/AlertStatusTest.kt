package ua.alerts.shared

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.NeptunAlertsResponse

class AlertStatusTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testParseNeptunAlertsResponse() {
        val sampleJson = """
        {
            "raions": [
                { "key": "odeska:odeskyi", "name": "Одеський район", "oblast": "Одеська область", "since": "2026-09-06T18:00:00.000Z" }
            ],
            "oblasts": [
                { "key": "krym", "name": "АР Крим", "oblast": "", "since": "2022-02-24T00:00:00.000Z" }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<NeptunAlertsResponse>(sampleJson)
        assertEquals(1, response.raions.size)
        assertEquals("odeska:odeskyi", response.raions[0].key)
        assertEquals(1, response.oblasts.size)
        assertEquals("krym", response.oblasts[0].key)
    }

    @Test
    fun testAlertStatusSerialization() {
        val status = AlertStatus(
            isAlarm = true,
            regionKey = "odeska",
            regionName = "Одеська область",
            districtKey = "odeska:odeskyi",
            districtName = "Одеський район",
            since = "2026-09-06T18:00:00.000Z",
            updatedAt = System.currentTimeMillis()
        )

        val serialized = json.encodeToString(AlertStatus.serializer(), status)
        val deserialized = json.decodeFromString<AlertStatus>(serialized)

        assertEquals(status.isAlarm, deserialized.isAlarm)
        assertEquals(status.regionKey, deserialized.regionKey)
        assertEquals(status.districtKey, deserialized.districtKey)
        assertEquals("Одеський район", deserialized.displayName)
        assertFalse(deserialized.isStale())
    }

    @Test
    fun testStalenessDetection() {
        val oldStatus = AlertStatus(
            isAlarm = false,
            regionKey = "kyivska",
            regionName = "Київська область",
            updatedAt = System.currentTimeMillis() - (20 * 60 * 1000L) // 20 mins ago
        )
        assertTrue(oldStatus.isStale())

        val recentStatus = AlertStatus(
            isAlarm = false,
            regionKey = "kyivska",
            regionName = "Київська область",
            updatedAt = System.currentTimeMillis() - (5 * 60 * 1000L) // 5 mins ago
        )
        assertFalse(recentStatus.isStale())
    }

    @Test
    fun testDefaultRegions() {
        assertTrue(DefaultRegions.ALL_REGIONS.isNotEmpty())
        val odeska = DefaultRegions.findRegion("odeska")
        assertTrue(odeska != null)
        assertEquals("Одеська область", odeska?.nameUk)

        val odeskyiDistrict = DefaultRegions.findDistrict("odeska:odeskyi")
        assertTrue(odeskyiDistrict != null)
        assertEquals("Одеський район", odeskyiDistrict?.nameUk)
    }
}
