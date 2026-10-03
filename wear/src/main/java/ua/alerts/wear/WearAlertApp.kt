package ua.alerts.wear

import android.app.Application
import ua.alerts.core.logging.AndroidLogInitializer
import ua.alerts.wear.data.WatchAlertRepository

class WearAlertApp : Application() {

    lateinit var alertRepository: WatchAlertRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidLogInitializer.init(isDebug = BuildConfig.DEBUG)
        instance = this
        alertRepository = WatchAlertRepository(this)
    }

    companion object {
        lateinit var instance: WearAlertApp
            private set

        val instanceOrNull: WearAlertApp?
            get() = if (::instance.isInitialized) instance else null
    }
}
