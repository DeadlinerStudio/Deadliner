package com.aritxonly.deadliner.ui.agent

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/** The overlay subscribes to retained state; dismissing it never cancels a run. */
internal object AIAgentSessionState {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val loading = mutableStateOf(false)
    val runJob = mutableStateOf<Job?>(null)
    val messages = mutableStateListOf<ChatDisplayItem>()
    val pendingApproval = mutableStateOf<PendingToolApproval?>(null)
    val streamBubbleId = mutableStateOf<String?>(null)
}
