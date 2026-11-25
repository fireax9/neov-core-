package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MemoryInfo
import com.example.ui.components.KeyValueSpecRow
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.RadialTelemetryGauge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusDotPill
import com.example.ui.components.TelemetryCard
import com.example.ui.components.TelemetryFormatter
import com.example.ui.components.TelemetrySparkline
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale

@Composable
fun MemoryScreen(
    memoryInfo: MemoryInfo?,
    ramHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current

    if (memoryInfo == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.memory_header_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val ramColor = when {
        memoryInfo.isLowMemory || memoryInfo.ramUsagePercent >= 85f -> telemetryColors.critical
        memoryInfo.ramUsagePercent >= 68f -> telemetryColors.warm
        else -> MaterialTheme.colorScheme.primary
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, ramColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.memory_ram_section),
                            icon = Icons.Outlined.Dns,
                            trailing = {
                                StatusDotPill(
                                    label = if (memoryInfo.isLowMemory) {
                                        stringResource(R.string.memory_ram_status_low)
                                    } else {
                                        stringResource(R.string.memory_ram_status_normal)
                                    },
                                    dotColor = if (memoryInfo.isLowMemory) {
                                        telemetryColors.critical
                                    } else {
                                        telemetryColors.optimal
                                    }
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        RadialTelemetryGauge(
                            progress = memoryInfo.ramUsagePercent / 100f,
                            centerValue = String.format(Locale.US, "%.0f%%", memoryInfo.ramUsagePercent),
                            centerLabel = "RAM",
                            accentColor = ramColor
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RamBreakdownItem(
                                label = stringResource(R.string.memory_ram_used),
                                value = TelemetryFormatter.formatBytes(memoryInfo.usedRamBytes),
                                color = ramColor
                            )
                            RamBreakdownItem(
                                label = stringResource(R.string.memory_ram_available),
                                value = TelemetryFormatter.formatBytes(memoryInfo.availableRamBytes),
                                color = telemetryColors.optimal
                            )
                            RamBreakdownItem(
                                label = stringResource(R.string.memory_ram_total),
                                value = TelemetryFormatter.formatBytes(memoryInfo.totalRamBytes),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    KeyValueSpecRow(
                        label = stringResource(R.string.memory_ram_threshold),
                        value = TelemetryFormatter.formatBytes(memoryInfo.lowMemoryThresholdBytes),
                        showDivider = memoryInfo.cachedBytes != null || memoryInfo.swapTotalBytes != null
                    )
                    if (memoryInfo.cachedBytes != null) {
                        KeyValueSpecRow(
                            label = stringResource(R.string.memory_cached_label),
                            value = TelemetryFormatter.formatBytes(memoryInfo.cachedBytes),
                            showDivider = memoryInfo.swapTotalBytes != null
                        )
                    }
                    if (memoryInfo.swapTotalBytes != null && memoryInfo.swapTotalBytes > 0L) {
                        val swapUsed = (memoryInfo.swapTotalBytes - (memoryInfo.swapFreeBytes ?: 0L))
                            .coerceAtLeast(0L)
                        KeyValueSpecRow(
                            label = stringResource(R.string.memory_swap_label),
                            value = "${TelemetryFormatter.formatBytes(swapUsed)} / ${TelemetryFormatter.formatBytes(memoryInfo.swapTotalBytes)}",
                            showDivider = false
                        )
                    }
                }
            }
        }

        if (ramHistory.isNotEmpty()) {
            item {
                TelemetryCard {
                    SectionHeader(
                        title = stringResource(R.string.memory_ram_history_title),
                        icon = Icons.Outlined.Timeline,
                        trailing = {
                            LtrTelemetryText(
                                text = String.format(Locale.US, "%.1f%%", memoryInfo.ramUsagePercent),
                                style = TelemetryTextStyles.metricMedium,
                                color = ramColor
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TelemetrySparkline(
                        values = ramHistory,
                        lineColor = ramColor,
                        minFloor = 0f,
                        maxCeil = 100f
                    )
                }
            }
        }

        item {
            TelemetryCard {
                SectionHeader(
                    title = stringResource(R.string.memory_storage_section),
                    icon = Icons.Outlined.SdStorage
                )
                Spacer(modifier = Modifier.height(12.dp))

                StoragePartitionBar(
                    title = stringResource(R.string.memory_storage_data_title),
                    usedBytes = memoryInfo.dataUsedBytes,
                    freeBytes = memoryInfo.dataFreeBytes,
                    totalBytes = memoryInfo.dataTotalBytes,
                    accentColor = MaterialTheme.colorScheme.primary
                )

                if (memoryInfo.systemTotalBytes > 0L) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StoragePartitionBar(
                        title = stringResource(R.string.memory_storage_system_title),
                        usedBytes = memoryInfo.systemUsedBytes,
                        freeBytes = memoryInfo.systemFreeBytes,
                        totalBytes = memoryInfo.systemTotalBytes,
                        accentColor = telemetryColors.optimal
                    )
                }
            }
        }
    }
}

@Composable
private fun RamBreakdownItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LtrTelemetryText(
            text = value,
            style = TelemetryTextStyles.metricMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun StoragePartitionBar(
    title: String,
    usedBytes: Long,
    freeBytes: Long,
    totalBytes: Long,
    accentColor: Color
) {
    val ratio = if (totalBytes > 0L) {
        (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "storage_bar"
    )
    val telemetryColors = LocalTelemetryColors.current
    val barColor = if (ratio >= 0.88f) telemetryColors.critical else accentColor

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LtrTelemetryText(
                    text = String.format(Locale.US, "%.1f%%", ratio * 100f),
                    style = TelemetryTextStyles.metricMedium.copy(fontWeight = FontWeight.Bold),
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageStatMini(
                    label = stringResource(R.string.memory_storage_used),
                    value = TelemetryFormatter.formatBytes(usedBytes)
                )
                StorageStatMini(
                    label = stringResource(R.string.memory_storage_free),
                    value = TelemetryFormatter.formatBytes(freeBytes)
                )
                StorageStatMini(
                    label = stringResource(R.string.memory_storage_total),
                    value = TelemetryFormatter.formatBytes(totalBytes)
                )
            }
        }
    }
}

@Composable
private fun StorageStatMini(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        LtrTelemetryText(
            text = value,
            style = TelemetryTextStyles.metricSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
