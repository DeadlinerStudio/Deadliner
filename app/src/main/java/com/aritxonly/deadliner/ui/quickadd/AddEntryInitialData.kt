package com.aritxonly.deadliner.ui.quickadd

import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.ui.editor.TaskEditorDraft
import com.aritxonly.deadliner.ui.editor.HabitEditorDraft

/** Initial drafts shared by generated-entry and traditional editor routes. */
data class AddEntryInitialData(val task: TaskEditorDraft, val habit: HabitEditorDraft,
                               val aiText: String, val autoRunAI: Boolean, val calendarEventId: Long? = null) {
    companion object {
        fun create(generated: GeneratedDDL?, prefill: String?, autoRunAI: Boolean,
                   intent: android.content.Intent? = null): AddEntryInitialData {
            intent?.let { QuickAddEditorEntry.initialData(it)?.let { data -> return data } }
            var task = TaskEditorDraft()
            var habit = HabitEditorDraft()
            if (generated != null) {
                task = task.copy(name = generated.name, endTime = generated.dueTime, note = generated.note)
                habit = habit.copy(name = generated.name)
            }
            val input = prefill?.trim().orEmpty()
            return AddEntryInitialData(task, habit, input, autoRunAI && input.isNotEmpty())
        }
    }
}
