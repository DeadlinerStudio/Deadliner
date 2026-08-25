package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.ColorfulIndicatorStyle
import com.materialkolor.ktx.harmonizeWithPrimary

data class TaskSwipeActionColors(
    val complete: Color,
    val giveUp: Color,
    val edit: Color,
    val delete: Color,
    val archive: Color,
)

@Composable
fun rememberTaskSwipeActionColors(
    colorfulIndicatorEnabled: Boolean = GlobalUtils.presetIndicatorColor,
    colorfulIndicatorStyle: ColorfulIndicatorStyle = GlobalUtils.colorfulIndicatorStyle,
): TaskSwipeActionColors {
    val scheme = MaterialTheme.colorScheme
    val harmonize = colorfulIndicatorEnabled &&
        colorfulIndicatorStyle == ColorfulIndicatorStyle.Harmonize

    fun actionColor(seed: Color, fallback: Color): Color {
        return if (harmonize) scheme.harmonizeWithPrimary(seed) else fallback
    }

    return TaskSwipeActionColors(
        complete = actionColor(
            seed = Color(0xFF22C55E),
            fallback = colorResource(R.color.chart_green),
        ),
        giveUp = actionColor(
            seed = Color(0xFFF59E0B),
            fallback = colorResource(R.color.chart_orange),
        ),
        edit = actionColor(
            seed = Color(0xFF60A5FA),
            fallback = colorResource(R.color.chart_blue),
        ),
        delete = if (harmonize) scheme.error else colorResource(R.color.chart_red),
        archive = actionColor(
            seed = Color.Gray,
            fallback = Color.Gray,
        ),
    )
}

@Composable
fun actionContentColor(background: Color): Color {
    val opaqueBackground = background.compositeOver(MaterialTheme.colorScheme.surface)
    val blackContrast = contrastRatio(opaqueBackground.luminance(), Color.Black.luminance())
    val whiteContrast = contrastRatio(opaqueBackground.luminance(), Color.White.luminance())
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

private fun contrastRatio(firstLuminance: Float, secondLuminance: Float): Float {
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}
