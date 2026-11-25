package com.example.ui.screens

import android.hardware.Sensor
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.BatteryHealthState
import com.example.data.model.BatteryInfo
import com.example.data.model.CpuInfo
import com.example.data.model.LiveSensorReading
import com.example.data.model.MemoryInfo
import com.example.data.model.SensorItem
import com.example.data.model.SystemInfo
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.StatusDotPill
import com.example.ui.components.TelemetryFormatter
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale

enum class DiagStatus {
    PENDING, PASSED, WARNING, UNSUPPORTED
}

private enum class InteractiveTestType {
    PIXEL_COLORS, MULTI_TOUCH, HAPTICS
}

private data class DiagnosticTestSpec(
    val number: Int,
    val title: String,
    val subtitle: String,
    val measuredValue: String,
    val icon: ImageVector,
    val status: DiagStatus,
    val interactiveType: InteractiveTestType? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    cpuInfo: CpuInfo?,
    systemInfo: SystemInfo?,
    memoryInfo: MemoryInfo?,
    batteryInfo: BatteryInfo?,
    sensors: List<SensorItem>,
    liveReadings: Map<Int, LiveSensorReading>,
    modifier: Modifier = Modifier
) {
    val telemetryColors = LocalTelemetryColors.current
    val resultsMap = remember { mutableStateMapOf<Int, DiagStatus>() }
    var activeInteractiveTest by remember { mutableStateOf<InteractiveTestType?>(null) }

    val accelSensor = remember(sensors) {
        sensors.firstOrNull { it.type == Sensor.TYPE_ACCELEROMETER }
    }
    val secondarySensor = remember(sensors) {
        sensors.firstOrNull { it.type == Sensor.TYPE_GYROSCOPE }
            ?: sensors.firstOrNull { it.type == Sensor.TYPE_LIGHT }
            ?: sensors.firstOrNull { it.type == Sensor.TYPE_MAGNETIC_FIELD }
    }

    fun evaluateAutomaticChecks() {
        resultsMap[1] = if (cpuInfo != null && cpuInfo.onlineCores >= 1) DiagStatus.PASSED else DiagStatus.WARNING
        resultsMap[2] = if (cpuInfo?.is64Bit == true) DiagStatus.PASSED else DiagStatus.WARNING
        resultsMap[3] = when {
            cpuInfo == null -> DiagStatus.PENDING
            cpuInfo.cpuLoadPercent != null && cpuInfo.cpuLoadPercent < 90f -> DiagStatus.PASSED
            cpuInfo.onlineCores == cpuInfo.totalCores -> DiagStatus.PASSED
            else -> DiagStatus.WARNING
        }
        resultsMap[4] = if (memoryInfo != null && !memoryInfo.isLowMemory) DiagStatus.PASSED else DiagStatus.WARNING
        resultsMap[5] = if (memoryInfo != null && memoryInfo.dataFreeBytes > 512L * 1024L * 1024L) {
            DiagStatus.PASSED
        } else {
            DiagStatus.WARNING
        }
        resultsMap[6] = when (batteryInfo?.health) {
            BatteryHealthState.GOOD -> DiagStatus.PASSED
            null -> DiagStatus.PENDING
            else -> DiagStatus.WARNING
        }
        resultsMap[7] = when {
            batteryInfo == null -> DiagStatus.PENDING
            batteryInfo.temperatureCelsius in 5f..43f -> DiagStatus.PASSED
            else -> DiagStatus.WARNING
        }
        resultsMap[8] = when {
            batteryInfo == null -> DiagStatus.PENDING
            batteryInfo.voltageMv in 3000..4700 -> DiagStatus.PASSED
            else -> DiagStatus.WARNING
        }
        resultsMap[9] = if (systemInfo != null && systemInfo.widthPx > 0 && systemInfo.refreshRateHz >= 50f) {
            DiagStatus.PASSED
        } else {
            DiagStatus.WARNING
        }
        resultsMap[10] = if (sensors.isNotEmpty()) DiagStatus.PASSED else DiagStatus.UNSUPPORTED
        resultsMap[14] = when {
            accelSensor == null -> DiagStatus.UNSUPPORTED
            liveReadings[accelSensor.id] != null -> DiagStatus.PASSED
            else -> DiagStatus.PENDING
        }
        resultsMap[15] = when {
            secondarySensor == null -> DiagStatus.UNSUPPORTED
            liveReadings[secondarySensor.id] != null -> DiagStatus.PASSED
            else -> DiagStatus.PENDING
        }
    }

    LaunchedEffect(cpuInfo?.totalCores, memoryInfo?.totalRamBytes, batteryInfo?.levelPercent, sensors.size) {
        if (resultsMap.isEmpty()) {
            evaluateAutomaticChecks()
        } else {
            if (accelSensor != null && liveReadings[accelSensor.id] != null) {
                resultsMap[14] = DiagStatus.PASSED
            }
            if (secondarySensor != null && liveReadings[secondarySensor.id] != null) {
                resultsMap[15] = DiagStatus.PASSED
            }
        }
    }

    val testSpecs = listOf(
        DiagnosticTestSpec(
            number = 1,
            title = "سلامة أنوية المعالج",
            subtitle = "التحقق من عمل جميع الأنوية المنطقية للمعالج",
            measuredValue = cpuInfo?.let { "${it.onlineCores}/${it.totalCores}" } ?: "—",
            icon = Icons.Outlined.Memory,
            status = resultsMap[1] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 2,
            title = "معمارية 64-bit",
            subtitle = "فحص دعم تعليمات المعالج الحديثة (ABI)",
            measuredValue = cpuInfo?.supportedAbis?.firstOrNull() ?: "—",
            icon = Icons.Outlined.Memory,
            status = resultsMap[2] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 3,
            title = "استقرار حمل المعالج",
            subtitle = "التأكد من عدم وجود ضغط حرج على المعالج",
            measuredValue = cpuInfo?.cpuLoadPercent?.let { String.format(Locale.US, "%.1f%%", it) }
                ?: cpuInfo?.governor ?: "—",
            icon = Icons.Outlined.FactCheck,
            status = resultsMap[3] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 4,
            title = "الذاكرة العشوائية (RAM)",
            subtitle = "فحص توفر الذاكرة وعدم بلوغ حد الخطر",
            measuredValue = memoryInfo?.let { TelemetryFormatter.formatBytes(it.availableRamBytes) } ?: "—",
            icon = Icons.Outlined.Dns,
            status = resultsMap[4] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 5,
            title = "التخزين الداخلي الحر",
            subtitle = "التحقق من وجود مساحة كافية على قسم البيانات",
            measuredValue = memoryInfo?.let { TelemetryFormatter.formatBytes(it.dataFreeBytes) } ?: "—",
            icon = Icons.Outlined.SdStorage,
            status = resultsMap[5] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 6,
            title = "الحالة الصحية للبطارية",
            subtitle = "فحص تقرير الصحة الصادر من متحكم الطاقة",
            measuredValue = batteryInfo?.health?.name ?: "—",
            icon = Icons.Outlined.BatteryChargingFull,
            status = resultsMap[6] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 7,
            title = "الأمان الحراري للبطارية",
            subtitle = "التأكد من بقاء الحرارة ضمن النطاق الطبيعي",
            measuredValue = batteryInfo?.let { String.format(Locale.US, "%.1f °C", it.temperatureCelsius) } ?: "—",
            icon = Icons.Outlined.Thermostat,
            status = resultsMap[7] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 8,
            title = "استقرار الفولتية",
            subtitle = "فحص جهد البطارية ضمن النطاق التشغيلي الآمن",
            measuredValue = batteryInfo?.let { "${it.voltageMv} mV" } ?: "—",
            icon = Icons.Outlined.Bolt,
            status = resultsMap[8] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 9,
            title = "الشاشة ومعدل التحديث",
            subtitle = "التحقق من أبعاد اللوحة وتردد الإنعاش",
            measuredValue = systemInfo?.let {
                "${it.widthPx}×${it.heightPx} • ${String.format(Locale.US, "%.0fHz", it.refreshRateHz)}"
            } ?: "—",
            icon = Icons.Outlined.Screenshot,
            status = resultsMap[9] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 10,
            title = "تعداد المستشعرات",
            subtitle = "التحقق من استجابة مدير المستشعرات بالنظام",
            measuredValue = "${sensors.size} sensors",
            icon = Icons.Outlined.Sensors,
            status = resultsMap[10] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 11,
            title = "فحص ألوان الشاشة والبكسلات",
            subtitle = "اختبار تفاعلي لاكتشاف البكسلات الميتة أو التغير اللوني",
            measuredValue = "RGBW",
            icon = Icons.Outlined.ColorLens,
            status = resultsMap[11] ?: DiagStatus.PENDING,
            interactiveType = InteractiveTestType.PIXEL_COLORS
        ),
        DiagnosticTestSpec(
            number = 12,
            title = "فحص اللمس المتعدد",
            subtitle = "اختبار تفاعلي لاستجابة الشاشة لعدة نقاط لمس",
            measuredValue = "Multi-Touch",
            icon = Icons.Outlined.TouchApp,
            status = resultsMap[12] ?: DiagStatus.PENDING,
            interactiveType = InteractiveTestType.MULTI_TOUCH
        ),
        DiagnosticTestSpec(
            number = 13,
            title = "فحص محرك الاهتزاز",
            subtitle = "اختبار تفاعلي لنبضة الاستجابة اللمسية",
            measuredValue = "Haptics",
            icon = Icons.Outlined.Vibration,
            status = resultsMap[13] ?: DiagStatus.PENDING,
            interactiveType = InteractiveTestType.HAPTICS
        ),
        DiagnosticTestSpec(
            number = 14,
            title = "استجابة مقياس التسارع",
            subtitle = "التحقق من تدفق القراءات المباشرة للمحاور الثلاثة",
            measuredValue = accelSensor?.let { s ->
                liveReadings[s.id]?.values?.firstOrNull()?.let { String.format(Locale.US, "X: %.2f", it) }
                    ?: "Ready"
            } ?: "N/A",
            icon = Icons.Outlined.Sensors,
            status = resultsMap[14] ?: DiagStatus.PENDING
        ),
        DiagnosticTestSpec(
            number = 15,
            title = "الجيروسكوب / الإضاءة",
            subtitle = "التحقق من استجابة المستشعر الثانوي المباشرة",
            measuredValue = secondarySensor?.let { s ->
                liveReadings[s.id]?.values?.firstOrNull()?.let { String.format(Locale.US, "%.2f %s", it, s.unit).trim() }
                    ?: s.friendlyTypeArabic
            } ?: "N/A",
            icon = Icons.Outlined.Sensors,
            status = resultsMap[15] ?: DiagStatus.PENDING
        )
    )

    val passedCount = testSpecs.count { it.status == DiagStatus.PASSED }

    if (activeInteractiveTest == InteractiveTestType.PIXEL_COLORS) {
        PixelColorTestDialog(
            onResult = { passed ->
                resultsMap[11] = if (passed) DiagStatus.PASSED else DiagStatus.WARNING
                activeInteractiveTest = null
            },
            onDismiss = { activeInteractiveTest = null }
        )
    }

    if (activeInteractiveTest == InteractiveTestType.MULTI_TOUCH) {
        ModalBottomSheet(
            onDismissRequest = { activeInteractiveTest = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            MultiTouchTestContent(
                onResult = { passed ->
                    resultsMap[12] = if (passed) DiagStatus.PASSED else DiagStatus.WARNING
                    activeInteractiveTest = null
                }
            )
        }
    }

    if (activeInteractiveTest == InteractiveTestType.HAPTICS) {
        ModalBottomSheet(
            onDismissRequest = { activeInteractiveTest = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            HapticsTestContent(
                onResult = { passed ->
                    resultsMap[13] = if (passed) DiagStatus.PASSED else DiagStatus.WARNING
                    activeInteractiveTest = null
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.diag_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatusDotPill(
                            label = stringResource(R.string.diag_summary_passed, passedCount),
                            dotColor = telemetryColors.optimal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { passedCount / 15f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = telemetryColors.optimal,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { evaluateAutomaticChecks() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("diag_run_auto_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.diag_run_auto))
                        }

                        OutlinedButton(
                            onClick = {
                                resultsMap.clear()
                                evaluateAutomaticChecks()
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.testTag("diag_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.diag_reset_all),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        items(testSpecs, key = { it.number }) { spec ->
            DiagnosticTestCard(
                spec = spec,
                onLaunchInteractive = {
                    if (spec.interactiveType != null) {
                        activeInteractiveTest = spec.interactiveType
                    }
                }
            )
        }
    }
}

@Composable
private fun DiagnosticTestCard(
    spec: DiagnosticTestSpec,
    onLaunchInteractive: () -> Unit
) {
    val telemetryColors = LocalTelemetryColors.current
    val statusColor = when (spec.status) {
        DiagStatus.PASSED -> telemetryColors.optimal
        DiagStatus.WARNING -> telemetryColors.warm
        DiagStatus.UNSUPPORTED -> MaterialTheme.colorScheme.onSurfaceVariant
        DiagStatus.PENDING -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when (spec.status) {
        DiagStatus.PASSED -> stringResource(R.string.diag_status_passed)
        DiagStatus.WARNING -> stringResource(R.string.diag_status_warning)
        DiagStatus.UNSUPPORTED -> stringResource(R.string.diag_status_unsupported)
        DiagStatus.PENDING -> stringResource(R.string.diag_status_pending)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_item_${spec.number}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                LtrTelemetryText(
                    text = String.format(Locale.US, "%02d", spec.number),
                    style = TelemetryTextStyles.metricMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusDotPill(label = statusLabel, dotColor = statusColor)
                    LtrTelemetryText(
                        text = spec.measuredValue,
                        style = TelemetryTextStyles.metricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (spec.interactiveType != null) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onLaunchInteractive,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("diag_launch_${spec.number}")
                ) {
                    Text(stringResource(R.string.diag_action_test))
                }
            } else {
                Icon(
                    imageVector = when (spec.status) {
                        DiagStatus.PASSED -> Icons.Outlined.CheckCircle
                        DiagStatus.WARNING -> Icons.Outlined.WarningAmber
                        else -> spec.icon
                    },
                    contentDescription = statusLabel,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PixelColorTestDialog(
    onResult: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val testColors = listOf(
        Color(0xFFEF4444),
        Color(0xFF10B981),
        Color(0xFF3B82F6),
        Color.White,
        Color.Black
    )
    var colorIndex by remember { mutableIntStateOf(0) }
    val bg = testColors[colorIndex]
    val textColor = if (bg == Color.White) Color.Black else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .clickable {
                    colorIndex = (colorIndex + 1) % testColors.size
                }
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.diag_pixel_hint),
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                modifier = Modifier.align(Alignment.Center)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onResult(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diag_action_confirm_pass))
                }
                OutlinedButton(
                    onClick = { onResult(false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diag_action_mark_issue), color = textColor)
                }
            }
        }
    }
}

@Composable
private fun MultiTouchTestContent(
    onResult: (Boolean) -> Unit
) {
    val activePointers = remember { mutableStateMapOf<Long, Offset>() }
    var maxObservedTouches by remember { mutableIntStateOf(0) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.diag_touch_hint),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.diag_touch_points, maxObservedTouches),
            style = MaterialTheme.typography.bodyMedium,
            color = primaryColor
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            activePointers.clear()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    activePointers[change.id.value] = change.position
                                }
                            }
                            if (activePointers.size > maxObservedTouches) {
                                maxObservedTouches = activePointers.size
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                activePointers.values.forEach { pt ->
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = 48.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 16.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onResult(true) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.diag_action_confirm_pass))
            }
            OutlinedButton(
                onClick = { onResult(false) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.diag_action_mark_issue))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun HapticsTestContent(
    onResult: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Vibration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.diag_haptic_trigger))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onResult(true) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.diag_action_confirm_pass))
            }
            OutlinedButton(
                onClick = { onResult(false) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.diag_action_mark_issue))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
