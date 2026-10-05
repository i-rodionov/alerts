package ua.alerts.shared.logging

/**
 * Severity levels for application logging.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Pluggable backend contract for logging implementations.
 */
interface LogBackend {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
