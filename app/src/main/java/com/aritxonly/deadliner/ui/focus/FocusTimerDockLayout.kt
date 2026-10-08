package com.aritxonly.deadliner.ui.focus

import androidx.compose.ui.unit.dp

/**
 * Geometry from MyHyperModifier 4af554b, SpotifyPlaybackCapsule / SpotifyFloatingNavigation:
 * the player is 50dp high, follows the dock's horizontal bounds, and sits 6dp above it.
 * Deadliner's dock adds 8dp inside its 442dp host; both rows must use that same inset.
 */
object FocusTimerDockLayout {
    val Height = 50.dp
    val Gap = 6.dp
    val ContentClearance = Height + Gap
}
