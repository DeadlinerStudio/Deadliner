package com.aritxonly.deadliner.notification

import androidx.annotation.DrawableRes
import com.aritxonly.deadliner.R
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.platform.NotificationPhase
import com.deadliner.service.focus.FocusTimerResult

/** Miuix Medium glyphs exported as framework vectors for SystemUI, separate from app identity. */
internal object IslandStateIcons {
    @DrawableRes fun realtime(phase: NotificationPhase?): Int = when (phase) {
        NotificationPhase.COMPLETED, NotificationPhase.HABIT_CHECKED_IN, NotificationPhase.FOCUS_FINISHED -> R.drawable.ic_island_miuix_topdownloads
        NotificationPhase.ABANDONED -> R.drawable.ic_island_miuix_recent_abandoned
        NotificationPhase.HABIT_DUE -> R.drawable.ic_island_miuix_tasks
        else -> R.drawable.ic_island_miuix_alarm
    }
    @DrawableRes fun focus(info: FocusTimerResult): Int {
        val snapshot = requireNotNull(info.snapshot)
        return when {
            !FocusRealtimeNotificationRenderer.running(snapshot) -> R.drawable.ic_island_miuix_pause
            snapshot.mode == FocusTimerMode.COUNTDOWN -> R.drawable.ic_island_miuix_timer
            else -> R.drawable.ic_island_miuix_stopwatch
        }
    }
}
