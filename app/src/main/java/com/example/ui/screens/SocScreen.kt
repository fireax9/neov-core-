package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CpuCoreInfo
import com.example.data.model.CpuInfo
import com.example.ui.components.KeyValueSpecRow
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryCard
import com.example.ui.components.TelemetryFormatter
import com.example.ui.components.TelemetryHeroCard
import com.example.ui.components.TelemetrySparkline
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocScreen(
    cpuInfo: CpuInfo?,
    clockHistoryMhz: List<Float>,
    loadHistory: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current

    if (cpuInfo == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.soc_header_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val hasValidClock = (cpuInfo.averageClockKhz ?: 0L) >= 10_000L ||
        (cpuInfo.peakClockKhz ?: 0L) >= 10_000L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val (heroValue, heroLabel) = when {
                hasValidClock && cpuInfo.averageClockKhz != null -> {
                    TelemetryFormatter.formatFreqKhz(cpuInfo.averageClockKhz) to
                        stringResource(R.string.soc_avg_clock)
                }
                cpuInfo.cpuLoadPercent != null -> {
                    String.format(Locale.US, "%.1f%%", cpuInfo.cpuLoadPercent) to
                        stringResource(R.string.soc_cpu_load)
                }
                hasValidClock && cpuInfo.peakClockKhz != null -> {
                    TelemetryFormatter.formatFreqKhz(cpuInfo.peakClockKhz) to
                        stringResource(R.string.soc_peak_clock)
                }
                else -> {
                    "${cpuInfo.onlineCores} / ${cpuInfo.totalCores} Cores" to
                        stringResource(R.string.soc_cores_label)
                }
            }

            TelemetryHeroCard(
                title = cpuInfo.socModel,
                subtitle = "${cpuInfo.socManufacturer} • ${cpuInfo.architecture}",
                heroValue = heroValue,
                heroLabel = heroLabel,
                icon = Icons.Outlined.Memory,
                accentColor = MaterialTheme.colorScheme.primary,
                badgeText = if (cpuInfo.is64Bit) {
                    stringResource(R.string.soc_arch_64)
                } else {
                    stringResource(R.string.soc_arch_32)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatChip(
                        label = stringResource(R.string.soc_cores_label),
                        value = "${cpuInfo.onlineCores}/${cpuInfo.totalCores}",
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatChip(
                        label = if (cpuInfo.cpuLoadPercent != null && hasValidClock) {
                            stringResource(R.string.soc_cpu_load)
                        } else if (hasValidClock) {
                            stringResource(R.string.soc_peak_clock)
                        } else {
                            stringResource(R.string.soc_avg_clock)
                        },
                        value = when {
                            cpuInfo.cpuLoadPercent != null && hasValidClock ->
                                String.format(Locale.US, "%.0f%%", cpuInfo.cpuLoadPercent)
                            hasValidClock -> TelemetryFormatter.formatFreqKhz(cpuInfo.peakClockKhz)
                            else -> TelemetryFormatter.formatFreqKhz(cpuInfo.averageClockKhz)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatChip(
                        label = stringResource(R.string.soc_governor_label),
                        value = cpuInfo.governor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val showClockChart = hasValidClock && clockHistoryMhz.isNotEmpty()
        val showLoadChart = !showClockChart && loadHistory.isNotEmpty()
        if (showClockChart || showLoadChart) {
            item {
                TelemetryCard {
                    if (showClockChart) {
                        SectionHeader(
                            title = stringResource(R.string.soc_live_waveform_title),
                            icon = Icons.Outlined.Timeline,
                            trailing = {
                                val latest = clockHistoryMhz.lastOrNull() ?: 0f
                                LtrTelemetryText(
                                    text = String.format(Locale.US, "%.0f MHz", latest),
                                    style = TelemetryTextStyles.metricMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val ceilMhz = ((cpuInfo.peakClockKhz ?: 2_400_000L) / 1000f).coerceAtLeast(1000f)
                        TelemetrySparkline(
                            values = clockHistoryMhz,
                            lineColor = MaterialTheme.colorScheme.primary,
                            minFloor = 0f,
                            maxCeil = ceilMhz
                        )
                    } else {
                        SectionHeader(
                            title = stringResource(R.string.soc_live_load_waveform_title),
                            icon = Icons.Outlined.Timeline,
                            trailing = {
                                val latest = loadHistory.lastOrNull() ?: 0f
                                LtrTelemetryText(
                                    text = String.format(Locale.US, "%.1f%%", latest),
                                    style = TelemetryTextStyles.metricMedium,
                                    color = telemetryColors.optimal
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TelemetrySparkline(
                            values = loadHistory,
                            lineColor = telemetryColors.optimal,
                            minFloor = 0f,
                            maxCeil = 100f
                        )
                    }
                }
            }
        }

        item {
            TelemetryCard {
                SectionHeader(
                    title = stringResource(R.string.soc_clusters_title),
                    icon = Icons.Outlined.Speed,
                    trailing = {
                        if (cpuInfo.freqRestricted && cpuInfo.cpuLoadPercent == null) {
                            Text(
                                text = stringResource(R.string.soc_sysfs_restricted),
                                style = MaterialTheme.typography.labelSmall,
                                color = telemetryColors.warm
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 560.dp) 4 else 2
                    val rows = cpuInfo.cores.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowCores ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowCores.forEach { core ->
                                    CoreTelemetryTile(
                                        core = core,
                                        hasRealClocks = hasValidClock,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowCores.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            TelemetryCard {
                SectionHeader(
                    title = stringResource(R.string.soc_header_title),
                    icon = Icons.Outlined.Tune
                )
                Spacer(modifier = Modifier.height(4.dp))
                KeyValueSpecRow(
                    label = stringResource(R.string.soc_model_label),
                    value = cpuInfo.socModel
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.soc_manufacturer_label),
                    value = cpuInfo.socManufacturer
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.soc_arch_label),
                    value = cpuInfo.architecture
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.soc_abis_label),
                    value = cpuInfo.supportedAbis.joinToString(", ")
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.soc_governor_label),
                    value = cpuInfo.governor,
                    showDivider = cpuInfo.loadAverage != null || cpuInfo.bogoMips != null || cpuInfo.clusters.isNotEmpty()
                )
                if (cpuInfo.loadAverage != null) {
                    KeyValueSpecRow(
                        label = stringResource(R.string.soc_load_avg),
                        value = cpuInfo.loadAverage,
                        showDivider = cpuInfo.bogoMips != null || cpuInfo.clusters.isNotEmpty()
                    )
                }
                if (cpuInfo.bogoMips != null) {
                    KeyValueSpecRow(
                        label = stringResource(R.string.soc_bogomips_label),
                        value = cpuInfo.bogoMips,
                        showDivider = cpuInfo.clusters.isNotEmpty()
                    )
                }
                cpuInfo.clusters.forEachIndexed { index, cluster ->
                    KeyValueSpecRow(
                        label = cluster.label,
                        value = "${cluster.coreCount}x @ ${TelemetryFormatter.formatFreqKhz(cluster.maxFreqKhz)}",
                        showDivider = index < cpuInfo.clusters.lastIndex
                    )
                }

                if (cpuInfo.features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.soc_features_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cpuInfo.features.forEach { flag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                LtrTelemetryText(
                                    text = flag,
                                    style = TelemetryTextStyles.metricSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreTelemetryTile(
    core: CpuCoreInfo,
    hasRealClocks: Boolean,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current
    val animatedUtil by animateFloatAsState(
        targetValue = core.utilizationRatio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "core_util_${core.coreIndex}"
    )

    val statusColor = when {
        !core.isOnline -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        animatedUtil >= 0.8f -> telemetryColors.warm
        else -> telemetryColors.optimal
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.soc_core_label, core.coreIndex),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (core.isOnline) {
                            stringResource(R.string.soc_core_online)
                        } else {
                            stringResource(R.string.soc_core_offline)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val primaryText = when {
                !core.isOnline -> stringResource(R.string.soc_core_offline)
                hasRealClocks && core.currentFreqKhz != null -> TelemetryFormatter.formatFreqKhz(core.currentFreqKhz)
                core.loadPercent != null -> String.format(Locale.US, "%.0f%%", core.loadPercent)
                core.currentFreqKhz != null -> TelemetryFormatter.formatFreqKhz(core.currentFreqKhz)
                core.maxFreqKhz != null -> TelemetryFormatter.formatFreqKhz(core.maxFreqKhz)
                else -> "—"
            }

            LtrTelemetryText(
                text = primaryText,
                style = TelemetryTextStyles.metricMedium.copy(fontWeight = FontWeight.Bold),
                color = if (core.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { if (core.isOnline) animatedUtil else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            val subRangeText = when {
                core.loadPercent != null && hasRealClocks && core.currentFreqKhz != null ->
                    String.format(Locale.US, "Load %.0f%% • Max %s", core.loadPercent, TelemetryFormatter.formatFreqKhz(core.maxFreqKhz))
                core.maxFreqKhz != null ->
                    "${TelemetryFormatter.formatFreqKhz(core.minFreqKhz)} – ${TelemetryFormatter.formatFreqKhz(core.maxFreqKhz)}"
                else -> null
            }
            if (subRangeText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LtrTelemetryText(
                    text = subRangeText,
                    style = TelemetryTextStyles.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            LtrTelemetryText(
                text = value,
                style = TelemetryTextStyles.metricMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
