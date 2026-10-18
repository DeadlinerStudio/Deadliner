package com.aritxonly.deadliner.ui.editor

import com.aritxonly.deadliner.ui.quickadd.QuickAddPersistence
import com.aritxonly.deadliner.ui.quickadd.QuickAddState
import com.deadliner.domain.quickadd.QuickAddProposal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

/** Recognition output only: form-owned category, calendar link and starred state never enter it. */
internal data class EditorQuickAddSuggestion(
    val text: String,
    val proposal: QuickAddProposal,
    val start: LocalDateTime?,
    val due: LocalDateTime?,
) {
    fun key(): String = Json.encodeToString(listOf(text, proposal.copy(source = "", message = "",
        recognitionState = "").toJson(), start?.toString().orEmpty(), due?.toString().orEmpty()))

    fun mergeTask(current: TaskEditorDraft): TaskEditorDraft {
        var startTime = start ?: current.startTime
        var endTime = due ?: current.endTime
        if (endTime.isBefore(startTime)) {
            when {
                start != null && due != null -> { startTime = current.startTime; endTime = current.endTime }
                due != null -> startTime = endTime.minusHours(1)
                else -> endTime = startTime.plusHours(1)
            }
        }
        return current.copy(name = proposal.title, note = proposal.note.ifBlank { current.note },
            startTime = startTime, endTime = endTime)
    }

    fun mergeHabit(current: HabitEditorDraft): HabitEditorDraft {
        val recognized = QuickAddPersistence.habitDraft(proposal, text)
        return current.copy(name = recognized.name, period = recognized.period, weekdaysMask = recognized.weekdaysMask,
            goalType = recognized.goalType, timesPerPeriod = recognized.timesPerPeriod, totalTarget = recognized.totalTarget,
            reminderEnabled = if (proposal.reminderTime != null) recognized.reminderEnabled else current.reminderEnabled,
            reminderTime = if (proposal.reminderTime != null) recognized.reminderTime else current.reminderTime)
    }
}

internal fun QuickAddState.editorSuggestion(): EditorQuickAddSuggestion? {
    val draft = effectiveProposal
    if (!canSubmit || draft.title.isBlank()) return null
    val normalized = draft.copy(source = "", message = "", recognitionState = "")
    return EditorQuickAddSuggestion(text, normalized, explicitStartAt,
        explicitDueAt?.let { runCatching { LocalDateTime.parse(it.replace(' ', 'T')) }.getOrNull() })
}
