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
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.ui.MainActivity
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile
import kotlin.math.abs

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_ALARM_BASE = 2000
    }

    fun buildServiceNotification(
        status: AlertStatus? = null,
        connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val connText = when (connectionStatus) {
            ConnectionStatus.CONNECTED -> context.getString(R.string.status_connected)
            ConnectionStatus.CONNECTING -> context.getString(R.string.status_connecting)
            ConnectionStatus.RECONNECTING -> context.getString(R.string.status_reconnecting)
            ConnectionStatus.ERROR -> context.getString(R.string.status_connection_error)
            ConnectionStatus.STOPPED -> context.getString(R.string.status_monitoring_disabled)
        }

        val hasStatus = status != null && status.regionName.isNotEmpty()
        val title = if (hasStatus) {
            when {
                status!!.isYellow -> context.getString(R.string.status_alarm_yellow) + " 🟡"
                status.isRed -> context.getString(R.string.status_alarm_red) + " 🔴"
                else -> context.getString(R.string.status_clear) + " 🟢"
            }
        } else {
            context.getString(R.string.app_name)
        }

        val regionPrefix = if (hasStatus) "${status!!.displayName} • " else ""
        val text = "$regionPrefix${context.getString(R.string.monitoring_active)} [$connText]"

        val smallIcon = if (hasStatus && status!!.isAlarm) {
            R.drawable.ic_warning_siren
        } else {
            R.drawable.ic_shield_check
        }

        return NotificationCompat.Builder(context, AlertApp.CHANNEL_SERVICE_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(smallIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun notifyAlarmTransition(
        profile: Profile,
        status: AlertStatus
    ) {
        val sound = if (status.isAlarm) profile.soundOnAlarm else profile.soundOnClear
        val vibrate = if (status.isAlarm) profile.vibrateOnAlarm else profile.vibrateOnClear

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            profile.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (status.isAlarm) AlertApp.CHANNEL_ALERTS_ID else AlertApp.CHANNEL_CLEAR_ID
        val title = if (status.isAlarm) {
            if (status.isYellow) {
                "🟡 " + context.getString(R.string.status_alarm_yellow)
            } else {
                "🔴 " + context.getString(R.string.status_alarm_red)
            }
        } else {
            "🟢 " + context.getString(R.string.status_clear)
        }

        val reasonSuffix = if (status.isAlarm && status.reasons.isNotEmpty()) " (${status.reasons.joinToString(", ")})" else ""
        val message = if (status.isAlarm) {
            "${profile.displayName}: Оголошено повітряну тривогу!$reasonSuffix"
        } else {
            "${profile.displayName}: Відбій повітряної тривоги."
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (status.isAlarm) R.drawable.ic_warning_siren else R.drawable.ic_shield_check)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (status.isAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        if (!sound) {
            builder.setSilent(true)
        }

        if (vibrate) {
            triggerVibration(status.isAlarm)
        }

        val notificationId = NOTIFICATION_ID_ALARM_BASE + abs(profile.id.hashCode() % 100000)
        notificationManager.notify(notificationId, builder.build())
    }

    fun notifyAlarmTransition(
        status: AlertStatus,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        val dummyProfile = Profile(
            id = status.regionKey,
            regionId = status.regionKey,
            regionName = status.regionName,
            districtId = status.districtKey,
            districtName = status.districtName,
            soundOnAlarm = soundEnabled,
            vibrateOnAlarm = vibrateEnabled,
            soundOnClear = soundEnabled,
            vibrateOnClear = vibrateEnabled
        )
        notifyAlarmTransition(dummyProfile, status)
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