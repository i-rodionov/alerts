package ua.alerts.wear.ui.theme

import android.content.res.Configuration
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import ua.alerts.wear.WearAlertApp

val WearSafeGreen = Color(0xFF4CAF50)
val WearDangerRed = Color(0xFFF44336)
val WearWarningYellow = Color(0xFFFFC107)

private val WearColorPalette = Colors(
    primary = Color(0xFF64B5F6),
    primaryVariant = Color(0xFF1976D2),
    secondary = Color(0xFF81C784),
    secondaryVariant = Color(0xFF388E3C),
    error = WearDangerRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.White
)

@Composable
fun WearAlertTheme(
    language: String? = null,
    content: @Composable () -> Unit
) {
    val repo = WearAlertApp.instanceOrNull?.alertRepository
    val repoLang = repo?.syncData?.collectAsState()?.value?.language ?: "system"
    val effectiveLanguage = language ?: repoLang
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val locale = remember(effectiveLanguage) {
        when (effectiveLanguage) {
            "uk" -> Locale.forLanguageTag("uk")
            "en" -> Locale.forLanguageTag("en")
            else -> Locale.getDefault()
        }
    }
    val localizedConfig = remember(configuration, locale) {
        Configuration(configuration).apply {
            setLocale(locale)
        }
    }
    val localizedContext = remember(context, localizedConfig) {
        context.createConfigurationContext(localizedConfig)
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext
    ) {
        MaterialTheme(
            colors = WearColorPalette,
            content = content
        )
    }
}
