package com.aritxonly.deadliner.capture

import com.aritxonly.deadliner.capture.model.InspirationItem

data class CaptureUiState(
    val draftText: String = "",
    val query: String = "",
    val items: List<InspirationItem> = emptyList(),
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val editingItemId: Long? = null,
    val editingText: String = "",
    val loading: Boolean = false
) {
    val filteredItems: List<InspirationItem>
        get() = if (query.isBlank()) items else items.filter { it.text.contains(query, ignoreCase = true) }
}
