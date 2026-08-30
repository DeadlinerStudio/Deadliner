package com.aritxonly.deadliner.ui.main.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import com.aritxonly.deadliner.model.DeadlineType

enum class MainSection {
    LIST,
    OVERVIEW,
    CAPTURE,
}

@Stable
class MainHostState(
    initialPage: DeadlineType,
) {
    var pendingUrl: String? by mutableStateOf(null)
    var selectedSection: MainSection by mutableStateOf(MainSection.LIST)
    var selectedPage: DeadlineType by mutableStateOf(initialPage)
    var showOverlay: Boolean by mutableStateOf(false)
    var selectionMode: Boolean by mutableStateOf(false)

    val selectedIds: SnapshotStateList<Long> = mutableStateListOf()

    fun enterSelection(id: Long) {
        selectionMode = true
        if (!selectedIds.contains(id)) selectedIds.add(id)
    }

    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        if (selectedIds.isEmpty()) selectionMode = false
    }

    fun clearSelection() {
        selectedIds.clear()
        selectionMode = false
    }
}

@Composable
fun rememberMainHostState(
    initialPage: DeadlineType,
): MainHostState = remember { MainHostState(initialPage = initialPage) }

