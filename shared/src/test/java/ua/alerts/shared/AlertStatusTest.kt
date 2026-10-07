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

        val statusWithLevel = status.copy(level = "yellow", reasons = listOf("Дронова загроза"))
        val serialized = json.encodeToString(AlertStatus.serializer(), statusWithLevel)
        val deserialized = json.decodeFromString<AlertStatus>(serialized)

        assertEquals(statusWithLevel.isAlarm, deserialized.isAlarm)
        assertEquals(statusWithLevel.regionKey, deserialized.regionKey)
        assertEquals(statusWithLevel.districtKey, deserialized.districtKey)
        assertEquals("yellow", deserialized.level)
        assertTrue(deserialized.isYellow)
        assertFalse(deserialized.isRed)
        assertEquals(listOf("Дронова загроза"), deserialized.reasons)
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
    fun testDefaultAlertStatus_hasZeroUpdatedAt_andIsStale() {
        val defaultStatus = AlertStatus()
        assertEquals(0L, defaultStatus.updatedAt)
        assertTrue("AlertStatus default must be stale to prevent synthesizing fresh CALM", defaultStatus.isStale())
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
            "version": 1788864689,
            "updatedAt": "2026-09-08T10:51:29.718748973Z",
            "raions": [
                { "key": "дніпровський", "name": "Дніпровський район", "oblast": "Дніпропетровська область", "since": "2026-09-06T22:26:00.000Z", "level": "yellow", "reasons": ["Дронова загроза (жовтий рівень)"] },
                { "key": "новомосковський", "name": "Самарівський район", "oblast": "Дніпропетровська область", "since": "2026-09-06T22:26:00.000Z", "level": "red", "reasons": ["Ракетна загроза (червоний рівень)"] },
                { "key": "криворізький", "name": "Криворізький район", "oblast": "Дніпропетровська область", "since": "2026-09-06T20:00:00.000Z" }
            ],
            "oblasts": [
                { "key": "луганська", "name": "Луганська область", "oblast": "Луганська область", "since": "2022-04-04T16:45:00.000Z", "level": "red" }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<NeptunAlertsResponse>(realJson)

        // 1. Specific district WITH active yellow alert (Dniprovskyi)
        val statusDnipro = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = "dnipropetrovska:dniprovskyi",
            districtName = "Дніпровський район"
        )
        assertTrue("Dniprovskyi district must be in alarm", statusDnipro.isAlarm)
        assertTrue("Dniprovskyi district must have isYellow = true", statusDnipro.isYellow)
        assertFalse("Dniprovskyi district must not be isRed", statusDnipro.isRed)
        assertEquals("yellow", statusDnipro.level)
        assertEquals(listOf("Дронова загроза (жовтий рівень)"), statusDnipro.reasons)
        assertEquals("2026-09-06T22:26:00.000Z", statusDnipro.since)

        // 2. Renamed district WITH active red alert (Novomoskovskyi / Samarivskyi)
        val statusNovomoskovsk = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = "dnipropetrovska:novomoskovskyi",
            districtName = "Новомосковський район"
        )
        assertTrue("Novomoskovskyi/Samarivskyi district must be in alarm", statusNovomoskovsk.isAlarm)
        assertTrue("Novomoskovskyi/Samarivskyi district must be red", statusNovomoskovsk.isRed)
        assertEquals("red", statusNovomoskovsk.level)

        // 3. District present in response but WITHOUT level field (API bug workaround -> Kryvorizkyi)
        val statusKryvyiRih = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = "dnipropetrovska:kryvorizkyi",
            districtName = "Криворізький район"
        )
        assertFalse("Kryvorizkyi district without level must NOT be in alarm (workaround)", statusKryvyiRih.isAlarm)
        assertFalse(statusKryvyiRih.isYellow)
        assertFalse(statusKryvyiRih.isRed)

        // 4. Entire Dnipropetrovsk oblast selected (one yellow, one red raion -> oblast resolves to RED)
        val statusWholeOblast = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtId = null,
            districtName = null
        )
        assertTrue("Whole oblast must show alarm", statusWholeOblast.isAlarm)
        assertTrue("Whole oblast must resolve to RED if any raion is RED", statusWholeOblast.isRed)
        assertEquals("red", statusWholeOblast.level)

        // 5. Whole Luhansk oblast is in red alarm -> district in Luhansk must ALSO show red alarm
        val statusLuhanskDistrict = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "luhanska",
            regionName = "Луганська область",
            districtId = "luhanska:luhanskyi",
            districtName = "Луганський район"
        )
        assertTrue("District in an oblast with whole-oblast alert must be in alarm", statusLuhanskDistrict.isAlarm)
        assertTrue(statusLuhanskDistrict.isRed)
        assertEquals("red", statusLuhanskDistrict.level)

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
