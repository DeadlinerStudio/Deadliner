package com.aritxonly.deadliner.ui.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationDockDefaults
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.kyant.shapes.Capsule
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.focus.FocusTimerState
import com.deadliner.viewmodel.focus.FocusTimerUiState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FocusGlobalBar(
    state: FocusTimerUiState,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer = state.timer ?: return
    val running = timer.state == FocusTimerState.RUNNING
    val material = LocalAdvancedMaterialSpec.current
    val accent = MiuixTheme.colorScheme.primary
    val shape = Capsule()
    // Animate the absolute clock, then wrap its minute phase when drawing the stopwatch line.
    // This avoids a backwards animation at every minute boundary and freezes naturally on pause.
    val animatedSeconds by animateFloatAsState(
        targetValue = timer.elapsedSeconds.toFloat(),
        animationSpec = tween(if (running) 1000 else 0, easing = LinearEasing),
        label = "focus-dock-elapsed",
    )
    SoftGlassSurface(
        modifier = modifier.padding(horizontal = 16.dp)
            .widthIn(max = MiuixFloatingNavigationDockDefaults.MaximumWidth)
            .fillMaxWidth()
            .padding(horizontal = MiuixFloatingNavigationDockDefaults.HorizontalPadding)
            .heightIn(min = FocusTimerDockLayout.Height)
            .softGlassShadow(shape, MaterialTheme.colorScheme.onSurface.luminance() > 0.5f),
        shape = shape,
        recipe = DeadlinerGlassRecipes.floatingNavigation(material.fineTuning),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f).heightIn(min = 50.dp)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(timer.taskTitle.ifBlank { stringResource(R.string.focus_timer_title) },
                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatFocusClock(timer.remainingSeconds ?: timer.elapsedSeconds) +
                        if (!running) " · " + stringResource(R.string.focus_paused) else "",
                    style = MiuixTheme.textStyles.footnote2.copy(fontWeight = FontWeight.Normal),
                    color = if (running) accent else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
            IconButton(
                onClick = if (running) onPause else onResume,
                enabled = !state.isBusy && if (running) timer.canPause else timer.canResume,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(iconResource(if (running) R.drawable.msr_pause_wght400 else R.drawable.msr_play_arrow_wght400),
                    contentDescription = stringResource(if (running) R.string.focus_pause else R.string.focus_resume),
                    tint = if (!state.isBusy && (timer.canPause || timer.canResume)) accent
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onFinish, enabled = !state.isBusy && timer.canFinish,
                modifier = Modifier.size(48.dp)) {
                Icon(iconResource(R.drawable.msr_stop_wght400),
                    contentDescription = stringResource(R.string.focus_finish),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.size(24.dp))
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp)) {
            Canvas(Modifier.fillMaxWidth().height(2.dp)) {
                val phase = if (timer.mode == FocusTimerMode.COUNTDOWN) {
                    (animatedSeconds / (timer.plannedDurationSeconds ?: 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                } else (animatedSeconds % 60f) / 60f
                drawLine(accent.copy(alpha = 0.12f), Offset.Zero, Offset(size.width, 0f),
                    strokeWidth = size.height, cap = StrokeCap.Round)
                drawLine(accent.copy(alpha = if (running) 0.8f else 0.35f), Offset.Zero,
                    Offset(size.width * phase, 0f), strokeWidth = size.height, cap = StrokeCap.Round)
            }
        }
    }
}
