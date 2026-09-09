package ua.alerts.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Notifications
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.alerts.mobile.R
import ua.alerts.mobile.ui.theme.DangerRed
import ua.alerts.shared.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileConfigScreen(
    profile: Profile?,
    isEditMode: Boolean,
    onUpdateProfile: ((Profile) -> Profile) -> Unit,
    onSaveNewProfile: () -> Unit,
    onDeleteProfile: (String) -> Unit,
    onOpenRegionPicker: () -> Unit,
    onPickAlertSound: () -> Unit = {},
    onPickClearSound: () -> Unit = {},
    onBack: () -> Unit
) {
    if (profile == null) return

    val locale = LocalConfiguration.current.locales[0]
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) stringResource(R.string.profile_edit_title) else stringResource(R.string.profile_new_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        TextButton(
                            onClick = onSaveNewProfile,
                            enabled = profile.regionId.isNotBlank()
                        ) {
                            Text(
                                text = stringResource(R.string.btn_save),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Region Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.choose_region),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = profile.getLocalizedDisplayName(locale.language).ifEmpty { stringResource(R.string.region_not_selected) },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = onOpenRegionPicker,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (profile.regionId.isEmpty()) stringResource(R.string.btn_select) else stringResource(R.string.btn_change))
                    }
                }
            }

            // Monitoring & Watch Integration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_monitoring_params),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    ProfileSettingToggle(
                        label = stringResource(R.string.service_enabled),
                        checked = profile.backgroundMonitoring,
                        onCheckedChange = { checked ->
                            onUpdateProfile { it.copy(backgroundMonitoring = checked) }
                        }
                    )

                    ProfileSettingToggle(
                        label = stringResource(R.string.profile_active_on_watch),
                        checked = profile.activeOnWatch,
                        onCheckedChange = { checked ->
                            onUpdateProfile { it.copy(activeOnWatch = checked) }
                        }
                    )
                }
            }

            // Notification Alerts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    ProfileSettingToggle(
                        label = stringResource(R.string.sound_on_alarm),
                        checked = profile.soundOnAlarm,
                        onCheckedChange = { checked ->
                            onUpdateProfile { it.copy(soundOnAlarm = checked) }
                        }
                    )

                    if (profile.soundOnAlarm) {
                        ProfileSoundSelector(
                            label = stringResource(R.string.notification_sound_alert),
                            soundUri = profile.alertSoundUri,
                            onClick = onPickAlertSound
                        )
                    }

                    ProfileSettingToggle(
                        label = stringResource(R.string.vibrate_on_alarm),
                        checked = profile.vibrateOnAlarm,
                        onCheckedChange = { checked ->
                            onUpdateProfile { it.copy(vibrateOnAlarm = checked) }
                        }
                    )

                    ProfileSettingToggle(
                        label = stringResource(R.string.sound_on_clear),
                        checked = profile.soundOnClear,
                        onCheckedChange = { checked ->
                            onUpdateProfile { it.copy(soundOnClear = checked) }
                        }
                    )

                    if (profile.soundOnClear) {
                        ProfileSoundSelector(
                            label = stringResource(R.string.notification_sound_clear),
                            soundUri = profile.clearSoundUri,
                            onClick = onPickClearSound
                        )
                    }

                    ProfileSettingToggle(
                        label = stringResource(R.string.vibrate_on_clear),
                        checked = profile.vibrateOnClear,
                        onCheckedChange = { checked ->
                            onUpdateProfile { it.copy(vibrateOnClear = checked) }
                        }
                    )
                }
            }

            // Delete Action (Only in Edit mode)
            if (isEditMode) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DangerRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_delete), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        val localizedName = profile.getLocalizedDisplayName(locale.language)
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = {
                Text(stringResource(R.string.profile_delete_message, localizedName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteProfile(profile.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) {
                    Text(stringResource(R.string.btn_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun ProfileSettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ProfileSoundSelector(
    label: String,
    soundUri: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val soundTitle = remember(soundUri) {
        resolveRingtoneTitle(context, soundUri)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = soundTitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun resolveRingtoneTitle(context: Context, uriString: String?): String {
    if (uriString.isNullOrBlank()) {
        return context.getString(R.string.sound_system_default)
    }
    return try {
        val uri = Uri.parse(uriString)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.getTitle(context) ?: context.getString(R.string.sound_system_default)
    } catch (_: Exception) {
        context.getString(R.string.sound_system_default)
    }
}
