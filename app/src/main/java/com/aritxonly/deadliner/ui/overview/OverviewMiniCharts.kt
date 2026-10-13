package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R

/** Small, quiet previews; full axes and chart interactions remain on the Trends page. */
@Composable
internal fun OverviewMiniChart(values: List<Float>, color: Color, label: String,
    modifier: Modifier = Modifier, bars: Boolean = false) {
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .15f)
    Canvas(modifier.semantics { contentDescription = label }) {
        val inset = 3.dp.toPx()
        val bottom = size.height - inset
        val height = (size.height - 2 * inset).coerceAtLeast(0f)
        val width = (size.width - 2 * inset).coerceAtLeast(0f)
        val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        if (values.isEmpty() || values.all { it == 0f }) {
            drawLine(neutral, Offset(inset, bottom), Offset(size.width - inset, bottom), 2.dp.toPx(), StrokeCap.Round)
        } else if (bars) {
            val step = width / values.size
            values.forEachIndexed { index, value ->
                val barHeight = (height * value / max).coerceAtLeast(2.dp.toPx())
                drawRoundRect(if (value > 0) color.copy(alpha = .45f + .5f * value / max) else neutral,
                    Offset(inset + step * index + step * .18f, bottom - barHeight),
                    Size(step * .64f, barHeight), CornerRadius(2.dp.toPx()))
            }
        } else {
            val points = values.mapIndexed { index, value ->
                Offset(inset + width * index / (values.size - 1).coerceAtLeast(1), bottom - height * value / max)
            }
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.zipWithNext().forEach { (start, end) ->
                    val control = (end.x - start.x) / 3
                    cubicTo(start.x + control, start.y, end.x - control, end.y, end.x, end.y)
                }
            }
            val fill = Path().apply {
                addPath(line); lineTo(points.last().x, bottom); lineTo(points.first().x, bottom); close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = .16f), color.copy(alpha = .01f))))
            drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color, 2.5.dp.toPx(), points.last())
        }
    }
}

@Composable
internal fun OverviewTrendPreviews(taskValues: List<Float>, focusValues: List<Float>) {
    val palette = rememberOverviewIndicatorPalette()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.dashboard_mini_tasks), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OverviewMiniChart(taskValues, palette.completed, stringResource(R.string.dashboard_mini_tasks),
                Modifier.fillMaxWidth().height(72.dp), bars = true)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.dashboard_mini_focus), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OverviewMiniChart(focusValues, palette.total, stringResource(R.string.dashboard_mini_focus),
                Modifier.fillMaxWidth().height(72.dp))
        }
    }
}
