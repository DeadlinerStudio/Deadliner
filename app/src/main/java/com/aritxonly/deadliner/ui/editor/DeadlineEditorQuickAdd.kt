package com.aritxonly.deadliner.ui.editor

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.aritxonly.deadliner.ui.quickadd.QuickAddEditorEntry
import com.aritxonly.deadliner.ui.quickadd.QuickAddState
import com.aritxonly.deadliner.ui.quickadd.rememberQuickAddState

/** One recognition pipeline, retained outside the lazy form so scrolling never resets it. */
@Composable
internal fun DeadlineEditorState.rememberEditorQuickAdd(): QuickAddState {
    val quick = rememberQuickAddState()
    var lastTaskApplied by rememberSaveable { mutableStateOf<String?>(null) }
    var lastHabitApplied by rememberSaveable { mutableStateOf<String?>(null) }
    SideEffect { quickAddState = quick }
    LaunchedEffect(quick) {
        if (!quick.visible) {
            if (!QuickAddEditorEntry.restoreDraft(quick, intent)) {
                quick.open(if (selectedPage == 1) "HABIT" else "TASK")
                if (aiInputText.isNotBlank()) quick.edit(aiInputText)
                quick.categoryUid = if (selectedPage == 1) habitDraft.categoryUid else taskDraft.categoryUid
            }
        }
        if (autoRunAiOnAppear && !aiAutoTriggered && quick.text.isNotBlank()) {
            aiAutoTriggered = true
            autoRunAiOnAppear = false
            quick.recognizeWithAI()
        }
    }
    LaunchedEffect(quick.text) { if (quick.visible) aiInputText = quick.text }
    LaunchedEffect(selectedPage) {
        quick.open(if (selectedPage == 1) "HABIT" else "TASK")
    }
    val suggestion = quick.editorSuggestion()
    LaunchedEffect(suggestion) {
        if (suggestion != null) {
            val habit = suggestion.proposal.kind == "HABIT"
            val lastApplied = if (habit) lastHabitApplied else lastTaskApplied
            if (suggestion.key() == lastApplied) return@LaunchedEffect
            val compatible = when {
                editingTaskUid != null -> !habit
                editingHabitUid != null -> habit
                else -> true
            }
            if (compatible) {
                if (habit) habitDraft = suggestion.mergeHabit(habitDraft)
                else taskDraft = suggestion.mergeTask(taskDraft)
                if (editingTaskUid == null && editingHabitUid == null) selectedPage = if (habit) 1 else 0
            }
            if (habit) lastHabitApplied = suggestion.key() else lastTaskApplied = suggestion.key()
        }
    }
    DisposableEffect(quick) { onDispose { quick.dismiss() } }
    return quick
}
