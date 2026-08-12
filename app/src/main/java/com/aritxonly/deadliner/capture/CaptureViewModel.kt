package com.aritxonly.deadliner.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** Android lifecycle/navigation wrapper; all Capture state and data intents live in KMP. */
class CaptureViewModel : ViewModel() {
    private val shared = KmpV6Runtime.captureViewModel
    val uiState = shared.uiState

    private val _effects = MutableSharedFlow<CaptureEffect>()
    val effects: SharedFlow<CaptureEffect> = _effects.asSharedFlow()

    init {
        shared.initialize()
    }

    fun updateDraft(text: String) = shared.updateDraft(text)
    fun updateQuery(text: String) = shared.updateQuery(text)
    fun saveDraft() = shared.saveDraft()
    fun openDetail(uid: String) = shared.openDetail(uid)
    fun closeDetail() = shared.closeDetail()
    fun updateEditingText(text: String) = shared.updateEditingText(text)
    fun saveEditing() = shared.saveEditing()
    fun deleteItem(uid: String) = shared.deleteCapture(uid)
    fun toggleMultiSelect() = shared.enterMultiSelect()
    fun exitMultiSelect() = shared.exitMultiSelect()
    fun toggleSelect(uid: String) = shared.toggleSelection(uid)
    fun deleteSelected() = shared.deleteSelected()

    fun convertCurrentEditingToTask(useAi: Boolean) =
        convertTextToTask(shared.uiState.value.editingText.trim(), useAi)

    fun convertCurrentEditingToHabit(useAi: Boolean) =
        convertTextToHabit(shared.uiState.value.editingText.trim(), useAi)

    fun convertItemToTask(uid: String, useAi: Boolean) =
        convertTextToTask(shared.uiState.value.items.firstOrNull { it.uid == uid }?.text.orEmpty(), useAi)

    fun convertItemToHabit(uid: String, useAi: Boolean) =
        convertTextToHabit(shared.uiState.value.items.firstOrNull { it.uid == uid }?.text.orEmpty(), useAi)

    fun convertSelectedToTask(useAi: Boolean) {
        convertTextToTask(selectedConversionText(), useAi)
    }

    fun convertSelectedToHabit(useAi: Boolean) {
        convertTextToHabit(selectedConversionText(), useAi)
    }

    private fun selectedConversionText(): String {
        val state = shared.uiState.value
        val text = if (state.selectedUids.size > 1) shared.mergeSelected() else shared.selectedText()
        if (state.selectedUids.size <= 1) shared.exitMultiSelect()
        return text.orEmpty()
    }

    private fun convertTextToTask(text: String, useAi: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _effects.emit(CaptureEffect.OpenTaskEditor(null, text, useAi))
        }
    }

    private fun convertTextToHabit(text: String, useAi: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch {
            if (useAi) _effects.emit(CaptureEffect.ToastRes(R.string.capture_ai_habit_toast))
            _effects.emit(CaptureEffect.OpenHabitEditor(text, useAi))
        }
    }
}
