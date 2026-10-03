package ua.alerts.core.logging

import ua.alerts.shared.logging.AppLog
import ua.alerts.shared.logging.LogLevel

/**
 * Convenience initializer to install [AndroidLogBackend] into [AppLog]
 * with an appropriate log level threshold based on debug status.
 */
object AndroidLogInitializer {

    /**
     * Installs [AndroidLogBackend] into [AppLog].
     *
     * @param isDebug True if running a debug variant (enables all logs including verbose/debug/info).
     *                False if running a release variant (restricts logs to warning and error).
     */
    fun init(isDebug: Boolean) {
        val minLevel = if (isDebug) LogLevel.VERBOSE else LogLevel.WARN
        AppLog.init(AndroidLogBackend(), minLevel)
    }
}
