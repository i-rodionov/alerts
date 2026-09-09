package ua.alerts.shared.model

enum class AlertLevel {
    NO_ALERT,
    YELLOW,
    RED
}

fun AlertStatus.toAlertLevel(): AlertLevel {
    return when {
        !isAlarm -> AlertLevel.NO_ALERT
        isYellow -> AlertLevel.YELLOW
        else -> AlertLevel.RED
    }
}

object AlertTransitionEvaluator {
    /**
     * Determines whether an alert notification should be triggered for a profile.
     *
     * @param prev The previously known effective alert level for this profile.
     *             Null indicates UNKNOWN (baseline initialization), which must NOT notify.
     * @param current The new effective alert level for this profile.
     * @return true if and only if there is an actual meaningful transition between known states.
     */
    fun shouldNotifyTransition(prev: AlertLevel?, current: AlertLevel): Boolean {
        if (prev == null) return false
        return prev != current
    }
}
