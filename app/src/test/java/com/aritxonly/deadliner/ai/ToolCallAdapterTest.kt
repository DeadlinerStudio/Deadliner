package com.aritxonly.deadliner.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallAdapterTest {
    @Test
    fun persistedDataToolsAreDelegatedUnchangedToKmp() = runBlocking {
        var received: Pair<String, String>? = null
        val adapter = AndroidToolCallAdapter { tool, arguments ->
            received = tool to arguments
            """{"ok":true}"""
        }
        val result = adapter.execute(ToolCallExecution("call-1", "create_task", """{"name":"v6"}"""))

        assertEquals("create_task" to """{"name":"v6"}""", received)
        assertEquals("""{"ok":true}""", result.resultJson)
    }

    @Test
    fun calendarRemainsAnAndroidPlatformBoundary() = runBlocking {
        val adapter = AndroidToolCallAdapter { _, _ -> error("must not reach shared data tools") }
        val result = adapter.execute(ToolCallExecution("call-2", DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR, "{}"))

        assertTrue(result.resultJson.contains("pending_skill"))
    }
}
