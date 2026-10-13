package com.aritxonly.deadliner.ui.focus

import com.aritxonly.deadliner.R
import com.deadliner.domain.focus.FocusTimerWarning
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusTimerMessagesTest {
    @Test fun warningsHaveDistinctAndCorrectMessages() {
        assertEquals(R.string.focus_clock_warning, focusWarningResource(FocusTimerWarning.CLOCK_ANOMALY))
        assertEquals(R.string.focus_long_running_warning, focusWarningResource(FocusTimerWarning.LONG_RUNNING))
        assertEquals(R.string.focus_excessive_warning, focusWarningResource(FocusTimerWarning.EXCESSIVE_DURATION))
        assertEquals(R.string.focus_alarm_fallback_warning, focusWarningResource(FocusTimerWarning.ALARM_UNAVAILABLE))
        assertEquals(4, FocusTimerWarning.entries.map(::focusWarningResource).toSet().size)
    }
}
