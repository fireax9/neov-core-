package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SystemInfo
import com.example.ui.components.KeyValueSpecRow
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusDotPill
import com.example.ui.components.TelemetryCard
import com.example.ui.components.TelemetryFormatter
import com.example.ui.components.TelemetryHeroCard
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale

@Composable
fun SystemScreen(
    systemInfo: SystemInfo?,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current

    if (systemInfo == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.system_header_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TelemetryHeroCard(
                title = "${systemInfo.brand} ${systemInfo.model}",
                subtitle = "${systemInfo.device} • ${systemInfo.board}",
                heroValue = "Android ${systemInfo.androidVersion}",
                heroLabel = stringResource(R.string.system_uptime_label) + ": " +
                    TelemetryFormatter.formatUptime(systemInfo.elapsedRealtimeMs),
                icon = Icons.Outlined.PhoneAndroid,
                accentColor = telemetryColors.optimal,
                badgeText = "API ${systemInfo.apiLevel}"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusDotPill(
                        label = "${systemInfo.widthPx}×${systemInfo.heightPx}",
                        dotColor = MaterialTheme.colorScheme.primary
                    )
                    StatusDotPill(
                        label = String.format(Locale.US, "%.0f Hz", systemInfo.refreshRateHz),
                        dotColor = telemetryColors.optimal
                    )
                    StatusDotPill(
                        label = "${systemInfo.densityDpi} dpi",
                        dotColor = telemetryColors.warm
                    )
                }
            }
        }

        item {
            TelemetryCard {
                SectionHeader(
                    title = stringResource(R.string.system_device_section),
                    icon = Icons.Outlined.PhoneAndroid
                )
                Spacer(modifier = Modifier.height(4.dp))
                KeyValueSpecRow(
                    label = stringResource(R.string.system_brand_label),
                    value = systemInfo.brand
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_manufacturer_label),
                    value = systemInfo.manufacturer
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_model_label),
                    value = systemInfo.model
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_device_code_label),
                    value = "${systemInfo.device} (${systemInfo.product})"
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_board_label),
                    value = systemInfo.board,
                    showDivider = false
                )
            }
        }

        item {
            TelemetryCard {
                SectionHeader(
                    title = stringResource(R.string.system_display_section),
                    icon = Icons.Outlined.Screenshot
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LtrTelemetryText(
                                text = String.format(Locale.US, "%.1f\"", systemInfo.screenInches),
                                style = TelemetryTextStyles.metricLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LtrTelemetryText(
                                text = systemInfo.densityBucket.uppercase(Locale.US),
                                style = TelemetryTextStyles.metricSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        KeyValueSpecRow(
                            label = stringResource(R.string.system_resolution_label),
                            value = "${systemInfo.widthPx} × ${systemInfo.heightPx} px"
                        )
                        KeyValueSpecRow(
                            label = stringResource(R.string.system_density_label),
                            value = "${systemInfo.densityDpi} dpi (${String.format(Locale.US, "%.2fx", systemInfo.densityScale)})"
                        )
                        KeyValueSpecRow(
                            label = stringResource(R.string.system_refresh_rate_label),
                            value = String.format(Locale.US, "%.1f Hz", systemInfo.refreshRateHz),
                            showDivider = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapabilityBadge(
                        label = stringResource(R.string.system_hdr_label),
                        supported = systemInfo.isHdrSupported,
                        modifier = Modifier.weight(1f)
                    )
                    CapabilityBadge(
                        label = stringResource(R.string.system_wide_color_label),
                        supported = systemInfo.isWideColorGamut,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            TelemetryCard {
                SectionHeader(
                    title = stringResource(R.string.system_os_section),
                    icon = Icons.Outlined.Android
                )
                Spacer(modifier = Modifier.height(4.dp))
                KeyValueSpecRow(
                    label = stringResource(R.string.system_android_ver_label),
                    value = systemInfo.androidVersion
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_api_level_label),
                    value = systemInfo.apiLevel.toString()
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_security_patch_label),
                    value = systemInfo.securityPatch
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_build_id_label),
                    value = systemInfo.buildId
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_kernel_label),
                    value = systemInfo.kernelVersion
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_vm_label),
                    value = "ART ${systemInfo.vmVersion}"
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.system_uptime_label),
                    value = TelemetryFormatter.formatUptime(systemInfo.elapsedRealtimeMs),
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun CapabilityBadge(
    label: String,
    supported: Boolean,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current
    val color = if (supported) telemetryColors.optimal else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (supported) {
                    stringResource(R.string.value_supported)
                } else {
                    stringResource(R.string.value_not_supported)
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
