package ua.alerts.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.alerts.mobile.R
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.ui.MainUiState
import androidx.compose.ui.text.style.TextAlign
import ua.alerts.mobile.ui.theme.DangerRed
import ua.alerts.mobile.ui.theme.DangerRedLight
import ua.alerts.mobile.ui.theme.SafeGreen
import ua.alerts.mobile.ui.theme.SafeGreenLight
import ua.alerts.mobile.ui.theme.WarningYellow
import ua.alerts.mobile.ui.theme.WarningYellowLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    soundAlarm: Boolean,
    vibrateAlarm: Boolean,
    soundClear: Boolean,
    vibrateClear: Boolean,
    serviceEnabled: Boolean,
    onOpenRegionPicker: () -> Unit,
    onRefresh: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onToggleSoundAlarm: (Boolean) -> Unit,
    onToggleVibrateAlarm: (Boolean) -> Unit,
    onToggleSoundClear: (Boolean) -> Unit,
    onToggleVibrateClear: (Boolean) -> Unit
) {
    val status = uiState.status
    val isAlarm = status.isAlarm

    val bannerBg = when {
        status.isYellow -> WarningYellowLight
        status.isRed -> DangerRedLight
        else -> SafeGreenLight
    }
    val primaryColor = when {
        status.isYellow -> WarningYellow
        status.isRed -> DangerRed
        else -> SafeGreen
    }
    val statusText = when {
        status.isYellow -> stringResource(R.string.status_alarm_yellow)
        status.isRed -> stringResource(R.string.status_alarm_red)
        else -> stringResource(R.string.status_clear)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ConnectionBadge(status = uiState.connectionStatus)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

            // Main Hero Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlarm) Icons.Default.Warning else Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = statusText,
                        color = primaryColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = status.displayName.ifEmpty { stringResource(R.string.region_not_selected) },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isAlarm && status.reasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        status.reasons.forEach { reason ->
                            Text(
                                text = reason,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = primaryColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (!status.since.isNullOrEmpty() && isAlarm) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.alarm_started, status.since ?: ""),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dedicated Monitoring Control Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.monitoring_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val statusText = if (!serviceEnabled) {
                            stringResource(R.string.status_monitoring_disabled)
                        } else {
                            when (uiState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected)
                                ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting)
                                ConnectionStatus.RECONNECTING -> stringResource(R.string.status_reconnecting)
                                ConnectionStatus.ERROR -> stringResource(R.string.status_connection_error)
                                ConnectionStatus.STOPPED -> stringResource(R.string.status_monitoring_disabled)
                            }
                        }
                        val statusColor = if (!serviceEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            when (uiState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> SafeGreen
                                ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFFFFA000)
                                ConnectionStatus.ERROR -> DangerRed
                                ConnectionStatus.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = onToggleService
                    )
                }
            }

            // Region Selection Card
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
                                text = status.displayName.ifEmpty { stringResource(R.string.region_not_selected) },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = onOpenRegionPicker,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Змінити")
                    }
                }
            }

            // Galaxy Watch Sync Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.watch_sync_status),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (uiState.connectedWatchCount > 0) {
                                "${stringResource(R.string.watch_connected)} (${uiState.connectedWatchCount})"
                            } else {
                                stringResource(R.string.watch_disconnected)
                            },
                            fontSize = 13.sp,
                            color = if (uiState.connectedWatchCount > 0) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(onClick = onRefresh) {
                        Text("Синхр.")
                    }
                }
            }

            // Settings Card
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

                    SettingToggle(
                        label = stringResource(R.string.service_enabled),
                        checked = serviceEnabled,
                        onCheckedChange = onToggleService
                    )

                    SettingToggle(
                        label = stringResource(R.string.sound_on_alarm),
                        checked = soundAlarm,
                        onCheckedChange = onToggleSoundAlarm
                    )

                    SettingToggle(
                        label = stringResource(R.string.vibrate_on_alarm),
                        checked = vibrateAlarm,
                        onCheckedChange = onToggleVibrateAlarm
                    )

                    SettingToggle(
                        label = stringResource(R.string.sound_on_clear),
                        checked = soundClear,
                        onCheckedChange = onToggleSoundClear
                    )

                    SettingToggle(
                        label = stringResource(R.string.vibrate_on_clear),
                        checked = vibrateClear,
                        onCheckedChange = onToggleVibrateClear
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ConnectionBadge(status: ConnectionStatus) {
    val (text, color) = when (status) {
        ConnectionStatus.CONNECTED -> "Live" to SafeGreen
        ConnectionStatus.CONNECTING -> "..." to Color(0xFFFFA000)
        ConnectionStatus.RECONNECTING -> "Sync..." to Color(0xFFFFA000)
        ConnectionStatus.ERROR -> "Error" to DangerRed
        ConnectionStatus.STOPPED -> "Off" to Color.Gray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
