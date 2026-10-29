package com.gymbro.mad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gymbro.mad.data.model.WeekVolume
import com.gymbro.mad.ui.theme.LocalPalette
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun WeeklyVolumeChart(
    weeks: List<WeekVolume>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val maxVolume = max(1.0, weeks.maxOfOrNull { it.volumeKg } ?: 0.0)
    val reveal by animateFloatAsState(1f, animationSpec = tween(650), label = "volume chart reveal")
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(164.dp)
                .pointerInput(weeks.size) {
                    detectTapGestures(
                        onTap = { tap ->
                            if (weeks.isNotEmpty() && size.width > 0) {
                                onSelect((tap.x / size.width * weeks.size).toInt().coerceIn(weeks.indices))
                            }
                        },
                        onLongPress = { tap ->
                            if (weeks.isNotEmpty() && size.width > 0) {
                                onSelect((tap.x / size.width * weeks.size).toInt().coerceIn(weeks.indices))
                            }
                        },
                    )
                },
        ) {
            val top = 8.dp.toPx()
            val bottom = 8.dp.toPx()
            val chartHeight = size.height - top - bottom
            repeat(4) { line ->
                val y = top + chartHeight * line / 3f
                drawLine(
                    color = palette.faint.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            val slot = size.width / weeks.size.coerceAtLeast(1)
            val barWidth = slot * 0.56f
            weeks.forEachIndexed { index, week ->
                val barHeight = (week.volumeKg / maxVolume).toFloat() * chartHeight * reveal
                val left = index * slot + (slot - barWidth) / 2f
                if (week.volumeKg > 0.0) {
                    drawRect(
                        color = if (index == selectedIndex) palette.accent2 else palette.accent,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            left,
                            top + chartHeight - barHeight,
                        ),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weeks.forEachIndexed { index, week ->
                MicroLabel(
                    text = week.weekStart.format(DateTimeFormatter.ofPattern("d/M")),
                    color = if (index == selectedIndex) palette.accentInk else palette.faint,
                )
            }
        }
    }
}

@Composable
fun ProgressionChart(
    values: List<Double>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val reveal by animateFloatAsState(1f, animationSpec = tween(650), label = "progress chart reveal")
    val low = values.minOrNull() ?: 0.0
    val high = values.maxOrNull() ?: 0.0
    val span = (high - low).takeIf { it > 0.0 } ?: 1.0
    Canvas(
        modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(values.size) {
                detectTapGestures(
                    onTap = { tap ->
                        if (values.isNotEmpty() && size.width > 0) {
                            onSelect((tap.x / size.width * values.size).toInt().coerceIn(values.indices))
                        }
                    },
                    onLongPress = { tap ->
                        if (values.isNotEmpty() && size.width > 0) {
                            onSelect((tap.x / size.width * values.size).toInt().coerceIn(values.indices))
                        }
                    },
                )
            },
    ) {
        val padding = 12.dp.toPx()
        val width = (size.width - padding * 2).coerceAtLeast(1f)
        val height = (size.height - padding * 2).coerceAtLeast(1f)
        repeat(3) { line ->
            val y = padding + height * line / 2f
            drawLine(
                color = palette.faint.copy(alpha = 0.12f),
                start = androidx.compose.ui.geometry.Offset(padding, y),
                end = androidx.compose.ui.geometry.Offset(size.width - padding, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        if (values.isEmpty()) return@Canvas
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) padding + width / 2 else {
                padding + width * index / (values.size - 1)
            }
            val y = padding + height -
                ((value - low) / span).toFloat() * height * reveal
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, palette.accent, style = Stroke(width = 3.dp.toPx()))
        points.forEachIndexed { index, point ->
            drawCircle(
                color = if (index == selectedIndex) palette.accent2 else palette.accent,
                radius = if (index == selectedIndex) 6.dp.toPx() else 4.dp.toPx(),
                center = point,
            )
        }
    }
}
