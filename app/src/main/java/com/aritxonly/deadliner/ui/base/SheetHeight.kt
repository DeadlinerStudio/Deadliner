package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** FULL leaves an 8dp gap below the top system safe area; MEDIUM retains the 600dp body. */
enum class SheetHeight { MEDIUM, FULL }

internal fun SheetHeight.topInset(systemTop: Dp): Dp =
    systemTop.coerceAtLeast(0.dp) + if (this == SheetHeight.FULL) 8.dp else 0.dp

internal fun SheetHeight.bodyHeight(windowHeight: Dp, systemTop: Dp): Dp {
    // The overlay's existing handle strip consumes another 8dp above the Scaffold.
    val available = (windowHeight - topInset(systemTop) - 8.dp).coerceAtLeast(0.dp)
    return if (this == SheetHeight.FULL) available else minOf(600.dp, available)
}
