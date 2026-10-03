package ua.alerts.mobile.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.alerts.mobile.data.SettingsRepository
import ua.alerts.mobile.service.AlertForegroundService
import ua.alerts.shared.logging.AppLog

class BootReceiver(
    private val isMonitoringEnabled: suspend (Context) -> Boolean = { context ->
        SettingsRepository(context).globalMonitoring.first()
    },
    private val startServiceAction: (Context) -> Unit = { context ->
        AlertForegroundService.startService(context)
    },
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val pendingResult = try { goAsync() } catch (_: Throwable) { null }
        handleAction(context, intent.action) {
            pendingResult?.finish()
        }
    }

    internal fun handleAction(
        context: Context,
        action: String?,
        onComplete: () -> Unit = {}
    ) {
        if (action !in SUPPORTED_ACTIONS) {
            AppLog.w(TAG) { "Received unsupported action: $action, ignoring." }
            onComplete()
            return
        }

        AppLog.i(TAG) { "Broadcast received: $action. Evaluating global monitoring status..." }
        val appContext = context.applicationContext ?: context

        CoroutineScope(coroutineDispatcher).launch {
            try {
                val shouldStart = isMonitoringEnabled(appContext)
                if (shouldStart) {
                    AppLog.i(TAG) { "Global monitoring is enabled. Restoring AlertForegroundService after $action." }
                    startServiceAction(appContext)
                } else {
                    AppLog.i(TAG) { "Global monitoring is disabled. Skipping AlertForegroundService startup." }
                }
            } catch (t: Throwable) {
                AppLog.e(TAG, t) { "Failed to evaluate settings or restore monitoring on $action" }
            } finally {
                onComplete()
            }
        }
    }
}
