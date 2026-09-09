package ua.alerts.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import ua.alerts.shared.data.DefaultRegions
import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.Region

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectScreen(
    selectedRegionKey: String? = null,
    selectedDistrictKey: String? = null,
    onSelectRegion: (regionId: String, regionName: String, districtId: String?, districtName: String?) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedRegionId by remember { mutableStateOf<String?>(selectedRegionKey) }

    val filteredRegions = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            DefaultRegions.ALL_REGIONS
        } else {
            val q = searchQuery.trim().lowercase()
            DefaultRegions.ALL_REGIONS.filter { region ->
                region.nameUk.lowercase().contains(q) ||
                region.nameEn.lowercase().contains(q) ||
                region.raions.any { it.nameUk.lowercase().contains(q) || it.nameEn.lowercase().contains(q) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_region), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
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
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.btn_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredRegions, key = { it.id }) { region ->
                    RegionItem(
                        region = region,
                        selectedRegionKey = selectedRegionKey,
                        selectedDistrictKey = selectedDistrictKey,
                        isExpanded = expandedRegionId == region.id || searchQuery.isNotBlank(),
                        onToggleExpand = {
                            expandedRegionId = if (expandedRegionId == region.id) null else region.id
                        },
                        onSelect = onSelectRegion
                    )
                }
            }
        }
    }
}

@Composable
fun RegionSelectScreen(
    currentStatus: AlertStatus,
    onSelectRegion: (regionId: String, regionName: String, districtId: String?, districtName: String?) -> Unit,
    onBack: () -> Unit
) {
    RegionSelectScreen(
        selectedRegionKey = currentStatus.regionKey,
        selectedDistrictKey = currentStatus.districtKey,
        onSelectRegion = onSelectRegion,
        onBack = onBack
    )
}

@Composable
fun RegionItem(
    region: Region,
    selectedRegionKey: String?,
    selectedDistrictKey: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: (regionId: String, regionName: String, districtId: String?, districtName: String?) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val regionName = region.getLocalizedName(locale.language)
    val isRegionSelected = selectedRegionKey == region.id && selectedDistrictKey == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRegionSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (region.raions.isEmpty()) {
                            onSelect(region.id, regionName, null, null)
                        } else {
                            onToggleExpand()
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = regionName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (region.raions.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.raions_count, region.raions.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (region.raions.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isRegionSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.item_selected),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && region.raions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    HorizontalDivider()

                    // Option: Entire oblast
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(region.id, regionName, null, null) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⌖ " + stringResource(R.string.all_oblast),
                            fontWeight = if (isRegionSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRegionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isRegionSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Raions
                    region.raions.forEach { district ->
                        val districtName = district.getLocalizedName(locale.language)
                        val isDistrictSelected = selectedDistrictKey == district.id
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(region.id, regionName, district.id, districtName) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = districtName,
                                fontWeight = if (isDistrictSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDistrictSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isDistrictSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
