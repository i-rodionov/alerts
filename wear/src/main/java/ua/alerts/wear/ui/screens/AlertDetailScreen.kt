package ua.alerts.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import ua.alerts.shared.model.AlertStatus
import ua.alerts.wear.R
import ua.alerts.wear.ui.theme.WearDangerRed
import ua.alerts.wear.ui.theme.WearSafeGreen
import ua.alerts.wear.ui.theme.WearWarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertDetailScreen(
    status: AlertStatus,
    onSyncClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val isOffline = status.isStale()
    val isAlarm = status.isAlarm

    val statusColor = when {
        isOffline -> WearWarningYellow
        status.isYellow -> WearWarningYellow
        status.isRed -> WearDangerRed
        else -> WearSafeGreen
    }

    val statusTitle = when {
        isOffline -> stringResource(R.string.status_offline)
        status.isYellow -> stringResource(R.string.status_alarm_yellow)
        status.isRed -> stringResource(R.string.status_alarm_red)
        else -> stringResource(R.string.status_clear)
    }

    val iconRes = when {
        isOffline -> R.drawable.ic_offline_warning
        isAlarm -> R.drawable.ic_warning_siren
        else -> R.drawable.ic_shield_check
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val lastSyncText = if (status.updatedAt > 0) {
        timeFormat.format(Date(status.updatedAt))
    } else {
        "—"
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        item {
            Text(
                text = statusTitle,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = status.displayName.ifEmpty { stringResource(R.string.app_name) },
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }

        if (isAlarm && status.reasons.isNotEmpty()) {
            item {
                Text(
                    text = status.reasons.joinToString("\n"),
                    fontSize = 11.sp,
                    color = statusColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (isAlarm && !status.since.isNullOrEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.alert_duration, status.since ?: ""),
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.last_synced, lastSyncText),
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = MaterialTheme.colors.primaryVariant),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(36.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.sync_button),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
