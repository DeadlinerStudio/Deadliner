package com.aritxonly.deadliner.ai

import com.deadliner.domain.ai.model.ToolCall
import com.deadliner.domain.habit.HabitGoal
import com.deadliner.domain.habit.HabitSchedule
import com.deadliner.domain.habit.HabitScheduleKind
import com.deadliner.domain.habit.HabitTracking
import com.deadliner.domain.habit.HabitTrackingMode
import com.deadliner.domain.habit.HabitWeekdays
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun createHabitToolKeepsSharedWeekdayAndTrackingContract() {
        val json = serializeToolArguments(
            ToolCall.CreateHabit(
                name = "背单词",
                schedule = HabitSchedule(
                    kind = HabitScheduleKind.WEEKLY_DAYS,
                    startOn = "2026-08-19",
                    weekdaysMask = HabitWeekdays.WEEKDAYS,
                ),
                goal = HabitGoal(targetCount = 20),
                tracking = HabitTracking(
                    mode = HabitTrackingMode.QUANTITY,
                    unit = "个单词",
                    defaultIncrement = 5,
                ),
            ),
        )
        val args = JsonParser.parseString(json).asJsonObject

        assertEquals("WEEKLY_DAYS", args.getAsJsonObject("schedule").get("kind").asString)
        assertEquals(HabitWeekdays.WEEKDAYS, args.getAsJsonObject("schedule").get("weekdaysMask").asInt)
        assertEquals(20, args.getAsJsonObject("goal").get("targetCount").asInt)
        assertEquals("QUANTITY", args.getAsJsonObject("tracking").get("mode").asString)
        assertFalse(args.has("period"))
    }
}
