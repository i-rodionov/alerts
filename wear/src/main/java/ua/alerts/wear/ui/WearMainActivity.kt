package ua.alerts.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ua.alerts.wear.WearAlertApp
import ua.alerts.wear.ui.screens.AlertDetailScreen
import ua.alerts.wear.ui.theme.WearAlertTheme

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = WearAlertApp.instance.alertRepository

        setContent {
            WearAlertTheme {
                val currentStatus by repo.currentStatus.collectAsState()

                AlertDetailScreen(
                    status = currentStatus,
                    onSyncClick = { repo.requestSyncFromPhone() }
                )
            }
        }
    }
}
