package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal val QuickAddPanelGap = 12.dp
internal val QuickAddMinimumControlsHeight = 64.dp

/** Same panel clearance above the dock as the expanded quick-add lens. */
internal fun quickAddPanelGapAboveControls(controlHeight: Dp): Dp =
    (maxOf(controlHeight, QuickAddMinimumControlsHeight) - controlHeight) / 2 + QuickAddPanelGap

internal data class QuickAddPlacement(
    val rightInset: Float,
    val panelBottomInset: Float,
    val actionBottomInset: Float,
    val liftAction: Boolean,
    val actionDiameter: Float,
    val dismissAreaHeight: Float,
)

/** All inputs are pixels; the anchor is in window coordinates, the panel uses its host's space. */
internal fun quickAddPlacement(
    size: Size, origin: Offset, anchor: Rect, imeBottom: Float,
    gap: Float, actionSize: Float, minimumControlsHeight: Float,
): QuickAddPlacement {
    val local = if (anchor.width > 0f) anchor.translate(-origin) else
        Rect(size.width - actionSize - gap, size.height - actionSize - gap, size.width - gap, size.height - gap)
    val diameter = if (anchor.width > 0f) minOf(local.width, local.height) else actionSize
    val controlTop = local.center.y - max(local.height, minimumControlsHeight) / 2f
    val actionBottom = size.height - local.bottom
    val panelBottom = max(size.height - controlTop + gap, if (imeBottom > 0f) imeBottom + gap else 0f)
    return QuickAddPlacement(
        rightInset = (size.width - local.right).coerceIn(gap, max(gap, size.width - actionSize)),
        panelBottomInset = panelBottom,
        actionBottomInset = actionBottom,
        liftAction = false,
        actionDiameter = diameter,
        dismissAreaHeight = (size.height - panelBottom + gap).coerceAtLeast(0f),
    )
}
