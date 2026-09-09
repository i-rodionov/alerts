package ua.alerts.shared

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.NeptunAlertsResponse
import ua.alerts.shared.model.Profile

class DefaultRegionsLocalizationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testRegionAndDistrictNameLookup() {
        assertEquals("м. Київ", DefaultRegions.getRegionName("m_kyiv", "uk"))
        assertEquals("Kyiv City", DefaultRegions.getRegionName("m_kyiv", "en"))

        assertEquals("Київська область", DefaultRegions.getRegionName("kyivska", "uk"))
        assertEquals("Kyiv Oblast", DefaultRegions.getRegionName("kyivska", "en"))

        assertEquals("Білоцерківський район", DefaultRegions.getDistrictName("kyivska:bilotserkivskyi", "uk"))
        assertEquals("Bila Tserkva District", DefaultRegions.getDistrictName("kyivska:bilotserkivskyi", "en"))
    }

    @Test
    fun testProfileLocalizedDisplayName() {
        val profileOblast = Profile(
            regionId = "odeska",
            regionName = "Одеська область"
        )
        assertEquals("Одеська область", profileOblast.getLocalizedDisplayName("uk"))
        assertEquals("Odesa Oblast", profileOblast.getLocalizedDisplayName("en"))

        val profileDistrict = Profile(
            regionId = "odeska",
            regionName = "Одеська область",
            districtId = "odeska:odeskyi",
            districtName = "Одеський район"
        )
        assertEquals("Одеський район", profileDistrict.getLocalizedDisplayName("uk"))
        assertEquals("Odesa District", profileDistrict.getLocalizedDisplayName("en"))
    }

    @Test
    fun testAlertStatusLocalizedDisplayName() {
        val statusOblast = AlertStatus(
            regionKey = "dnipropetrovska",
            regionName = "Дніпропетровська область"
        )
        assertEquals("Дніпропетровська область", statusOblast.getLocalizedDisplayName("uk"))
        assertEquals("Dnipropetrovsk Oblast", statusOblast.getLocalizedDisplayName("en"))

        val statusDistrict = AlertStatus(
            regionKey = "dnipropetrovska",
            regionName = "Дніпропетровська область",
            districtKey = "dnipropetrovska:dniprovskyi",
            districtName = "Дніпровський район"
        )
        assertEquals("Дніпровський район", statusDistrict.getLocalizedDisplayName("uk"))
        assertEquals("Dnipro District", statusDistrict.getLocalizedDisplayName("en"))
    }

    @Test
    fun testComputeAlertStatusWithEnglishProfileNames() {
        val sampleJson = """
        {
            "raions": [
                { "key": "odeska:odeskyi", "name": "Одеський район", "oblast": "Одеська область", "since": "2026-09-06T18:00:00.000Z", "level": "yellow" }
            ],
            "oblasts": [
                { "key": "kyivska", "name": "Київська область", "oblast": "", "since": "2026-09-06T19:00:00.000Z", "level": "red" }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<NeptunAlertsResponse>(sampleJson)

        // Pass English names in profile / computeAlertStatus parameters
        val statusOblastEn = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "kyivska",
            regionName = "Kyiv Oblast",
            districtId = null,
            districtName = null
        )
        assertTrue("Kyiv Oblast must be in red alarm even when passed with English name", statusOblastEn.isAlarm)
        assertEquals("red", statusOblastEn.level)
        assertEquals("kyivska", statusOblastEn.regionKey)

        val statusRaionEn = DefaultRegions.computeAlertStatus(
            alerts = response,
            regionId = "odeska",
            regionName = "Odesa Oblast",
            districtId = "odeska:odeskyi",
            districtName = "Odesa District"
        )
        assertTrue("Odesa District must be in yellow alarm even when passed with English name", statusRaionEn.isAlarm)
        assertEquals("yellow", statusRaionEn.level)
        assertEquals("odeska", statusRaionEn.regionKey)
        assertEquals("odeska:odeskyi", statusRaionEn.districtKey)
    }
}
