package com.aritxonly.deadliner.ai

import com.deadliner.domain.ai.agent.AgentV2Snapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-owned authoritative state survives overlay subscriptions. */
internal object LifiV2Session {
    private val mutableSnapshot = MutableStateFlow<AgentV2Snapshot?>(null)
    val snapshot = mutableSnapshot.asStateFlow()
    fun update(snapshot: AgentV2Snapshot) { mutableSnapshot.value = snapshot }
}
