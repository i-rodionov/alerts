package ua.alerts.mobile.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
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
        connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
        language: String = java.util.Locale.getDefault().language
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

        val regionPrefix = if (hasStatus) "${status!!.getLocalizedDisplayName(language)} • " else ""
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

    fun resolveSoundUri(uriString: String?): Uri {
        if (uriString.isNullOrBlank()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Settings.System.DEFAULT_NOTIFICATION_URI
        }
        return try {
            val uri = Uri.parse(uriString)
            if (uri != null && uri.scheme != null) {
                uri
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: Settings.System.DEFAULT_NOTIFICATION_URI
            }
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Settings.System.DEFAULT_NOTIFICATION_URI
        }
    }

    private fun soundHash(uriString: String?): String {
        return (uriString ?: "default").hashCode().toString()
    }

    fun getOrCreateProfileChannel(
        profile: Profile,
        isAlarm: Boolean,
        language: String = java.util.Locale.getDefault().language
    ): String {
        val soundUriString = if (isAlarm) profile.alertSoundUri else profile.clearSoundUri
        val hash = soundHash(soundUriString)
        val prefix = if (isAlarm) "alerts" else "clear"
        val channelId = "${prefix}_${profile.id}_$hash"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(channelId)
            if (existing != null) {
                return channelId
            }

            // Cleanup older channels for this profile with a different sound hash
            val targetPrefix = "${prefix}_${profile.id}_"
            try {
                notificationManager.notificationChannels.forEach { ch ->
                    if (ch.id.startsWith(targetPrefix) && ch.id != channelId) {
                        notificationManager.deleteNotificationChannel(ch.id)
                    }
                }
            } catch (_: Exception) {}

            val importance = if (isAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channelNamePrefix = if (isAlarm) {
                context.getString(R.string.alert_notification_channel)
            } else {
                context.getString(R.string.status_clear)
            }
            val displayName = profile.getLocalizedDisplayName(language)
            val channelName = "$channelNamePrefix: $displayName"
            val soundUri = resolveSoundUri(soundUriString)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = if (isAlarm) {
                    context.getString(R.string.notification_channel_alert_desc, displayName)
                } else {
                    context.getString(R.string.notification_channel_clear_desc, displayName)
                }
                setSound(soundUri, audioAttributes)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
        return channelId
    }

    fun deleteProfileChannels(profileId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertPrefix = "alerts_${profileId}_"
            val clearPrefix = "clear_${profileId}_"
            try {
                notificationManager.notificationChannels.forEach { ch ->
                    if (ch.id.startsWith(alertPrefix) || ch.id.startsWith(clearPrefix)) {
                        notificationManager.deleteNotificationChannel(ch.id)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun notifyAlarmTransition(
        profile: Profile,
        status: AlertStatus,
        language: String = java.util.Locale.getDefault().language
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

        val channelId = getOrCreateProfileChannel(profile, status.isAlarm, language)
        val title = if (status.isAlarm) {
            if (status.isYellow) {
                "🟡 " + context.getString(R.string.status_alarm_yellow)
            } else {
                "🔴 " + context.getString(R.string.status_alarm_red)
            }
        } else {
            "🟢 " + context.getString(R.string.status_clear)
        }

        val displayName = profile.getLocalizedDisplayName(language)
        val reasonSuffix = if (status.isAlarm && status.reasons.isNotEmpty()) " (${status.reasons.joinToString(", ")})" else ""
        val message = if (status.isAlarm) {
            context.getString(R.string.notification_alert_started, displayName, reasonSuffix)
        } else {
            context.getString(R.string.notification_alert_cleared, displayName)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (status.isAlarm) R.drawable.ic_warning_siren else R.drawable.ic_shield_check)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (status.isAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

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