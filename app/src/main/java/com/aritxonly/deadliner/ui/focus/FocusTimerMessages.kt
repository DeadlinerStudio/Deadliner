package com.aritxonly.deadliner.ui.focus

import com.aritxonly.deadliner.R
import com.deadliner.domain.focus.FocusTimerWarning

internal fun focusWarningResource(warning: FocusTimerWarning): Int = when (warning) {
    FocusTimerWarning.CLOCK_ANOMALY -> R.string.focus_clock_warning
    FocusTimerWarning.LONG_RUNNING -> R.string.focus_long_running_warning
    FocusTimerWarning.EXCESSIVE_DURATION -> R.string.focus_excessive_warning
    FocusTimerWarning.ALARM_UNAVAILABLE -> R.string.focus_alarm_fallback_warning
}
