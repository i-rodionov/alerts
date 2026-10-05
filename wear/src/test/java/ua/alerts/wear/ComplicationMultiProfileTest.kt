package ua.alerts.wear

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile
import ua.alerts.shared.model.WatchSyncData

data class ComplicationDisplayState(
    val isOffline: Boolean,
    val isAlarm: Boolean,
    val regionName: String,
    val shortText: String,
    val shortTitle: String,
    val longHeader: String
)

class ComplicationMultiProfileTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun resolveComplication(
        instanceId: Int,
        instanceMap: Map<Int, String>,
        syncData: WatchSyncData
    ): ComplicationDisplayState {
        val profileId = instanceMap[instanceId]
        val profile = syncData.profiles.find { it.id == profileId }
        val status = if (profile != null && profileId != null) syncData.statuses[profileId] else null

        val isOffline = profileId == null || profile == null || status == null || status.isStale()
        val isAlarm = !isOffline && status.isAlarm
        val regionName = profile?.displayName ?: status?.displayName ?: "Тривоги"

        val shortText = when {
            isOffline -> "—"
            status.isYellow -> "🟡"
            status.isRed -> "🔴"
            else -> "🟢"
        }
        val shortTitle = when {
            isOffline -> "⚠️"
            status.isYellow -> "ЖОВ"
            status.isRed -> "ТРВ"
            else -> "ОК"
        }
        val longHeader = when {
            isOffline -> "Немає зв'язку"
            status.isYellow -> "🟡 " + (status.reasons.firstOrNull() ?: "ЖОВТИЙ РІВЕНЬ")
            status.isRed -> "🔴 " + (status.reasons.firstOrNull() ?: "ЧЕРВОНИЙ РІВЕНЬ")
            else -> "🟢 Чисто"
        }

        return ComplicationDisplayState(
            isOffline = isOffline,
            isAlarm = isAlarm,
            regionName = regionName,
            shortText = shortText,
            shortTitle = shortTitle,
            longHeader = longHeader
        )
    }

    @Test
    fun testCase1_ThreeProfiles_SynchronizationFiltering() {
        val phoneProfiles = listOf(
            Profile(id = "A", regionName = "Київ", activeOnWatch = true),
            Profile(id = "B", regionName = "Полтава", activeOnWatch = true),
            Profile(id = "C", regionName = "Львів", activeOnWatch = false)
        )

        // Only profiles with activeOnWatch = true are synced to Wear OS
        val syncedProfiles = phoneProfiles.filter { it.activeOnWatch }
        val syncData = WatchSyncData(
            profiles = syncedProfiles,
            statuses = syncedProfiles.associate { it.id to AlertStatus(regionName = it.regionName) }
        )

        assertEquals(2, syncData.profiles.size)
        assertTrue(syncData.profiles.any { it.id == "A" })
        assertTrue(syncData.profiles.any { it.id == "B" })
        assertFalse(syncData.profiles.any { it.id == "C" })
    }

    @Test
    fun testCase2_TwoComplicationInstances_IndependentDisplay() {
        val profileA = Profile(id = "A", regionName = "Київ", activeOnWatch = true)
        val profileB = Profile(id = "B", regionName = "Полтава", activeOnWatch = true)
        val statusA = AlertStatus(isAlarm = true, level = "red", regionName = "Київ", reasons = listOf("Ракетна загроза"))
        val statusB = AlertStatus(isAlarm = false, regionName = "Полтава")

        val syncData = WatchSyncData(
            profiles = listOf(profileA, profileB),
            statuses = mapOf("A" to statusA, "B" to statusB)
        )

        val instanceConfigs = mapOf(101 to "A", 102 to "B")

        val display1 = resolveComplication(101, instanceConfigs, syncData)
        val display2 = resolveComplication(102, instanceConfigs, syncData)

        // Instance 1 displays Kyiv (alarm)
        assertFalse(display1.isOffline)
        assertTrue(display1.isAlarm)
        assertEquals("Київ", display1.regionName)
        assertEquals("🔴", display1.shortText)
        assertEquals("ТРВ", display1.shortTitle)

        // Instance 2 displays Poltava (clear)
        assertFalse(display2.isOffline)
        assertFalse(display2.isAlarm)
        assertEquals("Полтава", display2.regionName)
        assertEquals("🟢", display2.shortText)
        assertEquals("ОК", display2.shortTitle)
    }

    @Test
    fun testCase3_ChangeInstance1_Instance2Unaffected() {
        val profileA = Profile(id = "A", regionName = "Київ", activeOnWatch = true)
        val profileB = Profile(id = "B", regionName = "Полтава", activeOnWatch = true)
        val statusA = AlertStatus(isAlarm = true, level = "red", regionName = "Київ")
        val statusB = AlertStatus(isAlarm = false, regionName = "Полтава")

        val syncData = WatchSyncData(
            profiles = listOf(profileA, profileB),
            statuses = mapOf("A" to statusA, "B" to statusB)
        )

        val configs = mutableMapOf(101 to "A", 102 to "B")

        // Change Instance 1 from A to B
        configs[101] = "B"

        val display1 = resolveComplication(101, configs, syncData)
        val display2 = resolveComplication(102, configs, syncData)

        assertEquals("Полтава", display1.regionName)
        assertEquals("🟢", display1.shortText)

        // Instance 2 must remain configured for B and be unaffected
        assertEquals("Полтава", display2.regionName)
        assertEquals("🟢", display2.shortText)
    }

    @Test
    fun testCase4_DisableSyncForB_GracefulOffline() {
        val profileA = Profile(id = "A", regionName = "Київ", activeOnWatch = true)
        // B had sync disabled on phone -> removed from Wear OS profiles
        val syncData = WatchSyncData(
            profiles = listOf(profileA),
            statuses = mapOf("A" to AlertStatus(isAlarm = false, regionName = "Київ"))
        )

        val configs = mapOf(101 to "A", 102 to "B")

        val display2 = resolveComplication(102, configs, syncData)

        // Instance 2 referencing B must display Offline, not crash, not switch to A
        assertTrue(display2.isOffline)
        assertEquals("—", display2.shortText)
        assertEquals("⚠️", display2.shortTitle)
        assertEquals("Немає зв'язку", display2.longHeader)
    }

    @Test
    fun testCase5_DeleteB_GracefulOfflineNoCrash() {
        // B was deleted on phone -> disappeared from synced data
        val syncData = WatchSyncData(
            profiles = emptyList(),
            statuses = emptyMap()
        )

        val configs = mapOf(102 to "B")

        val display = resolveComplication(102, configs, syncData)

        assertTrue(display.isOffline)
        assertEquals("—", display.shortText)
        assertEquals("⚠️", display.shortTitle)
    }

    @Test
    fun testCase6_ReEnableSyncForB_ResolvesAutomatically() {
        val configs = mapOf(102 to "B")

        // Initially B is unsynchronized -> offline
        val syncDataOffline = WatchSyncData(profiles = emptyList(), statuses = emptyMap())
        val displayOffline = resolveComplication(102, configs, syncDataOffline)
        assertTrue(displayOffline.isOffline)

        // B becomes synchronized again with the same stable profileId
        val profileB = Profile(id = "B", regionName = "Полтава", activeOnWatch = true)
        val statusB = AlertStatus(isAlarm = true, level = "yellow", regionName = "Полтава", reasons = listOf("Дрони"))
        val syncDataRestored = WatchSyncData(
            profiles = listOf(profileB),
            statuses = mapOf("B" to statusB)
        )

        val displayRestored = resolveComplication(102, configs, syncDataRestored)

        // Resolves automatically without needing reconfiguration
        assertFalse(displayRestored.isOffline)
        assertTrue(displayRestored.isAlarm)
        assertEquals("🟡", displayRestored.shortText)
        assertEquals("ЖОВ", displayRestored.shortTitle)
        assertEquals("Полтава", displayRestored.regionName)
    }

    @Test
    fun testCase7_PersistenceRoundTripAcrossRestarts() {
        val profileA = Profile(id = "A", regionName = "Київ", activeOnWatch = true)
        val profileB = Profile(id = "B", regionName = "Полтава", activeOnWatch = true)
        val statusA = AlertStatus(isAlarm = true, level = "red", regionName = "Київ")
        val statusB = AlertStatus(isAlarm = false, regionName = "Полтава")

        val syncData = WatchSyncData(
            profiles = listOf(profileA, profileB),
            statuses = mapOf("A" to statusA, "B" to statusB),
            updatedAt = 987654321L
        )

        // Serialize to JSON (as stored in SharedPreferences)
        val serializedJson = json.encodeToString(WatchSyncData.serializer(), syncData)

        // Simulate app/device restart: deserialize from JSON
        val restoredData = json.decodeFromString<WatchSyncData>(serializedJson)

        assertEquals(2, restoredData.profiles.size)
        assertEquals(987654321L, restoredData.updatedAt)
        assertEquals("Київ", restoredData.statuses["A"]?.regionName)
        assertEquals("Полтава", restoredData.statuses["B"]?.regionName)

        val configs = mapOf(101 to "A", 102 to "B")
        val display1 = resolveComplication(101, configs, restoredData)
        val display2 = resolveComplication(102, configs, restoredData)

        assertEquals("🔴", display1.shortText)
        assertEquals("🟢", display2.shortText)
    }

    @Test
    fun testUnconfiguredComplication_DisplaysOffline() {
        val profileA = Profile(id = "A", regionName = "Київ", activeOnWatch = true)
        val syncData = WatchSyncData(
            profiles = listOf(profileA),
            statuses = mapOf("A" to AlertStatus(isAlarm = false, regionName = "Київ"))
        )

        // Complication instance 999 has no configuration mapping
        val configs = emptyMap<Int, String>()

        val display = resolveComplication(999, configs, syncData)
        assertTrue(display.isOffline)
        assertEquals("—", display.shortText)
        assertEquals("⚠️", display.shortTitle)
    }

    @Test
    fun testLanguageSyncAndLocalizedProfileNames() {
        val profile = Profile(
            id = "test_profile",
            regionId = "odeska",
            regionName = "Одеська область",
            districtId = "odeska:odeskyi",
            districtName = "Одеський район",
            activeOnWatch = true
        )
        val syncDataEn = WatchSyncData(
            profiles = listOf(profile),
            language = "en"
        )
        val syncDataUk = WatchSyncData(
            profiles = listOf(profile),
            language = "uk"
        )

        assertEquals("en", syncDataEn.language)
        assertEquals("uk", syncDataUk.language)

        assertEquals("Odesa District", profile.getLocalizedDisplayName(syncDataEn.language))
        assertEquals("Odesa Oblast", profile.getLocalizedRegionName(syncDataEn.language))

        assertEquals("Одеський район", profile.getLocalizedDisplayName(syncDataUk.language))
        assertEquals("Одеська область", profile.getLocalizedRegionName(syncDataUk.language))
    }
}
