package ua.alerts.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import ua.alerts.shared.util.EventTimeFormatter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.alerts.mobile.R
import ua.alerts.mobile.data.ConnectionStatus
import ua.alerts.mobile.ui.theme.DangerRed
import ua.alerts.mobile.ui.theme.DangerRedLight
import ua.alerts.mobile.ui.theme.SafeGreen
import ua.alerts.mobile.ui.theme.SafeGreenLight
import ua.alerts.mobile.ui.theme.WarningYellow
import ua.alerts.mobile.ui.theme.WarningYellowLight
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profiles: List<Profile>,
    profileAlerts: Map<String, AlertStatus>,
    connectionStatus: ConnectionStatus,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onProfileClick: (Profile) -> Unit,
    onAddProfile: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

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
                        ConnectionBadge(status = connectionStatus)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
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
        ) {
            if (profiles.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_profiles_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_profiles_desc),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onAddProfile,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_profile_button))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        val alertStatus = profileAlerts[profile.id] ?: AlertStatus()
                        ProfileCard(
                            profile = profile,
                            alertStatus = alertStatus,
                            onClick = { onProfileClick(profile) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedCard(
                            onClick = onAddProfile,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.add_profile_button),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Footer — NEPTUN attribution & disclaimer
            // <a href="https://neptun.in.ua/">Дані: Карта повітряних тривог — NEPTUN</a>
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.data_source_attribution),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://neptun.in.ua/")
                    }
                )
                Text(
                    text = stringResource(R.string.neptun_disclaimer_short),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: Profile,
    alertStatus: AlertStatus,
    onClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val isAlarm = alertStatus.isAlarm
    val cardBg = when {
        alertStatus.isYellow -> WarningYellowLight
        alertStatus.isRed -> DangerRedLight
        else -> SafeGreenLight
    }
    val statusColor = when {
        alertStatus.isYellow -> WarningYellow
        alertStatus.isRed -> DangerRed
        else -> SafeGreen
    }
    val statusText = when {
        alertStatus.isYellow -> stringResource(R.string.status_alarm_yellow)
        alertStatus.isRed -> stringResource(R.string.status_alarm_red)
        else -> stringResource(R.string.status_clear)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alert state icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAlarm) Icons.Default.Warning else Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.getLocalizedDisplayName(locale.language).ifEmpty { stringResource(R.string.region_not_selected) },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1C1B1F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Discoverable icons / badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Active on Watch icon
                    if (profile.activeOnWatch) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = stringResource(R.string.action_active_on_watch),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Background monitoring indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (profile.backgroundMonitoring) SafeGreen.copy(alpha = 0.15f)
                                else Color.Gray.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (profile.backgroundMonitoring) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = if (profile.backgroundMonitoring) stringResource(R.string.monitoring_active) else stringResource(R.string.status_monitoring_disabled),
                            tint = if (profile.backgroundMonitoring) SafeGreen else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isAlarm && alertStatus.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alertStatus.reasons.joinToString(", "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            if (isAlarm && !alertStatus.since.isNullOrEmpty()) {
                val formattedTime = EventTimeFormatter.formatLocalEventTime(alertStatus.since, locale)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.alarm_started, formattedTime),
                    fontSize = 12.sp,
                    color = Color(0xFF49454F)
                )
            }
        }
    }
}

@Composable
fun ConnectionBadge(status: ConnectionStatus) {
    val text = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.badge_live)
        ConnectionStatus.CONNECTING -> stringResource(R.string.badge_connecting)
        ConnectionStatus.RECONNECTING -> stringResource(R.string.badge_syncing)
        ConnectionStatus.ERROR -> stringResource(R.string.badge_error)
        ConnectionStatus.STOPPED -> stringResource(R.string.badge_off)
    }
    val color = when (status) {
        ConnectionStatus.CONNECTED -> SafeGreen
        ConnectionStatus.CONNECTING -> Color(0xFFFFA000)
        ConnectionStatus.RECONNECTING -> Color(0xFFFFA000)
        ConnectionStatus.ERROR -> DangerRed
        ConnectionStatus.STOPPED -> Color.Gray
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
