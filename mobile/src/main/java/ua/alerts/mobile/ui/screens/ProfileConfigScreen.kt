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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
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
    onBack: () -> Unit
) {
    if (profile == null) return

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Налаштування профілю" else "Новий профіль",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
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
                                text = "Зберегти",
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
                                text = profile.displayName.ifEmpty { stringResource(R.string.region_not_selected) },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = onOpenRegionPicker,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (profile.regionId.isEmpty()) "Обрати" else "Змінити")
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
                        text = "Параметри моніторингу",
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
                        label = "Активний на Galaxy Watch",
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
                    Text("Видалити профіль", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Видалити профіль?") },
            text = {
                Text("Ви дійсно бажаєте видалити профіль \"${profile.displayName}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteProfile(profile.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) {
                    Text("Видалити", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Скасувати")
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
