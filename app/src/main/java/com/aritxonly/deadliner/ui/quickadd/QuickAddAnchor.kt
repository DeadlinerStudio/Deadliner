package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned

/** Use layout dimensions: press feedback and icon rotation must not shrink the source. */
internal fun Modifier.quickAddAnchor(state: QuickAddState): Modifier = onGloballyPositioned {
    val size = Size(it.size.width.toFloat(), it.size.height.toFloat())
    state.anchor = quickAddAnchorRect(it.localToWindow(Offset(size.width / 2f, size.height / 2f)), size)
}

internal fun quickAddAnchorRect(center: Offset, size: Size): Rect =
    Rect(center.x - size.width / 2f, center.y - size.height / 2f,
        center.x + size.width / 2f, center.y + size.height / 2f)
