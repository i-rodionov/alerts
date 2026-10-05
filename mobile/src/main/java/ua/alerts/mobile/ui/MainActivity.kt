package ua.alerts.mobile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import ua.alerts.mobile.R
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import android.content.res.Configuration
import java.util.Locale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.alerts.mobile.service.AlertForegroundService
import ua.alerts.mobile.util.BatteryOptimizationHelper
import ua.alerts.mobile.ui.screens.DashboardScreen
import ua.alerts.mobile.ui.screens.GlobalSettingsScreen
import ua.alerts.mobile.ui.screens.ProfileConfigScreen
import ua.alerts.mobile.ui.screens.RegionSelectScreen
import ua.alerts.mobile.ui.theme.AlertsTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission handled
    }

    enum class SoundTarget {
        ALERT,
        CLEAR
    }

    private var pendingSoundPickerTarget: SoundTarget? = null

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val uriString = pickedUri?.toString()
            when (pendingSoundPickerTarget) {
                SoundTarget.ALERT -> {
                    viewModel.updateDraft { it.copy(alertSoundUri = uriString) }
                }
                SoundTarget.CLEAR -> {
                    viewModel.updateDraft { it.copy(clearSoundUri = uriString) }
                }
                null -> {}
            }
        }
        pendingSoundPickerTarget = null
    }

    private fun launchSoundPicker(target: SoundTarget, currentUriString: String?) {
        pendingSoundPickerTarget = target
        val currentUri = if (!currentUriString.isNullOrBlank()) {
            try {
                currentUriString.toUri() } catch (_: Exception) { null }
        } else {
            null
        }
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val titleRes = if (target == SoundTarget.ALERT) R.string.notification_sound_alert else R.string.notification_sound_clear
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri ?: defaultUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(titleRes))
        }
        ringtonePickerLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryOptimizationStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        lifecycleScope.launch {
            val enabled = viewModel.settingsRepo.globalMonitoring.first()
            if (enabled) {
                AlertForegroundService.startService(this@MainActivity)
            }
        }

        setContent {
            val appLanguage by viewModel.appLanguage.collectAsState()
            val configuration = LocalConfiguration.current
            val context = LocalContext.current
            val locale = remember(appLanguage) {
                when (appLanguage) {
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
                                val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()

                                GlobalSettingsScreen(
                                    globalMonitoring = globalMonitoring,
                                    connectedWatchCount = connectedWatchCount,
                                    appLanguage = appLanguage,
                                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                                    onToggleGlobalMonitoring = { viewModel.setGlobalMonitoring(it) },
                                    onSelectLanguage = { viewModel.setAppLanguage(it) },
                                    onSyncWatch = { viewModel.refreshData() },
                                    onRequestDisableBatteryOptimization = {
                                        try {
                                            startActivity(
                                                BatteryOptimizationHelper.createRequestIgnoreBatteryOptimizationsIntent(this@MainActivity)
                                            )
                                        } catch (_: Exception) {
                                            // Ignore if activity cannot be launched
                                        }
                                    },
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
                                    onPickAlertSound = { launchSoundPicker(SoundTarget.ALERT, draftProfile?.alertSoundUri) },
                                    onPickClearSound = { launchSoundPicker(SoundTarget.CLEAR, draftProfile?.clearSoundUri) },
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
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}