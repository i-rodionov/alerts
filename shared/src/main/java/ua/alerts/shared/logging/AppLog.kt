package ua.alerts.shared.logging

/**
 * High-performance, zero-allocation logging facade for pure Kotlin and Android modules.
 *
 * Uses [inline] functions and lambda expressions ([() -> String]) to guarantee that string
 * concatenation and formatting are completely skipped when the active log level is below [minLevel].
 * In release builds ([minLevel] = [LogLevel.WARN]), verbose, debug, and info lambdas are never evaluated.
 */
object AppLog {

    @Volatile
    var backend: LogBackend? = null

    @Volatile
    var minLevel: LogLevel = LogLevel.WARN

    fun init(backend: LogBackend, minLevel: LogLevel) {
        this.backend = backend
        this.minLevel = minLevel
    }

    fun reset() {
        backend = null
        minLevel = LogLevel.WARN
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun v(tag: String, msg: String) {
        if (minLevel <= LogLevel.VERBOSE) {
            backend?.v(tag, msg)
        }
    }

    inline fun v(tag: String, msg: () -> String) {
        if (minLevel <= LogLevel.VERBOSE) {
            backend?.v(tag, msg())
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun d(tag: String, msg: String) {
        if (minLevel <= LogLevel.DEBUG) {
            backend?.d(tag, msg)
        }
    }

    inline fun d(tag: String, msg: () -> String) {
        if (minLevel <= LogLevel.DEBUG) {
            backend?.d(tag, msg())
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun i(tag: String, msg: String) {
        if (minLevel <= LogLevel.INFO) {
            backend?.i(tag, msg)
        }
    }

    inline fun i(tag: String, msg: () -> String) {
        if (minLevel <= LogLevel.INFO) {
            backend?.i(tag, msg())
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (minLevel <= LogLevel.WARN) {
            backend?.w(tag, msg, tr)
        }
    }

    inline fun w(tag: String, tr: Throwable? = null, msg: () -> String) {
        if (minLevel <= LogLevel.WARN) {
            backend?.w(tag, msg(), tr)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (minLevel <= LogLevel.ERROR) {
            backend?.e(tag, msg, tr)
        }
    }

    inline fun e(tag: String, tr: Throwable? = null, msg: () -> String) {
        if (minLevel <= LogLevel.ERROR) {
            backend?.e(tag, msg(), tr)
        }
    }
}
