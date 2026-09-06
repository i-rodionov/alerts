package ua.alerts.mobile.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import ua.alerts.mobile.AlertApp
import ua.alerts.mobile.R
import ua.alerts.mobile.ui.MainActivity
import ua.alerts.shared.model.AlertStatus

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_ALARM = 1002
    }

    fun buildServiceNotification(status: AlertStatus): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (status.isAlarm) {
            context.getString(R.string.status_alarm) + " ??"
        } else {
            context.getString(R.string.status_clear) + " ??"
        }

        val text = "${status.displayName} • ${context.getString(R.string.monitoring_active)}"

        return NotificationCompat.Builder(context, AlertApp.CHANNEL_SERVICE_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(if (status.isAlarm) R.drawable.ic_warning_siren else R.drawable.ic_shield_check)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun notifyAlarmTransition(
        status: AlertStatus,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (status.isAlarm) AlertApp.CHANNEL_ALERTS_ID else AlertApp.CHANNEL_CLEAR_ID
        val title = if (status.isAlarm) {
            "?? " + context.getString(R.string.status_alarm)
        } else {
            "?? " + context.getString(R.string.status_clear)
        }

        val message = "${status.displayName}: ${
            if (status.isAlarm) "Оголошено повітряну тривогу!" else "Відбій загрози."
        }"

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (status.isAlarm) R.drawable.ic_warning_siren else R.drawable.ic_shield_check)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (status.isAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        if (vibrateEnabled) {
            triggerVibration(status.isAlarm)
        }

        notificationManager.notify(NOTIFICATION_ID_ALARM, builder.build())
    }

    private fun triggerVibration(isAlarm: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = if (isAlarm) {
                    longArrayOf(0, 600, 200, 600, 200, 800)
                } else {
                    longArrayOf(0, 250, 150, 250)
                }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(600)
            }
        } catch (_: Exception) {}
    }
}
