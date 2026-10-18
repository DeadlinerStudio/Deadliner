package com.aritxonly.deadliner.ui.lifi

import org.junit.Assert.*
import org.junit.Test

class LifiModelsTest {
    @Test fun editingHabitPreservesScheduleReminderAndFutureFields() {
        val source = """{"name":"阅读","schedule":{"kind":"WEEKLY_DAYS","weekdaysMask":127},"goal":{"window":"WEEK","targetCount":3},"tracking":{"mode":"QUANTITY","unit":"页"},"reminder":{"time":"20:00"},"future":{"keep":true}}"""
        val edited = lifiArguments(lifiEditedArguments("create_habit", source, "练习阅读", "", "", "5"))
        val original = lifiArguments(source)
        listOf("schedule", "tracking", "reminder", "future").forEach { assertEquals(original[it], edited[it]) }
        assertEquals("WEEK", edited.getAsJsonObject("goal").label("window"))
        assertEquals("5", edited.getAsJsonObject("goal").label("targetCount"))
    }
    @Test fun taskTimeUsesStrictCalendarValidation() {
        try { lifiEditedArguments("create_task", "{}", "会议", "2026-02-30 15:00", "", ""); fail("Invalid date must not be submitted") }
        catch (_: IllegalArgumentException) { }
        val edited = lifiArguments(lifiEditedArguments("create_task", """{"extra":42}""", "会议", "2026-10-13 15:00", "内容", ""))
        assertEquals("42", edited.label("extra")); assertEquals("内容", edited.label("note"))
    }
    @Test fun updateCannotChangeTheApprovedTargetId() {
        val result = lifiArguments(lifiEditedArguments("update_deadline", """{"taskId":"original-id"}""", "", "2026-10-13 15:00", "", ""))
        assertEquals("original-id", result.label("taskId"))
    }
    @Test fun failedPartialReservedOrMalformedReceiptsNeverDisplaySuccess() {
        listOf("""{"ok":false,"partialSuccess":true}""", """{"errorCode":"TARGET_CHANGED"}""", """{"ok":true,"reserved":true}""", "not-json", "{}").forEach { assertEquals(LifiActionState.FAILED, lifiReceiptState(it)) }
        assertEquals(LifiActionState.SUCCEEDED, lifiReceiptState("""{"ok":true,"createdTasks":[{"id":"1"}]}"""))
        assertEquals(LifiActionState.SUCCEEDED, lifiReceiptState("""{"tasks":[],"summary":{"count":0}}"""))
        assertEquals(LifiActionState.REJECTED, lifiReceiptState("""{"denied":true}"""))
    }
    @Test fun processDeathExpiresApprovalEvenForAProposal() {
        listOf(LifiActionState.PENDING, LifiActionState.EXECUTING).forEach { state ->
            val restored = LifiMessage(kind = LifiKind.ACTION, tool = "create_task", state = state, proposal = true).restored()
            assertEquals(LifiActionState.EXPIRED, restored.state)
        }
        assertEquals(LifiActionState.SUCCEEDED, LifiMessage(kind = LifiKind.ACTION, state = LifiActionState.SUCCEEDED).restored().state)
    }
    @Test fun historyRoundTripKeepsArgumentsAndCannotRestoreLiveApproval() {
        val state = LifiSavedState(history = listOf(LifiHistory(title = "原会话", messages = listOf(LifiMessage(kind = LifiKind.ACTION, arguments = """{"name":"会议"}""")))))
        val gson = com.google.gson.Gson()
        val restored = gson.fromJson(gson.toJson(state), LifiSavedState::class.java)
        assertEquals(state.history.single().messages.single().arguments, restored.history.single().messages.single().arguments)
        assertEquals(LifiActionState.EXPIRED, restored.history.single().messages.single().restored().state)
    }
}
