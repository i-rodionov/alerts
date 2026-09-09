package ua.alerts.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class AlertApp : Application() {

    companion object {
        const val CHANNEL_SERVICE_ID = "alert_service_channel"
        const val CHANNEL_ALERTS_ID = "alert_alarm_channel"
        const val CHANNEL_CLEAR_ID = "alert_clear_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Service Channel (Low importance, silent)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                getString(R.string.service_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Відображає активний стан моніторингу тривог"
                setShowBadge(false)
            }

            // High priority Alert Channel (Notification Category Sound)
            val defaultNotificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                getString(R.string.alert_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про початок повітряної тривоги"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 800)
                setSound(defaultNotificationSound, audioAttributes)
            }

            // All clear Channel
            val clearSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val clearChannel = NotificationChannel(
                CHANNEL_CLEAR_ID,
                getString(R.string.status_clear),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Сповіщення про відбій повітряної тривоги"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(clearSound, null)
            }

            manager.createNotificationChannels(listOf(serviceChannel, alertChannel, clearChannel))
        }
    }
}
