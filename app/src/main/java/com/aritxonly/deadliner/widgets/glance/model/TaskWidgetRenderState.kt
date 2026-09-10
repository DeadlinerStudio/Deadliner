package com.aritxonly.deadliner.widgets.glance.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class TaskWidgetRenderState(
    val snapshot: TaskWidgetSnapshot? = null,
    val revision: Long = 0L,
)

/** Observable input for Glance compositions that can outlive a single update request. */
internal class TaskWidgetRenderStateStore {
    private val mutableState = MutableStateFlow(TaskWidgetRenderState())

    val state: StateFlow<TaskWidgetRenderState> = mutableState.asStateFlow()

    @Synchronized
    fun publish(snapshot: TaskWidgetSnapshot, forceRevision: Boolean = false): Boolean {
        val current = mutableState.value
        if (!forceRevision && current.snapshot == snapshot) return false
        mutableState.value = TaskWidgetRenderState(
            snapshot = snapshot,
            revision = current.revision + 1L,
        )
        return true
    }

    @Synchronized
    fun invalidateConfiguration() {
        val current = mutableState.value
        mutableState.value = current.copy(revision = current.revision + 1L)
    }
}

internal object TaskWidgetRenderStateSource {
    val store = TaskWidgetRenderStateStore()
}
