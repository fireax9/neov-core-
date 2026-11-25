package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CompassCalibration
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LiveSensorReading
import com.example.data.model.SensorCategory
import com.example.data.model.SensorItem
import com.example.data.model.SensorReportingMode
import com.example.ui.components.KeyValueSpecRow
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.StatusDotPill
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale
import kotlin.math.abs

@Composable
fun SensorsScreen(
    sensors: List<SensorItem>,
    liveReadings: Map<Int, LiveSensorReading>,
    monitoredIds: Set<Int>,
    expandedSensorId: Int?,
    searchQuery: String,
    selectedCategory: SensorCategory,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (SensorCategory) -> Unit,
    onToggleExpand: (Int) -> Unit,
    onToggleMonitor: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current

    val filteredSensors = remember(sensors, searchQuery, selectedCategory) {
        sensors.filter { sensor ->
            val matchesCat = selectedCategory == SensorCategory.ALL || sensor.category == selectedCategory
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            val matchesQuery = q.isEmpty() ||
                sensor.friendlyTypeArabic.lowercase(Locale.getDefault()).contains(q) ||
                sensor.name.lowercase(Locale.getDefault()).contains(q) ||
                sensor.vendor.lowercase(Locale.getDefault()).contains(q)
            matchesCat && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusDotPill(
                    label = stringResource(R.string.sensors_total_count, sensors.size),
                    dotColor = MaterialTheme.colorScheme.primary
                )
                StatusDotPill(
                    label = stringResource(R.string.sensors_active_count, monitoredIds.size),
                    dotColor = telemetryColors.optimal
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sensors_search_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.sensors_search_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.action_close)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    SensorCategory.ALL to stringResource(R.string.sensors_filter_all),
                    SensorCategory.MOTION to stringResource(R.string.sensors_filter_motion),
                    SensorCategory.ENVIRONMENT to stringResource(R.string.sensors_filter_environment),
                    SensorCategory.POSITION to stringResource(R.string.sensors_filter_position),
                    SensorCategory.OTHER to stringResource(R.string.sensors_filter_other)
                )
                categories.forEach { (cat, label) ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("sensor_filter_${cat.name.lowercase(Locale.US)}")
                    )
                }
            }
        }

        if (filteredSensors.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.sensors_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.sensors_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredSensors, key = { it.id }) { sensor ->
                val isMonitored = sensor.id in monitoredIds
                val isExpanded = expandedSensorId == sensor.id
                val reading = liveReadings[sensor.id]

                SensorTelemetryCard(
                    sensor = sensor,
                    reading = reading,
                    isMonitored = isMonitored,
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleExpand(sensor.id) },
                    onToggleMonitor = { onToggleMonitor(sensor.id) }
                )
            }
        }
    }
}

@Composable
private fun SensorTelemetryCard(
    sensor: SensorItem,
    reading: LiveSensorReading?,
    isMonitored: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleMonitor: () -> Unit
) {
    val telemetryColors = LocalTelemetryColors.current
    val accent = if (isMonitored) telemetryColors.optimal else MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensor_card_${sensor.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (isExpanded) accent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(sensor.category),
                            contentDescription = sensor.friendlyTypeArabic,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sensor.friendlyTypeArabic,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isMonitored) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(telemetryColors.optimal)
                                )
                            }
                        }
                        LtrTelemetryText(
                            text = "${sensor.name} • ${sensor.vendor}",
                            style = TelemetryTextStyles.metricSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isMonitored && reading != null && !isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                val preview = reading.values.take(3).mapIndexed { i, v ->
                    val axis = sensor.valueLabels.getOrElse(i) { "V${i + 1}" }
                    String.format(Locale.US, "%s: %.2f", axis, v)
                }.joinToString("   ")
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    LtrTelemetryText(
                        text = if (sensor.unit.isNotBlank()) "$preview (${sensor.unit})" else preview,
                        style = TelemetryTextStyles.metricSmall,
                        color = telemetryColors.optimal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMonitored) {
                                stringResource(R.string.sensors_streaming_active)
                            } else {
                                stringResource(R.string.sensors_stream_action)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isMonitored) telemetryColors.optimal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = isMonitored,
                            onCheckedChange = { onToggleMonitor() },
                            modifier = Modifier.testTag("sensor_monitor_switch_${sensor.id}")
                        )
                    }

                    if (isMonitored) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.sensors_live_values),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (reading == null || reading.values.isEmpty()) {
                            Text(
                                text = stringResource(R.string.sensors_waiting_event),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxAbs = sensor.maxRange.takeIf { it > 0f } ?: 20f
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                reading.values.take(6).forEachIndexed { idx, value ->
                                    val axisName = sensor.valueLabels.getOrElse(idx) { "V${idx + 1}" }
                                    val normalized = (abs(value) / maxAbs).coerceIn(0f, 1f)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = axisName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            LtrTelemetryText(
                                                text = String.format(Locale.US, "%.3f %s", value, sensor.unit).trim(),
                                                style = TelemetryTextStyles.metricMedium.copy(fontWeight = FontWeight.Bold),
                                                color = telemetryColors.optimal
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { normalized },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = telemetryColors.optimal,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    KeyValueSpecRow(
                        label = stringResource(R.string.sensors_vendor_label),
                        value = sensor.vendor
                    )
                    KeyValueSpecRow(
                        label = stringResource(R.string.sensors_power_label),
                        value = String.format(Locale.US, "%.2f mA", sensor.powerMa)
                    )
                    KeyValueSpecRow(
                        label = stringResource(R.string.sensors_range_label),
                        value = String.format(Locale.US, "%.2f %s", sensor.maxRange, sensor.unit).trim()
                    )
                    KeyValueSpecRow(
                        label = stringResource(R.string.sensors_resolution_label),
                        value = String.format(Locale.US, "%.4f", sensor.resolution)
                    )
                    KeyValueSpecRow(
                        label = stringResource(R.string.sensors_delay_label),
                        value = "${sensor.minDelayUs} μs"
                    )
                    KeyValueSpecRow(
                        label = stringResource(R.string.sensors_mode_label),
                        value = when (sensor.reportingMode) {
                            SensorReportingMode.CONTINUOUS -> "Continuous"
                            SensorReportingMode.ON_CHANGE -> "On-Change"
                            SensorReportingMode.ONE_SHOT -> "One-Shot"
                            SensorReportingMode.SPECIAL_TRIGGER -> "Special"
                        },
                        showDivider = false
                    )
                }
            }
        }
    }
}

private fun categoryIcon(category: SensorCategory): ImageVector {
    return when (category) {
        SensorCategory.MOTION -> Icons.Outlined.CompassCalibration
        SensorCategory.ENVIRONMENT -> Icons.Outlined.LightMode
        SensorCategory.POSITION -> Icons.Outlined.Explore
        else -> Icons.Outlined.Sensors
    }
}
