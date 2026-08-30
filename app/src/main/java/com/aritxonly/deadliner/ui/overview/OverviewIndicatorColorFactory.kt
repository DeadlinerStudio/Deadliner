package com.aritxonly.deadliner.ui.overview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.materialkolor.ktx.harmonizeWithPrimary

enum class OverviewIndicatorRole {
    Total,
    Completed,
    Pending,
    Overdue,
    Abandoned,
}

data class OverviewIndicatorPalette(
    val total: Color,
    val completed: Color,
    val pending: Color,
    val overdue: Color,
    val abandoned: Color,
) {
    fun colorFor(role: OverviewIndicatorRole): Color = when (role) {
        OverviewIndicatorRole.Total -> total
        OverviewIndicatorRole.Completed -> completed
        OverviewIndicatorRole.Pending -> pending
        OverviewIndicatorRole.Overdue -> overdue
        OverviewIndicatorRole.Abandoned -> abandoned
    }
}

@Composable
fun rememberOverviewIndicatorPalette(
    colorfulIndicatorEnabled: Boolean = GlobalUtils.presetIndicatorColor,
): OverviewIndicatorPalette {
    return if (colorfulIndicatorEnabled) {
        harmonizedOverviewIndicatorPalette()
    } else {
        morandiOverviewIndicatorPalette()
    }
}

@Composable
fun rememberOverviewIndicatorColor(
    role: OverviewIndicatorRole,
    colorfulIndicatorEnabled: Boolean = GlobalUtils.presetIndicatorColor,
): Color {
    return rememberOverviewIndicatorPalette(colorfulIndicatorEnabled).colorFor(role)
}

@Composable
fun rememberOverviewHeatmapColor(
    role: OverviewIndicatorRole,
    count: Int,
    colorfulIndicatorEnabled: Boolean = GlobalUtils.presetIndicatorColor,
): Color {
    val palette = rememberOverviewIndicatorPalette(colorfulIndicatorEnabled)
    val accent = palette.colorFor(role)
    return when {
        count <= 0 -> MaterialTheme.colorScheme.surfaceVariant
        count < 2 -> accent.copy(alpha = 0.30f)
        count < 4 -> accent.copy(alpha = 0.50f)
        count < 6 -> accent.copy(alpha = 0.72f)
        else -> accent
    }
}

@Composable
private fun harmonizedOverviewIndicatorPalette(): OverviewIndicatorPalette {
    return OverviewIndicatorPalette(
        total = MaterialTheme.colorScheme.harmonizeWithPrimary(Color(0xFF3B82F6)),
        completed = MaterialTheme.colorScheme.harmonizeWithPrimary(Color(0xFF22C55E)),
        pending = MaterialTheme.colorScheme.harmonizeWithPrimary(Color(0xFFF59E0B)),
        overdue = MaterialTheme.colorScheme.harmonizeWithPrimary(Color.Red),
        abandoned = Color(0xFF8E8E93),
    )
}

@Composable
private fun morandiOverviewIndicatorPalette(): OverviewIndicatorPalette {
    return OverviewIndicatorPalette(
        total = colorResource(R.color.indicator_morandi_undergo),
        completed = colorResource(R.color.indicator_morandi_completed),
        pending = colorResource(R.color.indicator_morandi_near),
        overdue = colorResource(R.color.indicator_morandi_passed),
        abandoned = Color(0xFF8E8E93),
    )
}
