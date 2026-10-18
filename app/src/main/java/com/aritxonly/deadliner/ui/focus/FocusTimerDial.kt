package com.aritxonly.deadliner.ui.focus

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.Dp
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
    if (mode == FocusTimerMode.COUNTDOWN) (elapsedSeconds / (plannedSeconds ?: 1L).coerceAtLeast(1L)).coerceIn(0f, 1f)
    else elapsedSeconds.coerceAtLeast(0f) / 60f

@Composable
internal fun FocusTimerDial(timer: FocusTimerSnapshot, modifier: Modifier = Modifier) {
    val running = timer.state == FocusTimerState.RUNNING
    val elapsed by animateFloatAsState(timer.elapsedSeconds.toFloat(),
        animationSpec = tween(if (running) 1000 else 0, easing = LinearEasing), label = "focus-dial-clock")
    val turns = focusDialTurns(timer.mode, elapsed, timer.plannedDurationSeconds)
    FocusClockFace(clock = formatFocusClock(timer.remainingSeconds ?: timer.elapsedSeconds),
        label = stringResource(if (timer.mode == FocusTimerMode.COUNTDOWN) R.string.focus_remaining else R.string.focus_elapsed),
        status = stringResource(if (running) R.string.focus_running else R.string.focus_paused),
        progress = if (timer.mode == FocusTimerMode.COUNTDOWN) turns else turns % 1f,
        running = running, modifier = modifier)
}

/** Setup and live timing share geometry and typography; only the source of time changes. */
@Composable
internal fun FocusClockFace(
    clock: String, label: String, status: String, progress: Float = 0f, running: Boolean = true,
    maximumDiameter: Dp = 304.dp, modifier: Modifier = Modifier,
) {
    val accent = MiuixTheme.colorScheme.primary
    val numberScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val ringAlpha by animateFloatAsState(if (running) 1f else 0.42f, FocusVisuals.motion(), label = "focus-ring-state")
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maximumDiameter)
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val radius = size.minDimension / 2f - stroke * 2
                val origin = Offset(center.x - radius, center.y - radius)
                val bounds = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                drawCircle(accent.copy(alpha = 0.10f), radius, style = Stroke(stroke))
                drawArc(accent.copy(alpha = ringAlpha * 0.85f), -90f, progress.coerceIn(0f, 1f) * 360f,
                    false, origin, bounds, style = Stroke(stroke, cap = StrokeCap.Round))
                val angle = (progress * 360f - 90f) * Math.PI / 180f
                drawCircle(accent.copy(alpha = ringAlpha), 4.dp.toPx(),
                    Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius))
            }
            Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text(clock, style = MiuixTheme.textStyles.title1.copy(
                    fontSize = (diameter.value / (if (clock.length > 5) 6.5f else 4.8f) / numberScale).sp,
                    fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.Center, maxLines = 1, color = MiuixTheme.colorScheme.onSurface)
                Crossfade(status, animationSpec = FocusVisuals.motion(), label = "focus-clock-status") { value ->
                    Text(value, style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center, color = if (running) accent else MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}
