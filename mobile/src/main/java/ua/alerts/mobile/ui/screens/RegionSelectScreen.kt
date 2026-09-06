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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
    currentStatus: AlertStatus,
    onSelectRegion: (regionId: String, regionName: String, districtId: String?, districtName: String?) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedRegionId by remember { mutableStateOf<String?>(currentStatus.regionKey) }

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
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
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
                            Icon(Icons.Default.Clear, contentDescription = "Очистити")
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
                        currentStatus = currentStatus,
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
fun RegionItem(
    region: Region,
    currentStatus: AlertStatus,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: (regionId: String, regionName: String, districtId: String?, districtName: String?) -> Unit
) {
    val isRegionSelected = currentStatus.regionKey == region.id && currentStatus.districtKey == null

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
                            onSelect(region.id, region.nameUk, null, null)
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
                        text = region.nameUk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (region.raions.isNotEmpty()) {
                        Text(
                            text = "${region.raions.size} районів",
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
                        contentDescription = "Обрано",
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
                    Divider()

                    // Option: Entire oblast
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(region.id, region.nameUk, null, null) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "?? " + stringResource(R.string.all_oblast),
                            fontWeight = if (isRegionSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRegionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isRegionSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Raions
                    region.raions.forEach { district ->
                        val isDistrictSelected = currentStatus.districtKey == district.id
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(region.id, region.nameUk, district.id, district.nameUk) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = district.nameUk,
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
