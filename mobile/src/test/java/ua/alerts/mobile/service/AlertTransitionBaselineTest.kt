package ua.alerts.mobile.service

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.shared.model.AlertLevel
import ua.alerts.shared.model.AlertNotificationEngine
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.AlertTargetKey
import ua.alerts.shared.model.Profile
import java.io.File

class AlertTransitionBaselineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dummyContext: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private fun createTestSettingsRepo(scope: CoroutineScope): SettingsRepository {
        val testFile = File(tempFolder.newFolder(), "test_settings_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { testFile }
        )
        return SettingsRepository(context = dummyContext, dataStore = dataStore)
    }

    // 1. Normal transition: GREEN -> RED notifies alert, RED -> GREEN notifies all-clear
    @Test
    fun test1_NormalTransition_GreenToRed_and_RedToGreen() {
        val engine = AlertNotificationEngine()
        val profile = Profile(id = "p1", regionId = "kyivska", regionName = "Київська")
        val targetKey = AlertTargetKey.fromProfile(profile).storageKey

        // Baseline initialization with GREEN (NO_ALERT)
        val initialStatus = AlertStatus(isAlarm = false)
        val initTransitions = engine.evaluateTransitions(listOf(profile), mapOf("p1" to initialStatus), isGlobalEnabled = true)
        assertEquals(1, initTransitions.size)
        assertFalse("Baseline initialization must never notify", initTransitions[0].shouldNotify)
        assertTrue(initTransitions[0].isNewBaseline)
        assertEquals(AlertLevel.NO_ALERT, engine.currentBaselines[targetKey])

        // GREEN -> RED transition
        val redStatus = AlertStatus(isAlarm = true, level = "red")
        val redTransitions = engine.evaluateTransitions(listOf(profile), mapOf("p1" to redStatus), isGlobalEnabled = true)
        assertEquals(1, redTransitions.size)
        assertTrue("GREEN -> RED must trigger notification", redTransitions[0].shouldNotify)
        assertEquals(AlertLevel.RED, redTransitions[0].currentLevel)
        assertEquals(AlertLevel.RED, engine.currentBaselines[targetKey])

        // RED -> GREEN (all-clear) transition
        val clearStatus = AlertStatus(isAlarm = false)
        val clearTransitions = engine.evaluateTransitions(listOf(profile), mapOf("p1" to clearStatus), isGlobalEnabled = true)
        assertEquals(1, clearTransitions.size)
        assertTrue("RED -> GREEN must trigger all-clear notification", clearTransitions[0].shouldNotify)
        assertEquals(AlertLevel.NO_ALERT, clearTransitions[0].currentLevel)
        assertEquals(AlertLevel.NO_ALERT, engine.currentBaselines[targetKey])
    }

    // 2. No transition: GREEN -> GREEN produces no notification
    @Test
    fun test2_NoTransition_GreenToGreen() {
        val engine = AlertNotificationEngine()
        val profile = Profile(id = "p1", regionId = "kyivska", regionName = "Київська")

        // Seed baseline as GREEN
        val statusGreen = AlertStatus(isAlarm = false)
        engine.evaluateTransitions(listOf(profile), mapOf("p1" to statusGreen), isGlobalEnabled = true)

        // Same status arrives again
        val transitions = engine.evaluateTransitions(listOf(profile), mapOf("p1" to statusGreen), isGlobalEnabled = true)
        assertTrue("Same level must not trigger any transitions", transitions.isEmpty())
    }

    // 3. Process restart while alert is active: persisted GREEN -> process dies -> current state RED -> notifies on restart
    @Test
    fun test3_ProcessRestartWhileAlertIsActive() = runTest {
        val settingsRepo = createTestSettingsRepo(backgroundScope)
        val profile = Profile(id = "p1", regionId = "kyivska", regionName = "Київська")
        val targetKey = AlertTargetKey.fromProfile(profile).storageKey

        // Persist baseline = NO_ALERT (GREEN)
        settingsRepo.setAlertBaseline(targetKey, AlertLevel.NO_ALERT)

        // Process dies and restarts: new engine instance created, loads baseline from DataStore
        val engineAfterRestart = AlertNotificationEngine()
        val loadedBaselines = settingsRepo.getAlertBaselines()
        engineAfterRestart.initializeBaselines(loadedBaselines)
        assertEquals(AlertLevel.NO_ALERT, engineAfterRestart.currentBaselines[targetKey])

        // State while process was dead became RED
        val currentRedStatus = AlertStatus(isAlarm = true, level = "red")
        val transitions = engineAfterRestart.evaluateTransitions(
            listOf(profile),
            mapOf("p1" to currentRedStatus),
            isGlobalEnabled = true
        )

        assertEquals(1, transitions.size)
        assertTrue("Restart with state change during downtime must notify", transitions[0].shouldNotify)
        assertEquals(AlertLevel.RED, transitions[0].currentLevel)
        assertEquals(AlertLevel.RED, engineAfterRestart.currentBaselines[targetKey])

        // Update persisted baseline to RED
        settingsRepo.setAlertBaseline(targetKey, transitions[0].currentLevel)
        assertEquals(AlertLevel.RED, settingsRepo.getAlertBaselines()[targetKey])
    }

    // 4. Restart without a transition: persisted RED -> service restarts while still RED -> no duplicate notification
    @Test
    fun test4_RestartWithoutTransition() = runTest {
        val settingsRepo = createTestSettingsRepo(backgroundScope)
        val profile = Profile(id = "p1", regionId = "kyivska", regionName = "Київська")
        val targetKey = AlertTargetKey.fromProfile(profile).storageKey

        // Persist baseline = RED
        settingsRepo.setAlertBaseline(targetKey, AlertLevel.RED)

        // Service restarts
        val engine = AlertNotificationEngine()
        engine.initializeBaselines(settingsRepo.getAlertBaselines())

        // Current state received from WebSocket snapshot is still RED
        val currentRedStatus = AlertStatus(isAlarm = true, level = "red")
        val transitions = engine.evaluateTransitions(
            listOf(profile),
            mapOf("p1" to currentRedStatus),
            isGlobalEnabled = true
        )

        assertTrue("Restart with unchanged active alarm must NOT notify duplicate", transitions.isEmpty())
    }

    // 5. Region change: (id=1, region=A) RED -> (id=1, region=B) GREEN: no false all-clear notification
    @Test
    fun test5_RegionChange_DoesNotTriggerFalseTransition() {
        val engine = AlertNotificationEngine()
        val profileRegionA = Profile(id = "p1", regionId = "region_A", regionName = "Region A")
        val targetKeyA = AlertTargetKey.fromProfile(profileRegionA).storageKey

        // Profile on Region A establishes RED baseline
        engine.initializeBaselines(mapOf(targetKeyA to AlertLevel.RED))

        // User changes profile target to Region B, which currently has GREEN
        val profileRegionB = profileRegionA.copy(regionId = "region_B", regionName = "Region B")
        val targetKeyB = AlertTargetKey.fromProfile(profileRegionB).storageKey
        val statusRegionB = AlertStatus(isAlarm = false)

        val transitions = engine.evaluateTransitions(
            listOf(profileRegionB),
            mapOf("p1" to statusRegionB),
            isGlobalEnabled = true
        )

        assertEquals(1, transitions.size)
        assertFalse("First observation of new region must NOT trigger notification", transitions[0].shouldNotify)
        assertTrue("Must be marked as new baseline", transitions[0].isNewBaseline)
        assertEquals(AlertLevel.NO_ALERT, transitions[0].currentLevel)
        assertEquals(AlertLevel.NO_ALERT, engine.currentBaselines[targetKeyB])

        // Baseline for region A remains intact and independent
        assertEquals(AlertLevel.RED, engine.currentBaselines[targetKeyA])
    }

    // 6. Region change followed by real transition: first GREEN establishes baseline, later RED notifies
    @Test
    fun test6_RegionChange_FollowedByRealTransition() {
        val engine = AlertNotificationEngine()
        val profileRegionA = Profile(id = "p1", regionId = "region_A", regionName = "Region A")
        engine.initializeBaselines(mapOf(AlertTargetKey.fromProfile(profileRegionA).storageKey to AlertLevel.RED))

        // Switch to Region B (GREEN)
        val profileRegionB = profileRegionA.copy(regionId = "region_B", regionName = "Region B")
        val targetKeyB = AlertTargetKey.fromProfile(profileRegionB).storageKey
        val statusGreen = AlertStatus(isAlarm = false)

        val initTransitions = engine.evaluateTransitions(listOf(profileRegionB), mapOf("p1" to statusGreen), isGlobalEnabled = true)
        assertFalse(initTransitions[0].shouldNotify)

        // Later Region B turns RED
        val statusRed = AlertStatus(isAlarm = true, level = "red")
        val alertTransitions = engine.evaluateTransitions(listOf(profileRegionB), mapOf("p1" to statusRed), isGlobalEnabled = true)

        assertEquals(1, alertTransitions.size)
        assertTrue("Subsequent real transition on region B must notify", alertTransitions[0].shouldNotify)
        assertEquals(AlertLevel.RED, alertTransitions[0].currentLevel)
        assertEquals(AlertLevel.RED, engine.currentBaselines[targetKeyB])
    }

    // 7. District change: same behavior as region change, distinct baseline per district
    @Test
    fun test7_DistrictChange_IndependentBaselines() {
        val engine = AlertNotificationEngine()
        val profileDistrictX = Profile(id = "p1", regionId = "kyivska", districtId = "bila_tserkva")
        val targetKeyX = AlertTargetKey.fromProfile(profileDistrictX).storageKey
        engine.initializeBaselines(mapOf(targetKeyX to AlertLevel.RED))

        // Change district to boryspil which is GREEN
        val profileDistrictY = profileDistrictX.copy(districtId = "boryspil")
        val targetKeyY = AlertTargetKey.fromProfile(profileDistrictY).storageKey
        val statusGreen = AlertStatus(isAlarm = false)

        val transitions = engine.evaluateTransitions(listOf(profileDistrictY), mapOf("p1" to statusGreen), isGlobalEnabled = true)
        assertEquals(1, transitions.size)
        assertFalse("District change must establish baseline silently", transitions[0].shouldNotify)
        assertEquals(AlertLevel.NO_ALERT, engine.currentBaselines[targetKeyY])
        assertEquals(AlertLevel.RED, engine.currentBaselines[targetKeyX])
    }

    // 8. Multiple profiles: baselines for different profile IDs remain independent
    @Test
    fun test8_MultipleProfilesIndependent() {
        val engine = AlertNotificationEngine()
        val profile1 = Profile(id = "p1", regionId = "kyivska")
        val profile2 = Profile(id = "p2", regionId = "lvivska")

        val statusKyivRed = AlertStatus(isAlarm = true, level = "red")
        val statusLvivGreen = AlertStatus(isAlarm = false)

        // Initialize both
        engine.evaluateTransitions(
            listOf(profile1, profile2),
            mapOf("p1" to statusKyivRed, "p2" to statusLvivGreen),
            isGlobalEnabled = true
        )

        // Lviv turns RED -> only p2 notifies
        val statusLvivRed = AlertStatus(isAlarm = true, level = "red")
        val transitionsLviv = engine.evaluateTransitions(
            listOf(profile1, profile2),
            mapOf("p1" to statusKyivRed, "p2" to statusLvivRed),
            isGlobalEnabled = true
        )

        assertEquals(1, transitionsLviv.size)
        assertEquals("p2", transitionsLviv[0].profile.id)
        assertTrue(transitionsLviv[0].shouldNotify)

        // Kyiv turns GREEN -> only p1 notifies
        val statusKyivGreen = AlertStatus(isAlarm = false)
        val transitionsKyiv = engine.evaluateTransitions(
            listOf(profile1, profile2),
            mapOf("p1" to statusKyivGreen, "p2" to statusLvivRed),
            isGlobalEnabled = true
        )

        assertEquals(1, transitionsKyiv.size)
        assertEquals("p1", transitionsKyiv[0].profile.id)
        assertTrue(transitionsKyiv[0].shouldNotify)
    }

    // 9. Same profile, different targets: baselines for different targets remain independent
    @Test
    fun test9_SameProfileDifferentTargets() {
        val engine = AlertNotificationEngine()
        val profileTarget1 = Profile(id = "p1", regionId = "region_A", districtId = "dist_1")
        val profileTarget2 = Profile(id = "p1", regionId = "region_B", districtId = "dist_2")

        val key1 = AlertTargetKey.fromProfile(profileTarget1).storageKey
        val key2 = AlertTargetKey.fromProfile(profileTarget2).storageKey

        engine.initializeBaselines(mapOf(key1 to AlertLevel.RED, key2 to AlertLevel.NO_ALERT))

        assertEquals(AlertLevel.RED, engine.currentBaselines[key1])
        assertEquals(AlertLevel.NO_ALERT, engine.currentBaselines[key2])

        // Switch back to target 1 with RED state -> no duplicate notification
        val transitions = engine.evaluateTransitions(
            listOf(profileTarget1),
            mapOf("p1" to AlertStatus(isAlarm = true, level = "red")),
            isGlobalEnabled = true
        )
        assertTrue("Known target with same state produces no notification", transitions.isEmpty())
    }

    // 10. Persistence failure / corrupted DataStore: handles gracefully without crashing or throwing
    @Test
    fun test10_PersistenceFailure_GracefulHandling() = runTest {
        // Inject corrupted JSON directly into the preference key
        val testFile = File(tempFolder.newFolder(), "test_corrupted_${System.nanoTime()}.preferences_pb")
        val corruptedDataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { testFile }
        )
        corruptedDataStore.edit { prefs ->
            prefs[SettingsRepository.KEY_ALERT_BASELINES_JSON] = "{ corrupted invalid json !!!"
        }

        val corruptedRepo = SettingsRepository(context = dummyContext, dataStore = corruptedDataStore)
        val baselines = corruptedRepo.getAlertBaselines()

        // Gracefully returns empty map, doesn't throw
        assertTrue("Corrupted JSON must gracefully yield empty map", baselines.isEmpty())

        // Can still write new valid baseline without crashing
        corruptedRepo.setAlertBaseline("p1:kyiv:", AlertLevel.RED)
        val updated = corruptedRepo.getAlertBaselines()
        assertEquals(AlertLevel.RED, updated["p1:kyiv:"])
    }

    // 11. Profile deletion: deleting a profile purges all its baselines from DataStore and memory
    @Test
    fun test11_ProfileDeletion_CleansUpBaselines() = runTest {
        val settingsRepo = createTestSettingsRepo(backgroundScope)
        val targetKey1 = "p1:kyivska:"
        val targetKey2 = "p2:lvivska:"

        settingsRepo.setAlertBaselines(
            mapOf(
                targetKey1 to AlertLevel.RED,
                "p1:other_region:district" to AlertLevel.YELLOW,
                targetKey2 to AlertLevel.NO_ALERT
            )
        )

        val before = settingsRepo.getAlertBaselines()
        assertEquals(3, before.size)

        // Delete profile 1
        settingsRepo.deleteProfile("p1")

        val remainingBaselines = settingsRepo.getAlertBaselines()
        assertFalse("p1 baselines must be purged", remainingBaselines.containsKey(targetKey1))
        assertFalse("p1 old target baselines must be purged", remainingBaselines.containsKey("p1:other_region:district"))
        assertTrue("p2 baseline must remain intact", remainingBaselines.containsKey(targetKey2))
        assertEquals(AlertLevel.NO_ALERT, remainingBaselines[targetKey2])

        // Engine pruning check
        val engine = AlertNotificationEngine()
        engine.initializeBaselines(mapOf(targetKey1 to AlertLevel.RED, targetKey2 to AlertLevel.NO_ALERT))
        engine.pruneDeletedProfiles(setOf("p2"))
        assertEquals(1, engine.currentBaselines.size)
        assertTrue(engine.currentBaselines.containsKey(targetKey2))
    }

    // 12. AlertTargetKey encoding and parsing roundtrip
    @Test
    fun test12_AlertTargetKey_Roundtrip() {
        val keyWithDistrict = AlertTargetKey(profileId = "prof-1", regionId = "kyiv", districtId = "raion-2")
        assertEquals("prof-1:kyiv:raion-2", keyWithDistrict.storageKey)
        val parsed1 = AlertTargetKey.fromStorageKey(keyWithDistrict.storageKey)
        assertEquals(keyWithDistrict, parsed1)

        val keyWithoutDistrict = AlertTargetKey(profileId = "prof-2", regionId = "lviv", districtId = null)
        assertEquals("prof-2:lviv:", keyWithoutDistrict.storageKey)
        val parsed2 = AlertTargetKey.fromStorageKey(keyWithoutDistrict.storageKey)
        assertEquals(keyWithoutDistrict, parsed2)
        assertNull(parsed2.districtId)
    }
}
