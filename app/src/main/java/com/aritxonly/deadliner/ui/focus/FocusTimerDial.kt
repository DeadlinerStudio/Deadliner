package com.aritxonly.deadliner.ui.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.focus.FocusTimerSnapshot
import com.deadliner.domain.focus.FocusTimerState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.sin

internal fun focusDialTurns(mode: FocusTimerMode, elapsedSeconds: Float, plannedSeconds: Long?): Float =
    if (mode == FocusTimerMode.COUNTDOWN) {
        (elapsedSeconds / (plannedSeconds ?: 1L).coerceAtLeast(1L)).coerceIn(0f, 1f)
    } else elapsedSeconds.coerceAtLeast(0f) / 60f

@Composable
internal fun FocusTimerDial(timer: FocusTimerSnapshot) {
    val running = timer.state == FocusTimerState.RUNNING
    val elapsed by animateFloatAsState(timer.elapsedSeconds.toFloat(),
        animationSpec = tween(if (running) 1000 else 0, easing = LinearEasing), label = "focus-dial-clock")
    val accent = MiuixTheme.colorScheme.primary
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, 320.dp)
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 5.dp.toPx()
                val radius = size.minDimension / 2f - stroke
                val origin = Offset(center.x - radius, center.y - radius)
                val bounds = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                drawCircle(accent.copy(alpha = 0.10f), radius, style = Stroke(stroke))
                val turns = focusDialTurns(timer.mode, elapsed, timer.plannedDurationSeconds)
                val progress = if (timer.mode == FocusTimerMode.COUNTDOWN) turns else turns % 1f
                drawArc(accent.copy(alpha = if (running) 0.85f else 0.4f), -90f,
                    progress * 360f, useCenter = false, topLeft = origin, size = bounds,
                    style = Stroke(stroke, cap = StrokeCap.Round))
                val angle = (progress * 360f - 90f) * Math.PI / 180f
                drawCircle(accent.copy(alpha = if (running) 1f else 0.4f), 5.dp.toPx(),
                    Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (timer.mode == FocusTimerMode.COUNTDOWN) R.string.focus_remaining else R.string.focus_elapsed),
                    style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Normal),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                val clock = formatFocusClock(timer.remainingSeconds ?: timer.elapsedSeconds)
                Text(clock,
                    style = MiuixTheme.textStyles.title1.copy(
                        fontSize = (diameter.value / if (clock.length > 5) 6.3f else 4.6f).sp,
                        fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.Center, maxLines = 1,
                    color = MiuixTheme.colorScheme.onSurface)
                Text(stringResource(if (running) R.string.focus_running else R.string.focus_paused),
                    style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Medium),
                    color = if (running) accent else MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}
