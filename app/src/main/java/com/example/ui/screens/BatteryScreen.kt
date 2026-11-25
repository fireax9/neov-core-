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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BatteryChargeStatus
import com.example.data.model.BatteryHealthState
import com.example.data.model.BatteryInfo
import com.example.data.model.BatteryPowerSource
import com.example.data.model.BatteryThermalLevel
import com.example.ui.components.KeyValueSpecRow
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.RadialTelemetryGauge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusDotPill
import com.example.ui.components.TelemetryCard
import com.example.ui.components.TelemetrySparkline
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale

@Composable
fun BatteryScreen(
    batteryInfo: BatteryInfo?,
    tempHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current

    if (batteryInfo == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.battery_header_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val levelColor = when {
        batteryInfo.status == BatteryChargeStatus.CHARGING ||
            batteryInfo.status == BatteryChargeStatus.FULL -> telemetryColors.optimal
        batteryInfo.levelPercent <= 20 -> telemetryColors.critical
        batteryInfo.levelPercent <= 40 -> telemetryColors.warm
        else -> MaterialTheme.colorScheme.primary
    }

    val thermalColor = when (batteryInfo.thermalLevel) {
        BatteryThermalLevel.COOL -> telemetryColors.cool
        BatteryThermalLevel.NORMAL -> telemetryColors.optimal
        BatteryThermalLevel.WARM -> telemetryColors.warm
        BatteryThermalLevel.HOT -> telemetryColors.critical
    }

    val statusText = when (batteryInfo.status) {
        BatteryChargeStatus.CHARGING -> stringResource(R.string.battery_status_charging)
        BatteryChargeStatus.DISCHARGING -> stringResource(R.string.battery_status_discharging)
        BatteryChargeStatus.FULL -> stringResource(R.string.battery_status_full)
        BatteryChargeStatus.NOT_CHARGING -> stringResource(R.string.battery_status_not_charging)
        BatteryChargeStatus.UNKNOWN -> stringResource(R.string.battery_status_unknown)
    }

    val sourceText = when (batteryInfo.powerSource) {
        BatteryPowerSource.AC -> stringResource(R.string.battery_source_ac)
        BatteryPowerSource.USB -> stringResource(R.string.battery_source_usb)
        BatteryPowerSource.WIRELESS -> stringResource(R.string.battery_source_wireless)
        BatteryPowerSource.BATTERY -> stringResource(R.string.battery_source_battery)
    }

    val healthText = when (batteryInfo.health) {
        BatteryHealthState.GOOD -> stringResource(R.string.battery_health_good)
        BatteryHealthState.OVERHEAT -> stringResource(R.string.battery_health_overheat)
        BatteryHealthState.DEAD -> stringResource(R.string.battery_health_dead)
        BatteryHealthState.OVER_VOLTAGE -> stringResource(R.string.battery_health_over_voltage)
        BatteryHealthState.COLD -> stringResource(R.string.battery_health_cold)
        BatteryHealthState.FAILURE -> stringResource(R.string.battery_health_failure)
        BatteryHealthState.UNKNOWN -> stringResource(R.string.battery_health_unknown)
    }

    val healthColor = if (batteryInfo.health == BatteryHealthState.GOOD) {
        telemetryColors.optimal
    } else {
        telemetryColors.critical
    }

    val thermalLabel = when (batteryInfo.thermalLevel) {
        BatteryThermalLevel.COOL -> stringResource(R.string.battery_thermal_cool)
        BatteryThermalLevel.NORMAL -> stringResource(R.string.battery_thermal_normal)
        BatteryThermalLevel.WARM -> stringResource(R.string.battery_thermal_warm)
        BatteryThermalLevel.HOT -> stringResource(R.string.battery_thermal_hot)
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
                border = BorderStroke(1.dp, levelColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.battery_header_title),
                            icon = Icons.Outlined.BatteryChargingFull,
                            trailing = {
                                StatusDotPill(label = statusText, dotColor = levelColor)
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
                            progress = batteryInfo.levelPercent / 100f,
                            centerValue = "${batteryInfo.levelPercent}%",
                            centerLabel = batteryInfo.technology,
                            accentColor = levelColor
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KeyValueSpecRow(
                                label = stringResource(R.string.battery_source_label),
                                value = sourceText
                            )
                            KeyValueSpecRow(
                                label = stringResource(R.string.battery_health_label),
                                value = healthText,
                                valueColor = healthColor,
                                showDivider = false
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BatteryMetricTile(
                    icon = Icons.Outlined.Thermostat,
                    label = stringResource(R.string.battery_temp_label),
                    value = String.format(Locale.US, "%.1f °C", batteryInfo.temperatureCelsius),
                    subLabel = thermalLabel,
                    accentColor = thermalColor,
                    modifier = Modifier.weight(1f)
                )
                BatteryMetricTile(
                    icon = Icons.Outlined.Bolt,
                    label = stringResource(R.string.battery_voltage_label),
                    value = String.format(Locale.US, "%d mV", batteryInfo.voltageMv),
                    subLabel = String.format(Locale.US, "%.2f V", batteryInfo.voltageMv / 1000f),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val currentDisplay = batteryInfo.currentMa?.let {
                    String.format(Locale.US, "%+.0f mA", it)
                } ?: stringResource(R.string.value_unavailable)
                val powerDisplay = batteryInfo.powerWatts?.let {
                    String.format(Locale.US, "%.2f W", it)
                } ?: "—"

                BatteryMetricTile(
                    icon = Icons.Outlined.ElectricalServices,
                    label = stringResource(R.string.battery_current_label),
                    value = currentDisplay,
                    subLabel = powerDisplay,
                    accentColor = telemetryColors.optimal,
                    modifier = Modifier.weight(1f)
                )
                BatteryMetricTile(
                    icon = Icons.Outlined.HealthAndSafety,
                    label = stringResource(R.string.battery_health_label),
                    value = healthText,
                    subLabel = batteryInfo.chargeCounterMah?.let { "$it mAh" } ?: batteryInfo.technology,
                    accentColor = healthColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (tempHistory.isNotEmpty()) {
            item {
                TelemetryCard {
                    SectionHeader(
                        title = stringResource(R.string.battery_temp_history_title),
                        icon = Icons.Outlined.Timeline,
                        trailing = {
                            LtrTelemetryText(
                                text = String.format(Locale.US, "%.1f °C", batteryInfo.temperatureCelsius),
                                style = TelemetryTextStyles.metricMedium,
                                color = thermalColor
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TelemetrySparkline(
                        values = tempHistory,
                        lineColor = thermalColor,
                        minFloor = 15f,
                        maxCeil = 50f
                    )
                }
            }
        }

        item {
            TelemetryCard {
                KeyValueSpecRow(
                    label = stringResource(R.string.battery_status_label),
                    value = statusText
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.battery_tech_label),
                    value = batteryInfo.technology
                )
                KeyValueSpecRow(
                    label = stringResource(R.string.battery_power_label),
                    value = batteryInfo.powerWatts?.let { String.format(Locale.US, "%.2f W", it) }
                        ?: stringResource(R.string.value_unavailable),
                    showDivider = batteryInfo.chargeCounterMah != null
                )
                if (batteryInfo.chargeCounterMah != null) {
                    KeyValueSpecRow(
                        label = stringResource(R.string.battery_charge_counter_label),
                        value = "${batteryInfo.chargeCounterMah} mAh",
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LtrTelemetryText(
                text = value,
                style = TelemetryTextStyles.metricLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            LtrTelemetryText(
                text = subLabel,
                style = TelemetryTextStyles.metricSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
