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
        assertEquals("одеська", odeska?.neptunKey)

        val odeskyiDistrict = DefaultRegions.findDistrict("odeska:odeskyi")
        assertTrue(odeskyiDistrict != null)
        assertEquals("Одеський район", odeskyiDistrict?.nameUk)
        assertEquals("одеський", odeskyiDistrict?.neptunKey)

        // Find by neptunKey
        val foundByNeptunKey = DefaultRegions.findDistrict("одеський")
        assertEquals("odeska:odeskyi", foundByNeptunKey?.id)
        val regionByNeptunKey = DefaultRegions.findRegion("одеська")
        assertEquals("odeska", regionByNeptunKey?.id)

        // Verify Crimea raions
        val crimea = DefaultRegions.findRegion("krym")
        assertEquals(10, crimea?.raions?.size)
        val simferopol = DefaultRegions.findDistrict("krym:simferopolskyi")
        assertEquals("сімферопольський", simferopol?.neptunKey)
    }

    @Test
    fun testComputeAlertStatus_realNeptunResponse() {
        val realJson = """
        {
            "raions": [
                { "key": "дніпровський", "name": "Дніпровський район", "oblast": "Дніпропетровська область", "since": "2026-09-06T22:26:00.000Z" },
                { "key": "новомосковський", "name": "Самарівський район", "oblast": "Дніпропетровська область", "since": "2026-09-06T22:26:00.000Z" }
            ],
            "oblasts": [
                { "key": "луганська", "name": "Луганська область", "oblast": "Луганська область", "since": "2022-04-04T16:45:00.000Z" }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<NeptunAlertsResponse>(realJson)

        // 1. Specific district WITH active alert (Dniprovskyi)
        val statusDnipro = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = "dnipropetrovska:dniprovskyi",
            districtName = "Дніпровський район"
        )
        assertTrue("Dniprovskyi district must be in alarm", statusDnipro.isAlarm)
        assertEquals("2026-09-06T22:26:00.000Z", statusDnipro.since)

        // 2. Renamed district WITH active alert (Novomoskovskyi / Samarivskyi)
        val statusNovomoskovsk = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = "dnipropetrovska:novomoskovskyi",
            districtName = "Новомосковський район"
        )
        assertTrue("Novomoskovskyi/Samarivskyi district must be in alarm", statusNovomoskovsk.isAlarm)

        // 3. District WITHOUT active alert in the same oblast (Kryvorizkyi)
        val statusKryvyiRih = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = "dnipropetrovska:kryvorizkyi",
            districtName = "Криворізький район"
        )
        assertFalse("Kryvorizkyi district must NOT be in alarm", statusKryvyiRih.isAlarm)

        // 4. Entire Dnipropetrovsk oblast selected (should be alarm because raions inside are in alarm)
        val statusWholeOblast = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = null,
            districtName = null
        )
        assertTrue("Whole oblast must show alarm when raions have active alarm", statusWholeOblast.isAlarm)

        // 5. Whole Luhansk oblast is in alarm -> district in Luhansk must ALSO show alarm
        val statusLuhanskDistrict = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "luhanska",
            regionName = "Луганська область",
            districtId = "luhanska:luhanskyi",
            districtName = "Луганський район"
        )
        assertTrue("District in an oblast with whole-oblast alert must be in alarm", statusLuhanskDistrict.isAlarm)

        // 6. Calm oblast (Kyivska) -> must be All Clear
        val statusKyivDistrict = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "kyivska",
            regionName = "Київська область",
            districtId = "kyivska:fastivskyi",
            districtName = "Фастівський район"
        )
        assertFalse("Fastiv district must NOT be in alarm", statusKyivDistrict.isAlarm)
    }
}
