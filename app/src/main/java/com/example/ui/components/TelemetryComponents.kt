package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.TelemetryTextStyles
import java.util.Locale
import kotlin.math.max

object TelemetryFormatter {
    fun formatFreqKhz(khz: Long?): String {
        if (khz == null || khz <= 0L) return "—"
        return when {
            khz >= 1_000_000L -> String.format(Locale.US, "%.2f GHz", khz / 1_000_000.0)
            khz >= 1_000L -> String.format(Locale.US, "%d MHz", khz / 1_000L)
            else -> String.format(Locale.US, "%d kHz", khz)
        }
    }

    fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "—"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatUptime(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val days = totalSeconds / 86400L
        val hours = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (days > 0) {
            String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}

@Composable
fun LtrTelemetryText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TelemetryTextStyles.metricMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    val telemetryColors = LocalTelemetryColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, telemetryColors.cardBorder.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun TelemetryHeroCard(
    title: String,
    subtitle: String,
    heroValue: String,
    heroLabel: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    badgeText: String? = null,
    modifier: Modifier = Modifier,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val telemetryColors = LocalTelemetryColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                            telemetryColors.optimal.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LtrTelemetryText(
                                text = subtitle,
                                style = TelemetryTextStyles.metricSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            LtrTelemetryText(
                                text = badgeText,
                                style = TelemetryTextStyles.metricSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = heroLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LtrTelemetryText(
                            text = heroValue,
                            style = TelemetryTextStyles.metricHero,
                            color = accentColor
                        )
                    }
                }

                if (extraContent != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    extraContent()
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun KeyValueSpecRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.46f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(0.54f),
                contentAlignment = Alignment.CenterEnd
            ) {
                LtrTelemetryText(
                    text = value,
                    style = TelemetryTextStyles.metricMedium,
                    color = valueColor,
                    maxLines = 2
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun TelemetrySparkline(
    values: List<Float>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    minFloor: Float? = null,
    maxCeil: Float? = null,
    height: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    val gridColor = LocalTelemetryColors.current.gridLine
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle horizontal grid lines
            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val y = h * fraction
                drawLine(
                    color = gridColor.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            if (values.size < 2) {
                val midY = h * 0.5f
                drawLine(
                    color = lineColor.copy(alpha = 0.4f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 2.5f
                )
                return@Canvas
            }

            val rawMin = minFloor ?: (values.minOrNull() ?: 0f)
            val rawMax = maxCeil ?: (values.maxOrNull() ?: 1f)
            val span = max(rawMax - rawMin, 1f)

            val points = values.mapIndexed { index, v ->
                val x = (index.toFloat() / (values.size - 1).toFloat()) * w
                val norm = ((v - rawMin) / span).coerceIn(0.05f, 0.95f)
                val y = h - (norm * h)
                Offset(x, y)
            }

            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }

            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        lineColor.copy(alpha = 0.0f)
                    )
                )
            )

            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            val last = points.last()
            drawCircle(
                color = lineColor.copy(alpha = 0.28f),
                radius = 9f,
                center = last
            )
            drawCircle(
                color = lineColor,
                radius = 4.5f,
                center = last
            )
        }
    }
}

@Composable
fun RadialTelemetryGauge(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    accentColor: Color,
    size: Dp = 136.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "gauge_progress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val startAngle = 140f
            val sweepMax = 260f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepMax,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.65f),
                        accentColor
                    )
                ),
                startAngle = startAngle,
                sweepAngle = sweepMax * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LtrTelemetryText(
                text = centerValue,
                style = TelemetryTextStyles.metricLarge,
                color = accentColor
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusDotPill(
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = dotColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = dotColor
            )
        }
    }
}
