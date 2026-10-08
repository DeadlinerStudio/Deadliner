package com.aritxonly.deadliner.ui.focus

import com.deadliner.domain.focus.FocusTimerMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusTimerDialTest {
    @Test fun countdownShowsElapsedShareAndStopsAtOneFullTurn() {
        assertEquals(0.5f, focusDialTurns(FocusTimerMode.COUNTDOWN, 750f, 1500L), 0.001f)
        assertEquals(1f, focusDialTurns(FocusTimerMode.COUNTDOWN, 1510f, 1500L), 0.001f)
        assertEquals(0f, focusDialTurns(FocusTimerMode.COUNTDOWN, -10f, 1500L), 0.001f)
    }

    @Test fun stopwatchClockDoesNotAnimateBackwardsAtMinuteBoundary() {
        val before = focusDialTurns(FocusTimerMode.STOPWATCH, 59f, null)
        val after = focusDialTurns(FocusTimerMode.STOPWATCH, 61f, null)
        assertEquals(2f / 60f, after - before, 0.001f)
    }

    @Test fun timerClockHandlesHoursAndClampsNegativeTime() {
        assertEquals("00:00", formatFocusClock(-1))
        assertEquals("00:59", formatFocusClock(59))
        assertEquals("01:00:01", formatFocusClock(3601))
    }
}
