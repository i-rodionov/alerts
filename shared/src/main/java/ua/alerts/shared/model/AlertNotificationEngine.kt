package ua.alerts.shared.model

class AlertNotificationEngine(
    private val baselines: MutableMap<String, AlertLevel> = mutableMapOf()
) {
    val currentBaselines: Map<String, AlertLevel>
        get() = baselines.toMap()

    fun initializeBaselines(persistedBaselines: Map<String, AlertLevel>) {
        baselines.putAll(persistedBaselines)
    }

    fun pruneDeletedProfiles(activeProfileIds: Set<String>) {
        baselines.keys.retainAll { storageKey ->
            AlertTargetKey.fromStorageKey(storageKey).profileId in activeProfileIds
        }
    }

    data class TransitionResult(
        val targetKey: String,
        val profile: Profile,
        val status: AlertStatus,
        val currentLevel: AlertLevel,
        val shouldNotify: Boolean,
        val isNewBaseline: Boolean
    )

    fun evaluateTransitions(
        profiles: List<Profile>,
        statusMap: Map<String, AlertStatus>,
        isGlobalEnabled: Boolean
    ): List<TransitionResult> {
        val results = mutableListOf<TransitionResult>()
        for (profile in profiles) {
            val status = statusMap[profile.id] ?: continue
            val currentLevel = status.toAlertLevel()
            val targetKey = AlertTargetKey.fromProfile(profile).storageKey
            val prevLevel = baselines[targetKey]
            val isMonitored = isGlobalEnabled && profile.backgroundMonitoring

            if (prevLevel == null) {
                // Initial baseline establishment: must never trigger false notification!
                baselines[targetKey] = currentLevel
                results.add(
                    TransitionResult(
                        targetKey = targetKey,
                        profile = profile,
                        status = status,
                        currentLevel = currentLevel,
                        shouldNotify = false,
                        isNewBaseline = true
                    )
                )
            } else {
                if (AlertTransitionEvaluator.shouldNotifyTransition(prevLevel, currentLevel)) {
                    baselines[targetKey] = currentLevel
                    results.add(
                        TransitionResult(
                            targetKey = targetKey,
                            profile = profile,
                            status = status,
                            currentLevel = currentLevel,
                            shouldNotify = isMonitored,
                            isNewBaseline = false
                        )
                    )
                }
            }
        }
        return results
    }
}
