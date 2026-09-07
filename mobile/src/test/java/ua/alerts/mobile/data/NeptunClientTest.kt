package ua.alerts.mobile.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
}
