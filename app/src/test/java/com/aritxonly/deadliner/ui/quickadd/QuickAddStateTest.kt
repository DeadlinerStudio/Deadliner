package com.aritxonly.deadliner.ui.quickadd

import com.aritxonly.deadliner.model.CalendarEvent
import androidx.compose.runtime.saveable.SaverScope
import com.deadliner.domain.quickadd.QuickAddProposal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class QuickAddStateTest {
    @Test
    fun readyModelBlocksRuleSubmissionWhileRecognizingAndAfterRejection() = kotlinx.coroutines.runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var aiCalls = 0
            val state = QuickAddState(scope) { _, _ -> aiCalls++; QuickAddProposal() }
            val result = CompletableDeferred<QuickAddProposal>()
            state.modelAvailable = { true }
            state.modelRecognizer = { _, _ -> result.await() }
            state.open("TASK")
            state.edit("明天下午3点交报告")
            assertFalse(state.canSubmit)
            assertNull(state.proposal.dueAt)
            result.complete(com.deadliner.domain.quickadd.model.QuickAddRecognitionPolicy().rejected("明天下午3点交报告", "TASK"))
            kotlinx.coroutines.delay(150)
            assertFalse(state.canSubmit)
            assertEquals("REJECTED", state.proposal.recognitionState)
            assertNull(state.proposal.dueAt)
            assertEquals(0, aiCalls)
        } finally { scope.cancel() }
    }

    @Test
    fun aiConfirmationAndEmptyResultsProvideFeedbackAndKeepTheDraft() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var empty = false
            val state = QuickAddState(scope) { _, _ ->
                if (empty) QuickAddProposal(source = "AI", message = "未识别出任务")
                else QuickAddProposal(title = "交报告", ready = true, source = "AI")
            }
            state.open("TASK")
            state.edit("交报告")
            state.recognizeWithAI()
            assertEquals("AI 已确认草案", state.message)
            assertEquals("AI", state.proposal.source)
            empty = true
            state.recognizeWithAI()
            assertEquals("交报告", state.proposal.title)
            assertEquals("未识别出任务", state.message)
            assertTrue(state.canSubmit)
        } finally { scope.cancel() }
    }

    @Test
    fun missingApiKeyIsVisibleAndDoesNotEraseTheDraft() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope) { _, _ -> throw com.deadliner.domain.ai.error.AIError.MissingAPIKey }
            state.open("TASK")
            state.edit("交报告")
            state.recognizeWithAI()
            assertTrue(state.message.contains("API Key"))
            assertEquals("交报告", state.proposal.title)
            assertFalse(state.busy)
        } finally { scope.cancel() }
    }

    @Test
    fun closingAndReopeningPreservesTheDraftAndSendNeverInvokesAI() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var requests = 0
            val state = QuickAddState(scope) { _, _ -> requests++; QuickAddProposal() }
            state.open("TASK")
            state.edit("123")
            assertTrue(state.canClose)
            assertFalse(state.canSubmit)
            state.submit { fail("Unrecognized input cannot save") }
            assertEquals(0, requests)
            state.edit("明天下午3点交报告")
            state.categoryUid = "work"
            val due = state.explicitDueAt
            state.dismiss()
            assertFalse(state.visible)
            state.open("TASK")
            assertEquals("明天下午3点交报告", state.text)
            assertEquals(due, state.explicitDueAt)
            assertEquals("work", state.categoryUid)
            assertTrue(state.canSubmit)
        } finally { scope.cancel() }
    }

    @Test
    fun recognizedTimesCanBeRemovedWithoutReturningFromTheProposal() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            assertNull(state.explicitStartAt)
            assertNull(state.explicitDueAt)
            state.edit("从明天上午9点到明天下午5点开会")
            assertTrue(state.proposal.ready)
            assertEquals("开会", state.proposal.title)
            assertEquals(9, state.explicitStartAt!!.hour)
            assertEquals(17, LocalDateTime.parse(state.explicitDueAt).hour)
            state.removeDue()
            assertNull(state.explicitDueAt)
            assertEquals(state.explicitStartAt!!.plusHours(1).toString(), state.effectiveProposal.dueAt)
            state.removeStart()
            assertNull(state.explicitStartAt)
            state.edit("明天上午9点开始开会")
            assertEquals(9, state.explicitStartAt!!.hour)
            assertNull(state.explicitDueAt)
            assertEquals(state.explicitStartAt!!.plusHours(1).toString(), state.effectiveProposal.dueAt)
        } finally { scope.cancel() }
    }

    @Test
    fun explicitStartAndDeletedDueSurviveRestoration() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            state.edit("从明天上午9点到明天下午5点开会")
            state.removeDue()
            val saver = QuickAddState.saver(scope)
            val saved = with(saver) {
                with(object : SaverScope { override fun canBeSaved(value: Any) = true }) { save(state) }
            }
            val restored = saver.restore(saved!!)!!
            assertEquals(state.explicitStartAt, restored.explicitStartAt)
            assertNull(restored.explicitDueAt)
            assertEquals(state.effectiveProposal.dueAt, restored.effectiveProposal.dueAt)
        } finally { scope.cancel() }
    }

    @Test
    fun recognizedDraftCanBeReinterpretedWithoutSaving() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var requests = 0
            val state = QuickAddState(scope) { text, kind ->
                assertEquals("交报告", text)
                assertEquals("TASK", kind)
                requests++
                QuickAddProposal(title = "交最终报告", note = "AI 补充", ready = true, source = "AI")
            }
            state.open("TASK")
            state.edit("交报告")
            assertTrue(state.canReinterpret)
            state.recognizeWithAI()
            assertEquals(1, requests)
            assertEquals("交最终报告", state.proposal.title)
            assertTrue(state.visible)
            assertFalse(state.busy)
            assertFalse(state.recognizingAI)
            state.setNote("")
            assertEquals("", state.proposal.note)
            state.edit("每天晚上8点读书")
            state.setReminder(null)
            assertNull(state.proposal.reminderTime)
            assertTrue(state.setSchedule("每周 3次"))
            assertEquals("WEEKLY", state.proposal.period)
            assertEquals(3, state.proposal.targetCount)
            state.removeSchedule()
            assertEquals("TASK", state.proposal.kind)
        } finally { scope.cancel() }
    }

    @Test
    fun editingCancelsOptionalAIRecognition() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val pending = CompletableDeferred<QuickAddProposal>()
            val state = QuickAddState(scope) { _, _ -> pending.await() }
            state.open("TASK")
            state.edit("交报告")
            state.recognizeWithAI()
            assertTrue(state.recognizingAI)
            state.edit("买牛奶")
            pending.complete(QuickAddProposal(title = "过期草案", ready = true, source = "AI"))
            assertEquals("买牛奶", state.proposal.title)
            assertFalse(state.busy)
            assertFalse(state.recognizingAI)
        } finally { scope.cancel() }
    }

    @Test
    fun blankInputHasDefaultTimesAndOnlyRecognizedTasksCanEditThem() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            val start = state.effectiveStartAt
            assertEquals(start.plusHours(1).toString(), state.effectiveProposal.dueAt)
            assertFalse(state.canEditTaskTimes)
            state.edit("123")
            assertTrue(state.needsAI)
            assertFalse(state.canSubmit)
            assertTrue(state.canRecognize)
            assertFalse(state.canEditTaskTimes)
            state.edit("交报告")
            assertTrue(state.canEditTaskTimes)
            assertEquals(start.plusHours(1).toString(), state.effectiveProposal.dueAt)
            state.edit("明天下午3点交报告")
            assertEquals(state.proposal.dueAt, state.effectiveProposal.dueAt)
            state.edit("每天读书")
            assertFalse(state.canEditTaskTimes)
            assertNull(state.effectiveProposal.dueAt)
        } finally { scope.cancel() }
    }

    @Test
    fun defaultTimesSurviveStateRestoration() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val original = QuickAddState(scope)
            original.open("TASK")
            original.edit("交报告")
            val saver = QuickAddState.saver(scope)
            val saved = with(saver) {
                with(object : SaverScope { override fun canBeSaved(value: Any) = true }) { save(original) }
            }
            val restored = saver.restore(saved!!)!!
            assertEquals(original.effectiveStartAt, restored.effectiveStartAt)
            assertEquals(original.effectiveProposal.dueAt, restored.effectiveProposal.dueAt)
            assertTrue(restored.canEditTaskTimes)
        } finally { scope.cancel() }
    }

    @Test
    fun incompleteInputShowsAIActionEvenWithoutALocalTitle() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            state.edit("明天")
            assertFalse(state.proposal.ready)
            assertTrue(state.needsAI)
            assertFalse(state.canSubmit)
            assertTrue(state.canRecognize)
            state.edit("")
            assertFalse(state.needsAI)
            assertFalse(state.canSubmit)
        } finally { scope.cancel() }
    }

    @Test
    fun editingImportedEventDropsCalendarLinkAndImportedDates() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            state.importEvent(CalendarEvent(17, "会议", 1_800_000_000_000, 1_800_003_600_000, "议程", null))
            assertEquals(17L, state.calendarEventId)
            assertNotNull(state.startAt)
            assertNotNull(state.dueAt)
            state.edit("明天下午3点交报告")
            assertNull(state.calendarEventId)
            assertNull(state.startAt)
            assertNull(state.dueAt)
            assertTrue(state.proposal.ready)
            assertEquals("交报告", state.proposal.title)
        } finally { scope.cancel() }
    }

    @Test
    fun manualPropertiesSurviveInputEditsAndReopening() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            state.edit("交报告")
            val due = LocalDateTime.of(2030, 4, 3, 15, 0)
            state.dueAt = due
            state.categoryUid = "work"
            state.edit("交最终报告")
            state.dismiss()
            state.open("TASK")
            assertEquals(due.toString(), state.effectiveProposal.dueAt)
            assertEquals("work", state.categoryUid)
            assertEquals("交最终报告", state.effectiveProposal.title)
        } finally { scope.cancel() }
    }

    @Test
    fun invalidTimeRangeDoesNotSaveOrDiscardDraft() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            state.edit("交报告")
            state.dueAt = LocalDateTime.of(2030, 4, 3, 15, 0)
            state.startAt = state.dueAt!!.plusHours(1)
            var saved = false
            state.submit { saved = true }
            assertFalse(saved)
            assertTrue(state.visible)
            assertFalse(state.busy)
            assertEquals("交报告", state.text)
            assertEquals("截止时间不能早于开始时间", state.message)
        } finally { scope.cancel() }
    }

    @Test
    fun lateModelResultCannotOverwriteNewInputOrManualFields() = kotlinx.coroutines.runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val pending = CompletableDeferred<QuickAddProposal>()
        try {
            val state = QuickAddState(scope)
            state.modelRecognizer = { _, _ -> kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { pending.await() } }
            state.open("TASK")
            state.edit("交报告")
            kotlinx.coroutines.delay(120)
            state.edit("买牛奶")
            state.setNote("用户备注")
            pending.complete(QuickAddProposal(title = "旧模型结果", ready = true, source = "MODEL"))
            kotlinx.coroutines.delay(150)
            assertEquals("买牛奶", state.proposal.title)
            assertEquals("用户备注", state.proposal.note)
            assertEquals("LOCAL", state.proposal.source)
        } finally { pending.complete(QuickAddProposal()); scope.cancel() }
    }

    @Test
    fun readyModelUpdatesAutomaticallyAndUnavailableModelKeepsRules() = kotlinx.coroutines.runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.modelRecognizer = { _, _ -> null }
            state.open("TASK")
            state.edit("明天交报告")
            assertEquals("交报告", state.proposal.title) // Rule preview does not wait.
            kotlinx.coroutines.delay(120)
            assertEquals("LOCAL", state.proposal.source)
            state.modelRecognizer = { _, _ -> QuickAddProposal(title = "模型报告", ready = true, source = "MODEL") }
            state.refreshWithModel()
            kotlinx.coroutines.delay(120)
            assertEquals("MODEL", state.proposal.source)
            assertEquals("模型报告", state.proposal.title)
        } finally { scope.cancel() }
    }

    @Test
    fun closingOrStartingAiPreventsPendingModelFromOverwritingTheDraft() = kotlinx.coroutines.runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val pending = CompletableDeferred<QuickAddProposal>()
        try {
            val state = QuickAddState(scope) { _, _ -> QuickAddProposal(title = "AI 报告", ready = true, source = "AI") }
            state.modelRecognizer = { _, _ -> kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { pending.await() } }
            state.open("TASK"); state.edit("交报告")
            kotlinx.coroutines.delay(120)
            state.recognizeWithAI()
            pending.complete(QuickAddProposal(title = "旧模型结果", ready = true, source = "MODEL"))
            kotlinx.coroutines.delay(20)
            assertEquals("AI 报告", state.proposal.title)
            state.dismiss()
            state.refreshWithModel()
            kotlinx.coroutines.delay(120)
            assertEquals("AI", state.proposal.source)
            assertFalse(state.visible)
        } finally { pending.complete(QuickAddProposal()); scope.cancel() }
    }
}
