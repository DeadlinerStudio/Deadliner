package com.aritxonly.deadliner.ui.lifi

import com.aritxonly.deadliner.ai.ToolDecision
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LifiApprovalTest {
    @Test fun immediateReceiptCannotBeOverwrittenByApproval() = runBlocking {
        val deferred = CompletableDeferred<ToolDecision>()
        var state = LifiActionState.PENDING
        val receipt = launch(Dispatchers.Unconfined) {
            deferred.await()
            assertEquals(LifiActionState.EXECUTING, state)
            state = LifiActionState.FAILED
        }
        assertTrue(completeLifiApproval(deferred, ToolDecision.Approve) { state = LifiActionState.EXECUTING })
        receipt.join()
        assertEquals(LifiActionState.FAILED, state)
        assertFalse(completeLifiApproval(deferred, ToolDecision.Approve) { state = LifiActionState.EXECUTING })
        assertEquals(LifiActionState.FAILED, state)
    }
}
