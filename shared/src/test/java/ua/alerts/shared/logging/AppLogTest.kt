package ua.alerts.shared.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLogTest {

    private class RecordingLogBackend : LogBackend {
        val records = mutableListOf<String>()
        var lastThrowable: Throwable? = null

        override fun v(tag: String, message: String) { records.add("V:$tag:$message") }
        override fun d(tag: String, message: String) { records.add("D:$tag:$message") }
        override fun i(tag: String, message: String) { records.add("I:$tag:$message") }
        override fun w(tag: String, message: String, throwable: Throwable?) {
            records.add("W:$tag:$message")
            lastThrowable = throwable
        }
        override fun e(tag: String, message: String, throwable: Throwable?) {
            records.add("E:$tag:$message")
            lastThrowable = throwable
        }
    }

    @After
    fun tearDown() {
        AppLog.reset()
    }

    @Test
    fun testUninitializedSafeExecution() {
        AppLog.reset()
        // Calling logging methods before initialization must not crash
        AppLog.v("Tag", "msg")
        AppLog.v("Tag") { "lambda" }
        AppLog.d("Tag", "msg")
        AppLog.d("Tag") { "lambda" }
        AppLog.i("Tag", "msg")
        AppLog.i("Tag") { "lambda" }
        AppLog.w("Tag", "msg")
        AppLog.w("Tag") { "lambda" }
        AppLog.e("Tag", "msg")
        AppLog.e("Tag") { "lambda" }
        assertNull(AppLog.backend)
    }

    @Test
    fun testReleaseModeSuppressesDebugAndInfoLambdas() {
        val backend = RecordingLogBackend()
        AppLog.init(backend, minLevel = LogLevel.WARN)

        var vEvaluated = false
        var dEvaluated = false
        var iEvaluated = false
        var wEvaluated = false
        var eEvaluated = false

        AppLog.v("Tag") { vEvaluated = true; "v_text" }
        AppLog.d("Tag") { dEvaluated = true; "d_text" }
        AppLog.i("Tag") { iEvaluated = true; "i_text" }
        AppLog.w("Tag") { wEvaluated = true; "w_text" }
        AppLog.e("Tag") { eEvaluated = true; "e_text" }

        assertFalse("Verbose lambda must not be evaluated in release", vEvaluated)
        assertFalse("Debug lambda must not be evaluated in release", dEvaluated)
        assertFalse("Info lambda must not be evaluated in release", iEvaluated)
        assertTrue("Warn lambda must be evaluated in release", wEvaluated)
        assertTrue("Error lambda must be evaluated in release", eEvaluated)

        assertEquals(listOf("W:Tag:w_text", "E:Tag:e_text"), backend.records)
    }

    @Test
    fun testDebugModeEvaluatesAllLevels() {
        val backend = RecordingLogBackend()
        AppLog.init(backend, minLevel = LogLevel.VERBOSE)

        var vEvaluated = false
        var dEvaluated = false
        var iEvaluated = false
        var wEvaluated = false
        var eEvaluated = false

        AppLog.v("Tag") { vEvaluated = true; "v" }
        AppLog.d("Tag") { dEvaluated = true; "d" }
        AppLog.i("Tag") { iEvaluated = true; "i" }
        AppLog.w("Tag") { wEvaluated = true; "w" }
        AppLog.e("Tag") { eEvaluated = true; "e" }

        assertTrue(vEvaluated)
        assertTrue(dEvaluated)
        assertTrue(iEvaluated)
        assertTrue(wEvaluated)
        assertTrue(eEvaluated)

        assertEquals(listOf("V:Tag:v", "D:Tag:d", "I:Tag:i", "W:Tag:w", "E:Tag:e"), backend.records)
    }

    @Test
    fun testThrowablePassedCorrectly() {
        val backend = RecordingLogBackend()
        AppLog.init(backend, minLevel = LogLevel.WARN)

        val exception = IllegalStateException("Test exception")
        AppLog.w("Tag", "Warn with error", exception)
        assertEquals(exception, backend.lastThrowable)

        val errorException = RuntimeException("Fatal error")
        AppLog.e("Tag", errorException) { "Error lambda with error" }
        assertEquals(errorException, backend.lastThrowable)
    }
}
