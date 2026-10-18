package com.aritxonly.deadliner.ui.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.materialSymbolPainter
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** A single visual vocabulary for setup, management and the running timer. */
internal object FocusVisuals {
    val Corner = 24.dp
    val Gap = 12.dp
    val PageGutter = 16.dp
    val MaximumWidth = 1200.dp
    const val MotionMillis = 280
    fun <T> motion() = tween<T>(durationMillis = MotionMillis, easing = FastOutSlowInEasing)
}

internal data class FocusLayout(val summaryColumns: Int, val historyColumns: Int, val dashboardWide: Boolean, val setupWide: Boolean, val sessionWide: Boolean)

/** Minimum readable tile widths, including large text, decide reflow rather than device models. */
internal fun focusLayout(widthDp: Float, fontScale: Float): FocusLayout {
    val width = widthDp.coerceAtLeast(0f)
    val textScale = fontScale.coerceAtLeast(1f)
    return FocusLayout(
        summaryColumns = if (width >= 2 * 150f * textScale + 12f) 2 else 1,
        historyColumns = if (width >= 2 * 330f * textScale + 12f) 2 else 1,
        dashboardWide = width >= 840f * textScale.coerceAtMost(1.4f),
        setupWide = width >= 520f && textScale < 1.4f,
        sessionWide = width >= 620f && textScale < 1.4f,
    )
}

@Composable
internal fun FocusPanel(
    modifier: Modifier = Modifier, padding: Dp = 20.dp, emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Card(modifier = modifier.fillMaxWidth(), cornerRadius = FocusVisuals.Corner,
        colors = CardDefaults.defaultColors(
            color = if (emphasized) androidx.compose.ui.graphics.lerp(colors.surfaceContainer, colors.primary, 0.07f) else colors.surfaceContainer,
            contentColor = colors.onSurfaceContainer,
        )) {
        Column(Modifier.fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun FocusTargetHeading(title: String, supporting: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(MiuixTheme.colorScheme.primary.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(materialSymbolPainter("timer"), null, Modifier.size(23.dp), tint = MiuixTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium), maxLines = 2,
                color = MiuixTheme.colorScheme.onSurfaceContainer)
            if (supporting.isNotBlank()) Text(supporting, style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (onClick != null) Icon(materialSymbolPainter("expand_more"), null, Modifier.size(com.aritxonly.deadliner.ui.overviewChevronSize()), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
    }
}

@Composable
internal fun FocusSectionHeading(title: String, supporting: String = "", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium), color = MiuixTheme.colorScheme.onSurface)
        if (supporting.isNotBlank()) Text(supporting, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
