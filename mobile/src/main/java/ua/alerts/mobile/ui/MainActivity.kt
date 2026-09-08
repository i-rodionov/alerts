package ua.alerts.mobile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import ua.alerts.mobile.ui.screens.DashboardScreen
import ua.alerts.mobile.ui.screens.GlobalSettingsScreen
import ua.alerts.mobile.ui.screens.ProfileConfigScreen
import ua.alerts.mobile.ui.screens.RegionSelectScreen
import ua.alerts.mobile.ui.theme.AlertsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        lifecycleScope.launch {
            val enabled = viewModel.settingsRepo.globalMonitoring.first()
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
                    val currentScreen by viewModel.currentScreen.collectAsState()

                    BackHandler(enabled = currentScreen != Screen.Dashboard) {
                        viewModel.navigateBack()
                    }

                    when (val screen = currentScreen) {
                        is Screen.Dashboard -> {
                            val profiles by viewModel.profiles.collectAsState()
                            val profileAlerts by viewModel.profileAlerts.collectAsState()
                            val connectionStatus by viewModel.connectionStatus.collectAsState()

                            DashboardScreen(
                                profiles = profiles,
                                profileAlerts = profileAlerts,
                                connectionStatus = connectionStatus,
                                onRefresh = { viewModel.refreshData() },
                                onOpenSettings = { viewModel.navigateTo(Screen.GlobalSettings) },
                                onProfileClick = { profile ->
                                    viewModel.initDraftProfile(profile.id)
                                    viewModel.navigateTo(Screen.ProfileConfig(profile.id))
                                },
                                onAddProfile = {
                                    viewModel.initDraftProfile(null)
                                    viewModel.navigateTo(Screen.ProfileConfig(null))
                                }
                            )
                        }

                        is Screen.GlobalSettings -> {
                            val globalMonitoring by viewModel.globalMonitoring.collectAsState()
                            val connectedWatchCount by viewModel.connectedWatchCount.collectAsState()

                            GlobalSettingsScreen(
                                globalMonitoring = globalMonitoring,
                                connectedWatchCount = connectedWatchCount,
                                onToggleGlobalMonitoring = { viewModel.setGlobalMonitoring(it) },
                                onSyncWatch = { viewModel.refreshData() },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.ProfileConfig -> {
                            val draftProfile by viewModel.draftProfile.collectAsState()

                            ProfileConfigScreen(
                                profile = draftProfile,
                                isEditMode = screen.profileId != null,
                                onUpdateProfile = { updater -> viewModel.updateDraft(updater) },
                                onSaveNewProfile = { viewModel.saveDraftProfile() },
                                onDeleteProfile = { id -> viewModel.deleteProfile(id) },
                                onOpenRegionPicker = { viewModel.navigateTo(Screen.RegionPicker(screen.profileId)) },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.RegionPicker -> {
                            val draftProfile by viewModel.draftProfile.collectAsState()

                            RegionSelectScreen(
                                selectedRegionKey = draftProfile?.regionId,
                                selectedDistrictKey = draftProfile?.districtId,
                                onSelectRegion = { regId, regName, distId, distName ->
                                    viewModel.setDraftRegion(regId, regName, distId, distName)
                                    viewModel.navigateBack()
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }
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