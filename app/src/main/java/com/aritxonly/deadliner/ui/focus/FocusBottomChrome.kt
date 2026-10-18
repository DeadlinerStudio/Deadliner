package com.aritxonly.deadliner.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.deadliner.ui.base.FloatingNavigationBottomSpacing
import com.aritxonly.deadliner.ui.base.FloatingNavigationEdgeMask

/** Same navigation baseline as home; content clearance never becomes the button anchor. */
internal val FocusActionBottomSpacing get() = FloatingNavigationBottomSpacing

/** Reuse the exact home surface alpha gradient, including coverage height and system insets. */
@Composable
internal fun FocusBottomChrome(modifier: Modifier = Modifier) {
    FloatingNavigationEdgeMask(modifier)
}
