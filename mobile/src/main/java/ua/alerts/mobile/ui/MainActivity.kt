package ua.alerts.mobile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.alerts.mobile.service.AlertForegroundService
import ua.alerts.mobile.ui.screens.HomeScreen
import ua.alerts.mobile.ui.screens.RegionSelectScreen
import ua.alerts.mobile.ui.theme.AlertsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission granted/denied handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        lifecycleScope.launch {
            val enabled = viewModel.settingsRepo.serviceEnabled.first()
            if (enabled) {
                AlertForegroundService.startService(this@MainActivity)
            }
        }

        setContent {
            AlertsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val soundAlarm by viewModel.soundAlarm.collectAsState()
                    val vibrateAlarm by viewModel.vibrateAlarm.collectAsState()
                    val soundClear by viewModel.soundClear.collectAsState()
                    val vibrateClear by viewModel.vibrateClear.collectAsState()
                    val serviceEnabled by viewModel.serviceEnabled.collectAsState()

                    if (uiState.isRegionPickerOpen) {
                        RegionSelectScreen(
                            currentStatus = uiState.status,
                            onSelectRegion = { regId, regName, distId, distName ->
                                viewModel.selectRegion(regId, regName, distId, distName)
                            },
                            onBack = { viewModel.closeRegionPicker() }
                        )
                    } else {
                        HomeScreen(
                            uiState = uiState,
                            soundAlarm = soundAlarm,
                            vibrateAlarm = vibrateAlarm,
                            soundClear = soundClear,
                            vibrateClear = vibrateClear,
                            serviceEnabled = serviceEnabled,
                            onOpenRegionPicker = { viewModel.openRegionPicker() },
                            onRefresh = { viewModel.refreshData() },
                            onToggleService = { viewModel.toggleService(it) },
                            onToggleSoundAlarm = { viewModel.setSoundAlarm(it) },
                            onToggleVibrateAlarm = { viewModel.setVibrateAlarm(it) },
                            onToggleSoundClear = { viewModel.setSoundClear(it) },
                            onToggleVibrateClear = { viewModel.setVibrateClear(it) }
                        )
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
