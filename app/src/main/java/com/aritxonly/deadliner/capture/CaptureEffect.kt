package com.aritxonly.deadliner.capture

import androidx.annotation.StringRes
import com.aritxonly.deadliner.ai.GeneratedDDL

sealed interface CaptureEffect {
    data class ToastRes(@param:StringRes val resId: Int) : CaptureEffect
    data class OpenTaskEditor(
        val generated: GeneratedDDL?,
        val sourceText: String,
        val autoRunAi: Boolean = false
    ) : CaptureEffect
    data class OpenHabitEditor(
        val sourceText: String,
        val autoRunAi: Boolean = false
    ) : CaptureEffect
}
