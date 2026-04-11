package com.aritxonly.deadliner.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.data.CaptureRepository
import com.aritxonly.deadliner.capture.model.InspirationItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class CaptureViewModel(
    app: Application
) : AndroidViewModel(app) {
    private val repo = CaptureRepository(app)
    private val _uiState = MutableStateFlow(CaptureUiState(items = repo.load()))
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CaptureEffect>()
    val effects: SharedFlow<CaptureEffect> = _effects.asSharedFlow()

    fun updateDraft(text: String) {
        _uiState.value = _uiState.value.copy(draftText = text)
    }

    fun updateQuery(text: String) {
        _uiState.value = _uiState.value.copy(query = text)
    }

    fun saveDraft() {
        val text = _uiState.value.draftText.trim()
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        val newItem = InspirationItem(
            id = now + Random.nextLong(1000L, 99999L),
            text = text,
            createdAt = now,
            updatedAt = now
        )
        val newItems = listOf(newItem) + _uiState.value.items
        repo.save(newItems)
        _uiState.value = _uiState.value.copy(draftText = "", items = newItems)
    }

    fun openDetail(itemId: Long) {
        val item = _uiState.value.items.find { it.id == itemId } ?: return
        _uiState.value = _uiState.value.copy(editingItemId = item.id, editingText = item.text)
    }

    fun closeDetail() {
        _uiState.value = _uiState.value.copy(editingItemId = null, editingText = "")
    }

    fun updateEditingText(text: String) {
        _uiState.value = _uiState.value.copy(editingText = text)
    }

    fun saveEditing() {
        val state = _uiState.value
        val id = state.editingItemId ?: return
        val text = state.editingText.trim()
        if (text.isEmpty()) return
        val updated = state.items.map {
            if (it.id == id) it.copy(text = text, updatedAt = System.currentTimeMillis()) else it
        }.sortedByDescending { it.updatedAt }
        repo.save(updated)
        _uiState.value = state.copy(items = updated)
    }

    fun deleteItem(itemId: Long) {
        val state = _uiState.value
        val updated = state.items.filterNot { it.id == itemId }
        repo.save(updated)
        _uiState.value = state.copy(
            items = updated,
            selectedIds = state.selectedIds - itemId,
            editingItemId = if (state.editingItemId == itemId) null else state.editingItemId,
            editingText = if (state.editingItemId == itemId) "" else state.editingText
        )
    }

    fun toggleMultiSelect() {
        _uiState.value = _uiState.value.copy(isMultiSelectMode = true)
    }

    fun exitMultiSelect() {
        _uiState.value = _uiState.value.copy(isMultiSelectMode = false, selectedIds = emptySet())
    }

    fun toggleSelect(itemId: Long) {
        val state = _uiState.value
        val newSet = state.selectedIds.toMutableSet()
        if (!newSet.add(itemId)) newSet.remove(itemId)
        _uiState.value = state.copy(selectedIds = newSet)
    }

    fun deleteSelected() {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        val updated = state.items.filterNot { state.selectedIds.contains(it.id) }
        repo.save(updated)
        _uiState.value = state.copy(
            items = updated,
            selectedIds = emptySet(),
            isMultiSelectMode = false
        )
    }

    private fun mergeTextOfSelected(): String {
        val state = _uiState.value
        val selected = state.items.filter { state.selectedIds.contains(it.id) }
            .sortedBy { it.createdAt }
        return selected.joinToString("\n\n---\n\n") { it.text.trim() }
    }

    private fun replaceSelectedByMerged(): InspirationItem? {
        val state = _uiState.value
        if (state.selectedIds.size < 2) return null
        val mergedText = mergeTextOfSelected().trim()
        if (mergedText.isEmpty()) return null
        val now = System.currentTimeMillis()
        val merged = InspirationItem(
            id = now + Random.nextLong(111L, 999L),
            text = mergedText,
            createdAt = now,
            updatedAt = now
        )
        val remaining = state.items.filterNot { state.selectedIds.contains(it.id) }
        val updated = (listOf(merged) + remaining).sortedByDescending { it.updatedAt }
        repo.save(updated)
        _uiState.value = state.copy(
            items = updated,
            selectedIds = emptySet(),
            isMultiSelectMode = false
        )
        return merged
    }

    fun convertCurrentEditingToTask(useAi: Boolean) {
        val state = _uiState.value
        val text = state.editingText.trim()
        if (text.isEmpty()) return
        convertTextToTask(text, useAi)
    }

    fun convertCurrentEditingToHabit(useAi: Boolean) {
        val state = _uiState.value
        val text = state.editingText.trim()
        if (text.isEmpty()) return
        convertTextToHabit(text, useAi)
    }

    fun convertItemToTask(itemId: Long, useAi: Boolean) {
        val text = _uiState.value.items.find { it.id == itemId }?.text?.trim().orEmpty()
        if (text.isEmpty()) return
        convertTextToTask(text, useAi)
    }

    fun convertItemToHabit(itemId: Long, useAi: Boolean) {
        val text = _uiState.value.items.find { it.id == itemId }?.text?.trim().orEmpty()
        if (text.isEmpty()) return
        convertTextToHabit(text, useAi)
    }

    fun mergeAndConvertToTask(useAi: Boolean) {
        val merged = replaceSelectedByMerged() ?: return
        convertTextToTask(merged.text, useAi)
    }

    fun mergeAndConvertToHabit(useAi: Boolean) {
        val merged = replaceSelectedByMerged() ?: return
        convertTextToHabit(merged.text, useAi)
    }

    private fun convertTextToTask(text: String, useAi: Boolean) {
        viewModelScope.launch {
            _effects.emit(
                CaptureEffect.OpenTaskEditor(
                    generated = null,
                    sourceText = text,
                    autoRunAi = useAi
                )
            )
        }
    }

    private fun convertTextToHabit(text: String, useAi: Boolean) {
        viewModelScope.launch {
            if (useAi) {
                _effects.emit(CaptureEffect.ToastRes(R.string.capture_ai_habit_toast))
            }
            _effects.emit(
                CaptureEffect.OpenHabitEditor(
                    sourceText = text,
                    autoRunAi = useAi
                )
            )
        }
    }
}
