package ua.alerts.mobile.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.decodeFromJsonElement
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Test
import java.util.concurrent.TimeUnit

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

        val realWsFrame = """{"type":"alerts","ts":"2026-09-08T12:59:39.962Z","data":{"version":1788872379,"updatedAt":"2026-09-08T12:59:39.488432003Z","raions":[{"key":"бахмутський","name":"Бахмутський район","oblast":"Донецька область","since":"2026-09-08T04:42:56.778799Z","level":"yellow","reasons":["Дронова загроза (жовтий рівень)"]}]}}"""
        val realEnv = json.decodeFromString<ua.alerts.shared.model.NeptunWsEnvelope>(realWsFrame)
        assertEquals("alerts", realEnv.type)
        assertEquals("2026-09-08T12:59:39.962Z", realEnv.ts)
        val realAlertsData = json.decodeFromJsonElement<ua.alerts.shared.model.NeptunAlertsResponse>(realEnv.data!!)
        assertEquals(1788872379L, realAlertsData.version)
        assertEquals("2026-09-08T12:59:39.488432003Z", realAlertsData.updatedAt)
        assertEquals(1, realAlertsData.raions.size)
        assertEquals("2026-09-08T04:42:56.778799Z", realAlertsData.raions[0].since)
        assertEquals("yellow", realAlertsData.raions[0].level)

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

    @Test
    fun testFetchAlertsSnapshotSuccess() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val jsonBody = """{"raions":[{"key":"київський","name":"Київський район","level":"red"}],"oblasts":[]}"""
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .setHeader("Content-Type", "application/json")
                    .body(jsonBody)
                    .build()
            )

            val client = NeptunClient(
                scope = this,
                restAlertsUrl = server.url("/api/v1/alerts").toString()
            )

            val result = client.fetchAlertsSnapshot()
            assertTrue(result.isSuccess)
            val alerts = result.getOrNull()!!
            assertEquals(1, alerts.raions.size)
            assertEquals("київський", alerts.raions[0].key)
            assertEquals("red", alerts.raions[0].level)

            val emitted = client.alertsFlow.first()
            assertEquals("київський", emitted.raions[0].key)
        } finally {
            server.close()
        }
    }

    @Test
    fun testFetchAlertsSnapshotHttpError() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(500)
                    .body("Internal Server Error")
                    .build()
            )

            val client = NeptunClient(
                scope = this,
                restAlertsUrl = server.url("/api/v1/alerts").toString()
            )

            val result = client.fetchAlertsSnapshot()
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
        } finally {
            server.close()
        }
    }

    @Test
    fun testFetchAlertsSnapshotEmptyBody() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .build()
            )

            val client = NeptunClient(
                scope = this,
                restAlertsUrl = server.url("/api/v1/alerts").toString()
            )

            val result = client.fetchAlertsSnapshot()
            assertTrue(result.isFailure)
        } finally {
            server.close()
        }
    }

    @Test
    fun testFetchAlertsSnapshotCancellation() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body("""{"raions":[],"oblasts":[]}""")
                    .headersDelay(5, TimeUnit.SECONDS)
                    .build()
            )

            val client = NeptunClient(
                scope = this,
                restAlertsUrl = server.url("/api/v1/alerts").toString()
            )

            val job = launch {
                client.fetchAlertsSnapshot()
            }
            job.cancel()
            job.join()
            assertTrue(job.isCancelled)
        } finally {
            server.close()
        }
    }
}
