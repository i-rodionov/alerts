package ua.alerts.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import ua.alerts.shared.constants.WearConstants
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Profile
import ua.alerts.wear.R
import ua.alerts.wear.WearAlertApp
import ua.alerts.wear.ui.screens.AlertDetailScreen
import ua.alerts.wear.ui.theme.WearAlertTheme
import ua.alerts.wear.ui.theme.WearDangerRed
import ua.alerts.wear.ui.theme.WearSafeGreen
import ua.alerts.wear.ui.theme.WearWarningYellow

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialProfileId = intent.getStringExtra(WearConstants.EXTRA_PROFILE_ID)
        val repo = WearAlertApp.instance.alertRepository

        setContent {
            WearAlertTheme {
                val syncData by repo.syncData.collectAsState()
                var selectedProfileId by remember { mutableStateOf(initialProfileId) }

                if (selectedProfileId != null) {
                    val profile = syncData.profiles.find { it.id == selectedProfileId }
                    val status = syncData.statuses[selectedProfileId] ?: AlertStatus(
                        regionName = profile?.regionName ?: "",
                        districtName = profile?.districtName
                    )

                    BackHandler {
                        if (initialProfileId != null) {
                            finish()
                        } else {
                            selectedProfileId = null
                        }
                    }

                    AlertDetailScreen(
                        status = status,
                        onSyncClick = { repo.requestSyncFromPhone() }
                    )
                } else {
                    if (syncData.profiles.size == 1) {
                        val singleProfile = syncData.profiles.first()
                        val status = syncData.statuses[singleProfile.id] ?: AlertStatus(
                            regionName = singleProfile.regionName,
                            districtName = singleProfile.districtName
                        )
                        AlertDetailScreen(
                            status = status,
                            onSyncClick = { repo.requestSyncFromPhone() }
                        )
                    } else {
                        WearProfileListScreen(
                            profiles = syncData.profiles,
                            statuses = syncData.statuses,
                            onProfileClick = { profile ->
                                selectedProfileId = profile.id
                            },
                            onSyncClick = { repo.requestSyncFromPhone() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WearProfileListScreen(
    profiles: List<Profile>,
    statuses: Map<String, AlertStatus>,
    onProfileClick: (Profile) -> Unit,
    onSyncClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        if (profiles.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_synchronized_profiles),
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            item {
                Text(
                    text = stringResource(R.string.enable_sync_hint),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(profiles, key = { it.id }) { profile ->
                val status = statuses[profile.id]
                val isOffline = status == null || status.isStale()
                val isAlarm = status?.isAlarm == true

                val iconRes = when {
                    isOffline -> R.drawable.ic_offline_warning
                    isAlarm -> R.drawable.ic_warning_siren
                    else -> R.drawable.ic_shield_check
                }
                val tintColor = when {
                    isOffline -> WearWarningYellow
                    status?.isYellow == true -> WearWarningYellow
                    status?.isRed == true -> WearDangerRed
                    else -> WearSafeGreen
                }

                Chip(
                    onClick = { onProfileClick(profile) },
                    label = {
                        Text(
                            text = profile.displayName,
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    secondaryLabel = {
                        val labelText = when {
                            isOffline -> stringResource(R.string.status_offline)
                            status?.isAlarm == true -> stringResource(R.string.status_alarm)
                            else -> stringResource(R.string.status_clear)
                        }
                        Text(
                            text = labelText,
                            fontSize = 10.sp,
                            color = tintColor
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = MaterialTheme.colors.primaryVariant),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(34.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Text(
                    text = stringResource(R.string.sync_button),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
