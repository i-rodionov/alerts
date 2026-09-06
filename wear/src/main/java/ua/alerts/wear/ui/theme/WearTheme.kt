package ua.alerts.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

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
fun WearAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
