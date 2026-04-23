package com.aritxonly.deadliner.ui.main.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class MainHostUiState {
    var toolbarExpanded by mutableStateOf(true)
    var showOverlay by mutableStateOf(false)
    var moreExpanded by mutableStateOf(false)
    var childRequestsBlur by mutableStateOf(false)
}

@Composable
fun rememberMainHostUiState(): MainHostUiState = remember { MainHostUiState() }
