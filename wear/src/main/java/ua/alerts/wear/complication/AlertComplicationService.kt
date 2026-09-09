package ua.alerts.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import ua.alerts.shared.constants.WearConstants
import ua.alerts.wear.R
import ua.alerts.wear.WearAlertApp
import ua.alerts.wear.ui.WearMainActivity

class AlertComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val iconRes = R.drawable.ic_warning_siren
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                val text = PlainComplicationText.Builder(getString(R.string.complication_preview_text)).build()
                val title = PlainComplicationText.Builder(getString(R.string.complication_preview_title)).build()
                ShortTextComplicationData.Builder(text, text)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                val text = PlainComplicationText.Builder(getString(R.string.status_alarm)).build()
                val title = PlainComplicationText.Builder(getString(R.string.complication_preview_title)).build()
                LongTextComplicationData.Builder(text, text)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val repo = try {
            WearAlertApp.instance.alertRepository
        } catch (_: Exception) {
            null
        }

        val configuredProfileId = repo?.getComplicationProfileId(request.complicationInstanceId)
        val profile = repo?.getProfile(configuredProfileId)
        val status = if (profile != null) repo.getStatus(configuredProfileId) else null

        val tapIntent = Intent(this, WearMainActivity::class.java).apply {
            if (!configuredProfileId.isNullOrEmpty()) {
                putExtra(WearConstants.EXTRA_PROFILE_ID, configuredProfileId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            request.complicationInstanceId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Offline if unconfigured, or profile removed/unsynchronized, or status missing/stale
        val isOffline = configuredProfileId == null || profile == null || status == null || status.isStale()
        val isAlarm = !isOffline && status?.isAlarm == true
        val regionName = profile?.displayName?.ifEmpty { getString(R.string.app_name) }
            ?: status?.displayName?.ifEmpty { getString(R.string.app_name) }
            ?: getString(R.string.app_name)

        val iconRes = when {
            isOffline -> R.drawable.ic_offline_warning
            isAlarm -> R.drawable.ic_warning_siren
            else -> R.drawable.ic_shield_check
        }
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val textStr = when {
                    isOffline -> "—"
                    status?.isYellow == true -> "🟡"
                    status?.isRed == true -> "🔴"
                    else -> "🟢"
                }
                val titleStr = when {
                    isOffline -> "⚠️"
                    status?.isYellow == true -> "ЖОВ"
                    status?.isRed == true -> "ТРВ"
                    else -> "ОК"
                }
                val text = PlainComplicationText.Builder(textStr).build()
                val title = PlainComplicationText.Builder(titleStr).build()

                ShortTextComplicationData.Builder(text, text)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .setTapAction(pendingIntent)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                val headerStr = when {
                    isOffline -> getString(R.string.status_offline)
                    status?.isYellow == true -> "🟡 " + (status.reasons.firstOrNull() ?: getString(R.string.status_alarm_yellow))
                    status?.isRed == true -> "🔴 " + (status.reasons.firstOrNull() ?: getString(R.string.status_alarm_red))
                    else -> "🟢 " + getString(R.string.status_clear)
                }
                val text = PlainComplicationText.Builder(headerStr).build()
                val title = PlainComplicationText.Builder(regionName).build()

                LongTextComplicationData.Builder(text, text)
                    .setTitle(title)
                    .setMonochromaticImage(icon)
                    .setTapAction(pendingIntent)
                    .build()
            }
            else -> {
                // Fallback for empty/unsupported types
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("—").build(),
                    PlainComplicationText.Builder("—").build()
                ).setTapAction(pendingIntent).build()
            }
        }
    }
}
