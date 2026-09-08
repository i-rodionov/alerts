package ua.alerts.mobile.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NeptunClientTest {

    @Test
    fun testExponentialBackoffDelays() {
        assertEquals(2000L, NeptunClient.calculateBackoffDelayMs(0))
        assertEquals(4000L, NeptunClient.calculateBackoffDelayMs(1))
        assertEquals(8000L, NeptunClient.calculateBackoffDelayMs(2))
        assertEquals(16000L, NeptunClient.calculateBackoffDelayMs(3))
        assertEquals(30000L, NeptunClient.calculateBackoffDelayMs(4))
        assertEquals(30000L, NeptunClient.calculateBackoffDelayMs(5))
        assertEquals(30000L, NeptunClient.calculateBackoffDelayMs(10))
    }

    @Test
    fun testInitialStateIsStopped() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val client = NeptunClient(scope = testScope)

        assertEquals(ConnectionStatus.STOPPED, client.connectionStatus.value)
        assertFalse(client.isRunning)
        assertNull(client.lastError.value)
    }

    @Test
    fun testStopSetsStateToStopped() = runTest {
        val client = NeptunClient(scope = this)

        client.stop()
        assertEquals(ConnectionStatus.STOPPED, client.connectionStatus.value)
        assertFalse(client.isRunning)
        assertNull(client.lastError.value)
    }

    @Test
    fun testStartIdempotency() = runTest {
        val client = NeptunClient(scope = this)

        // Starting sets isRunning true
        client.start()
        assertTrue(client.isRunning)

        // Calling start again when already started does not duplicate or reset
        client.start()
        assertTrue(client.isRunning)

        client.stop()
        assertFalse(client.isRunning)
        assertEquals(ConnectionStatus.STOPPED, client.connectionStatus.value)
    }

    @Test
    fun testDecodeWsEnvelopes() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        val alertsEnvelope = """{"type":"alerts","data":{"raions":[{"key":"київський","name":"Київський район","level":"yellow","reasons":["Дронова загроза"]}],"oblasts":[]}}"""
        val env1 = json.decodeFromString<ua.alerts.shared.model.NeptunWsEnvelope>(alertsEnvelope)
        assertEquals("alerts", env1.type)
        val alertsData = json.decodeFromJsonElement<ua.alerts.shared.model.NeptunAlertsResponse>(env1.data!!)
        assertEquals(1, alertsData.raions.size)
        assertEquals("yellow", alertsData.raions[0].level)
        assertEquals(listOf("Дронова загроза"), alertsData.raions[0].reasons)

        val snapshotEnvelope = """{"type":"snapshot","data":{"threats":[{"id":"t1","type":"shahed"}]}}"""
        val env2 = json.decodeFromString<ua.alerts.shared.model.NeptunWsEnvelope>(snapshotEnvelope)
        assertEquals("snapshot", env2.type)

        val upsertEnvelope = """{"type":"upsert","data":{"id":"t1","speed":180}}"""
        val env3 = json.decodeFromString<ua.alerts.shared.model.NeptunWsEnvelope>(upsertEnvelope)
        assertEquals("upsert", env3.type)

        val removeEnvelope = """{"type":"remove","data":{"id":"t1"}}"""
        val env4 = json.decodeFromString<ua.alerts.shared.model.NeptunWsEnvelope>(removeEnvelope)
        assertEquals("remove", env4.type)

        val heartbeatEnvelope = """{"type":"heartbeat"}"""
        val env5 = json.decodeFromString<ua.alerts.shared.model.NeptunWsEnvelope>(heartbeatEnvelope)
        assertEquals("heartbeat", env5.type)
    }
}
