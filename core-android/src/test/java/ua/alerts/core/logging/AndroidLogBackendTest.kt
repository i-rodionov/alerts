package ua.alerts.core.logging

import org.junit.Assert.assertNotNull
import org.junit.Test
import ua.alerts.shared.logging.AppLog
import ua.alerts.shared.logging.LogLevel

class AndroidLogBackendTest {

    @Test
    fun testAndroidLogBackendDispatchesWithoutException() {
        val backend = AndroidLogBackend()
        backend.v("TestTag", "Verbose message")
        backend.d("TestTag", "Debug message")
        backend.i("TestTag", "Info message")

        val throwable = RuntimeException("Test exception")
        backend.w("TestTag", "Warn message", throwable)
        backend.w("TestTag", "Warn message without throwable", null)
        backend.e("TestTag", "Error message", throwable)
        backend.e("TestTag", "Error message without throwable", null)
        assertNotNull(backend)
    }

    @Test
    fun testAndroidLogInitializerConfiguresAppLog() {
        AndroidLogInitializer.init(isDebug = true)
        org.junit.Assert.assertEquals(LogLevel.VERBOSE, AppLog.minLevel)
        org.junit.Assert.assertTrue(AppLog.backend is AndroidLogBackend)

        AndroidLogInitializer.init(isDebug = false)
        org.junit.Assert.assertEquals(LogLevel.WARN, AppLog.minLevel)
        org.junit.Assert.assertTrue(AppLog.backend is AndroidLogBackend)

        AppLog.reset()
    }
}
