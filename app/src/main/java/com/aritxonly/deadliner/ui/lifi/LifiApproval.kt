package com.aritxonly.deadliner.ui.lifi

import com.aritxonly.deadliner.ai.ToolDecision
import kotlinx.coroutines.CompletableDeferred

/** Publish UI state before completion can resume an immediate coroutine on this thread. */
internal fun completeLifiApproval(deferred: CompletableDeferred<ToolDecision>, decision: ToolDecision,
    beforeResume: () -> Unit): Boolean {
    if (deferred.isCompleted) return false
    beforeResume()
    return deferred.complete(decision)
}
