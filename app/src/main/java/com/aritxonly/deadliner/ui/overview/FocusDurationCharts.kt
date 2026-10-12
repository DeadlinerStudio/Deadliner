package com.aritxonly.deadliner.ui.overview

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.deadliner.domain.statistics.FocusDurationPoint
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import java.time.LocalDate
import java.util.Locale

/** Project compose-charts 0.1.7; seconds stay precise in storage/popups, chart values are minutes. */
@Composable
internal fun FocusDurationChart(points: List<FocusDurationPoint>, kind: FocusChartKind, tag: String) {
    if (points.none { it.durationSeconds > 0 }) {
        Text(stringResource(R.string.focus_stats_empty_period), Modifier.padding(top = 8.dp).testTag("$tag-empty"),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val color = rememberOverviewIndicatorPalette().total
    val labels = focusChartLabels(points, kind, locale)
    val maximum = points.maxOf { it.durationSeconds / 60.0 }.coerceAtLeast(1.0)
    val description = points.joinToString("; ") { listOfNotNull("${it.startDate}: ${focusDuration(context, it.durationSeconds)}", focusTargetTimeBrief(context, it.durationByTarget)).joinToString("; ") }
    val popup = DefaultPopupProperties.copy(contentBuilder = { dataIndex, valueIndex, _ ->
        val point = points[if (kind == FocusChartKind.MONTHLY) valueIndex else dataIndex]
        val start = LocalDate.parse(point.startDate)
        val end = LocalDate.parse(point.endDateExclusive).minusDays(1)
        val period = if (start == end) start.toString() else "$start – $end"
        listOfNotNull("$period: ${focusDuration(context, point.durationSeconds)}",
            focusTargetTimeBrief(context, point.durationByTarget)).joinToString("\n")
    })
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Library caches measured geometry with remember {}; re-key on data/theme/locale changes.
        key(points, color, locale, kind) {
            val chartModifier = Modifier.fillMaxWidth().height(120.dp).testTag(tag)
                .semantics { contentDescription = description }
            if (kind == FocusChartKind.MONTHLY) {
                LineChart(
                    modifier = chartModifier,
                    data = listOf(Line(label = stringResource(R.string.focus_stats_input),
                        values = points.map { it.durationSeconds / 60.0 }, color = SolidColor(color),
                        firstGradientFillColor = color.copy(alpha = 0.16f), secondGradientFillColor = Color.Transparent,
                        drawStyle = DrawStyle.Stroke(width = 2.dp), strokeAnimationSpec = tween(400),
                        gradientAnimationDelay = 0, dotProperties = DotProperties(enabled = false,
                            color = SolidColor(color), strokeColor = SolidColor(color)))),
                    curvedEdges = false,
                    maxValue = maximum,
                    animationDelay = 0,
                    popupProperties = popup,
                    gridProperties = GridProperties(enabled = false),
                    indicatorProperties = HorizontalIndicatorProperties(enabled = false),
                    labelProperties = DefaultLabelProperties.copy(labels = labels),
                    labelHelperProperties = LabelHelperProperties(enabled = false),
                )
            } else {
                val bars = remember(points, color, labels) {
                    points.mapIndexed { index, point -> Bars(label = labels[index], values = listOf(
                        Bars.Data(value = point.durationSeconds / 60.0,
                            color = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.45f))))) ) }
                }
                ColumnChart(
                    modifier = chartModifier,
                    data = bars,
                    maxValue = maximum,
                    barProperties = BarProperties(spacing = 2.dp,
                        thickness = if (kind == FocusChartKind.MONTH_DAILY) 5.dp else 16.dp,
                        cornerRadius = Bars.Data.Radius.Rectangle(topLeft = 3.dp, topRight = 3.dp)),
                    animationDelay = 0,
                    animationMode = AnimationMode.Together(),
                    popupProperties = popup,
                    gridProperties = GridProperties(enabled = false),
                    indicatorProperties = HorizontalIndicatorProperties(enabled = false),
                    // 0.1.7 auto-rotation divides by the narrowest label; sparse labels have width zero.
                    labelProperties = DefaultLabelProperties.copy(rotation = LabelProperties.Rotation(degree = 0f)),
                    labelHelperProperties = LabelHelperProperties(enabled = false),
                )
            }
        }
    }
}
