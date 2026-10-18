package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal object GlassDropdownGeometry {
    fun position(anchor: Rect, safe: Rect, menu: IntSize, rtl: Boolean, gap: Float): IntOffset {
        // The panel covers its source value, with matching trailing content padding.
        val x = if (rtl) anchor.left - gap else anchor.right - menu.width + gap
        val y = anchor.bottom + gap - menu.height
        return IntOffset(
            x.coerceIn(safe.left, maxOf(safe.left, safe.right - menu.width)).roundToInt(),
            y.coerceIn(safe.top, maxOf(safe.top, safe.bottom - menu.height)).roundToInt(),
        )
    }

}
