package ua.alerts.wear.complication

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import ua.alerts.shared.model.Profile
import ua.alerts.wear.R
import ua.alerts.wear.WearAlertApp
import ua.alerts.wear.ui.theme.WearAlertTheme

class ComplicationConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val complicationId = intent.getIntExtra(
            ComplicationDataSourceService.EXTRA_CONFIG_COMPLICATION_ID,
            intent.getIntExtra("android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID", -1)
        )

        val repo = WearAlertApp.instance.alertRepository

        setContent {
            WearAlertTheme {
                val syncData by repo.syncData.collectAsState()
                val selectedProfileId = if (complicationId != -1) {
                    repo.getComplicationProfileId(complicationId)
                } else {
                    null
                }

                ComplicationConfigScreen(
                    profiles = syncData.profiles,
                    selectedProfileId = selectedProfileId,
                    onProfileSelected = { profile ->
                        if (complicationId != -1) {
                            repo.setComplicationProfileId(complicationId, profile.id)
                            val requester = ComplicationDataSourceUpdateRequester.create(
                                this@ComplicationConfigActivity,
                                ComponentName(this@ComplicationConfigActivity, AlertComplicationService::class.java)
                            )
                            requester.requestUpdate(complicationId)
                        }
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ComplicationConfigScreen(
    profiles: List<Profile>,
    selectedProfileId: String?,
    onProfileSelected: (Profile) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
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
                text = stringResource(R.string.select_region),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        if (profiles.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_synchronized_profiles),
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                val isSelected = profile.id == selectedProfileId
                ToggleChip(
                    checked = isSelected,
                    onCheckedChange = { onProfileSelected(profile) },
                    label = {
                        Text(
                            text = profile.getLocalizedDisplayName(locale.language),
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    },
                    secondaryLabel = if (!profile.districtName.isNullOrEmpty() && profile.regionName.isNotEmpty()) {
                        { Text(text = profile.getLocalizedRegionName(locale.language), fontSize = 9.sp, color = Color.Gray) }
                    } else null,
                    toggleControl = {
                        RadioButton(selected = isSelected)
                    },
                    colors = ToggleChipDefaults.toggleChipColors(),
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }
        }
    }
}
