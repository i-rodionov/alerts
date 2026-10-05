package ua.alerts.mobile.receiver

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {

    private val dummyContext: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @Test
    fun testBootCompletedStartsServiceWhenMonitoringEnabled() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var serviceStarted = false
        var completed = false

        val receiver = BootReceiver(
            isMonitoringEnabled = { true },
            startServiceAction = { serviceStarted = true },
            coroutineDispatcher = testDispatcher
        )

        receiver.handleAction(dummyContext, Intent.ACTION_BOOT_COMPLETED) {
            completed = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(serviceStarted)
        assertTrue(completed)
    }

    @Test
    fun testBootCompletedDoesNotStartServiceWhenMonitoringDisabled() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var serviceStarted = false
        var completed = false

        val receiver = BootReceiver(
            isMonitoringEnabled = { false },
            startServiceAction = { serviceStarted = true },
            coroutineDispatcher = testDispatcher
        )

        receiver.handleAction(dummyContext, Intent.ACTION_BOOT_COMPLETED) {
            completed = true
        }

        testScheduler.advanceUntilIdle()

        assertFalse(serviceStarted)
        assertTrue(completed)
    }

    @Test
    fun testPackageReplacedStartsServiceWhenMonitoringEnabled() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var serviceStarted = false
        var completed = false

        val receiver = BootReceiver(
            isMonitoringEnabled = { true },
            startServiceAction = { serviceStarted = true },
            coroutineDispatcher = testDispatcher
        )

        receiver.handleAction(dummyContext, Intent.ACTION_MY_PACKAGE_REPLACED) {
            completed = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(serviceStarted)
        assertTrue(completed)
    }

    @Test
    fun testQuickbootPoweronStartsServiceWhenMonitoringEnabled() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var serviceStarted = false
        var completed = false

        val receiver = BootReceiver(
            isMonitoringEnabled = { true },
            startServiceAction = { serviceStarted = true },
            coroutineDispatcher = testDispatcher
        )

        receiver.handleAction(dummyContext, BootReceiver.ACTION_QUICKBOOT_POWERON) {
            completed = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(serviceStarted)
        assertTrue(completed)
    }

    @Test
    fun testUnsupportedActionIgnored() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var serviceStarted = false
        var settingsChecked = false
        var completed = false

        val receiver = BootReceiver(
            isMonitoringEnabled = {
                settingsChecked = true
                true
            },
            startServiceAction = { serviceStarted = true },
            coroutineDispatcher = testDispatcher
        )

        receiver.handleAction(dummyContext, "android.intent.action.AIRPLANE_MODE") {
            completed = true
        }

        testScheduler.advanceUntilIdle()

        assertFalse(serviceStarted)
        assertFalse(settingsChecked)
        assertTrue(completed)
    }

    @Test
    fun testExceptionInSettingsHandledGracefully() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var serviceStarted = false
        var completed = false

        val receiver = BootReceiver(
            isMonitoringEnabled = { throw IllegalStateException("Storage error") },
            startServiceAction = { serviceStarted = true },
            coroutineDispatcher = testDispatcher
        )

        receiver.handleAction(dummyContext, Intent.ACTION_BOOT_COMPLETED) {
            completed = true
        }

        testScheduler.advanceUntilIdle()

        assertFalse(serviceStarted)
        assertTrue(completed)
    }

    @Test
    fun testSupportedActionsContainsAllExpectedTriggers() {
        assertTrue(BootReceiver.SUPPORTED_ACTIONS.contains(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(BootReceiver.SUPPORTED_ACTIONS.contains(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertTrue(BootReceiver.SUPPORTED_ACTIONS.contains(BootReceiver.ACTION_QUICKBOOT_POWERON))
        assertTrue(BootReceiver.SUPPORTED_ACTIONS.contains(BootReceiver.ACTION_HTC_QUICKBOOT_POWERON))
        assertEquals(4, BootReceiver.SUPPORTED_ACTIONS.size)
    }
}
