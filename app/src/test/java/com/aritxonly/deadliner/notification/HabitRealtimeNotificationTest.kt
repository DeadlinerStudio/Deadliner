package com.aritxonly.deadliner.notification

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.deadliner.domain.habit.*
import com.deadliner.domain.task.Task
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.platform.*
import com.deadliner.service.NotificationMetadataPlanner
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HabitRealtimeNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val at = "2026-10-08T12:00:00Z"
    private fun entry(window: HabitGoalWindow, records: List<HabitRecord> = emptyList()): NotificationMetadataEntry {
        val habit = Habit("habit-result", "每天阅读二十分钟坚持", schedule = HabitSchedule(HabitScheduleKind.WEEKLY_DAYS,
            "2026-10-01", weekdaysMask = HabitWeekdays.ALL), goal = HabitGoal(window, 3),
            reminder = HabitReminder("12:00"), createdAt = at)
        return NotificationMetadataPlanner.plan(emptyList(), listOf(habit), { records },
            NotificationPolicy(habitMode = HabitNotificationMode.REALTIME), now = Instant.parse("2026-10-08T12:31:00Z"),
            zone = TimeZone.UTC).realtime.single()
    }
    private fun params(entry: NotificationMetadataEntry): JSONObject = JSONObject(
        XiaomiIslandExtras.createRealtime(context, entry, "content").getString("miui.focus.param")!!).getJSONObject("param_v2")
    private fun area(params: JSONObject) = params.getJSONObject("param_island").getJSONObject("bigIslandArea")

    @Test fun readyHabitShowsPeriodAndEllipsizedNameInsteadOfTime() {
        listOf(HabitGoalWindow.SCHEDULED_DAY, HabitGoalWindow.WEEK, HabitGoalWindow.MONTH, HabitGoalWindow.LIFETIME).forEach { window ->
            val e = entry(window)
            val a = area(params(e))
            assertEquals(HabitIslandPresentation.period(context, e), a.getJSONObject("imageTextInfoLeft").getJSONObject("textInfo").getString("title"))
            val right = a.getJSONObject("imageTextInfoRight").getJSONObject("textInfo")
            assertEquals("每天阅读二十分…", right.getString("title"))
            assertFalse(right.has("frontTitle"))
            assertFalse(right.getBoolean("narrowFont"))
            assertFalse(a.getJSONObject("imageTextInfoLeft").getJSONObject("textInfo").getBoolean("narrowFont"))
        }
    }
    @Test fun checkInShowsGreenResultAndWindowProgressAndRejectsPreviousAction() {
        val before = entry(HabitGoalWindow.WEEK)
        val action = NotificationMetadataActions.intent(context, before, NotificationAction.CHECK_IN)
        val record = HabitRecord("checked", "habit-result", "2026-10-08", createdAt = "2026-10-08T12:30:00Z")
        val after = entry(HabitGoalWindow.WEEK, listOf(record))
        assertNull(NotificationMetadataActions.validate(context, Intent(action), listOf(after)))
        assertEquals(NotificationPhase.HABIT_CHECKED_IN, after.phase)
        val p = params(after)
        val a = area(p)
        assertEquals(context.getString(com.aritxonly.deadliner.R.string.notification_live_habit_checked),
            a.getJSONObject("imageTextInfoLeft").getJSONObject("textInfo").getString("title"))
        assertEquals(context.getString(com.aritxonly.deadliner.R.string.notification_island_encouragement),
            a.getJSONObject("imageTextInfoRight").getJSONObject("textInfo").getString("title"))
        val native = RealtimeNotificationRenderer.build(context, after)
        assertTrue(native.extras.getString(android.app.Notification.EXTRA_TEXT)!!.contains("1/3"))
        assertEquals("#FFA8DFC0", p.getJSONObject("param_island").getString("highlightColor"))
        assertFalse(after.actions.contains(NotificationAction.CHECK_IN))
    }
    @Test fun nameLimitDoesNotSplitEmojiAndKeepsShortNamesIntact() {
        assertEquals("早起跑步", HabitIslandPresentation.name("早起跑步"))
        assertEquals("😀😀😀😀😀😀😀…", HabitIslandPresentation.name("😀😀😀😀😀😀😀😀😀"))
    }
    @Test fun clearingPersistedCheckInKeepsTombstoneVersionAndRejectsOldButton() {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            val habit = Habit("persisted", "Read", schedule = HabitSchedule(HabitScheduleKind.WEEKLY_DAYS,
                "2026-10-01", weekdaysMask = HabitWeekdays.ALL), goal = HabitGoal(), reminder = HabitReminder("12:00"), createdAt = at)
            db.habits.create(habit)
            fun planned() = NotificationMetadataPlanner.plan(emptyList(), db.habits.list(), db.habits::recordsIncludingDeleted,
                NotificationPolicy(habitMode = HabitNotificationMode.REALTIME), now = Instant.parse("2026-10-08T12:31:00Z"), zone = TimeZone.UTC).realtime.single()
            val before = planned()
            val old = NotificationMetadataActions.intent(context, before, NotificationAction.CHECK_IN)
            db.habits.saveRecord(HabitRecord("persisted-record", habit.uid, "2026-10-08", createdAt = "2026-10-08T12:30:00Z"))
            assertEquals(NotificationPhase.HABIT_CHECKED_IN, planned().phase)
            db.habits.clearRecords(habit.uid, "2026-10-08", "2026-10-08T12:31:00Z")
            val cleared = planned()
            assertEquals(NotificationPhase.HABIT_DUE, cleared.phase)
            assertEquals(0, cleared.completedCount)
            assertTrue(db.habits.recordsIncludingDeleted(habit.uid).single().isDeleted)
            assertNotEquals(before.version, cleared.version)
            assertNull(NotificationMetadataActions.validate(context, old, listOf(cleared)))
        } finally { db.close() }
    }
    @Test fun taskTerminalTransitionKeepsBothTextComponentsOutOfNarrowFont() {
        val task = Task("font-task", "Report", dueAt = at, createdAt = at, updatedAt = at)
        val active = NotificationMetadataPlanner.plan(listOf(task), emptyList(), { emptyList() }, NotificationPolicy(),
            now = Instant.parse("2026-10-08T11:31:00Z"), zone = TimeZone.UTC).realtime.single()
        listOf(active, active.copy(phase = NotificationPhase.COMPLETED), active.copy(phase = NotificationPhase.ABANDONED)).forEach { e ->
            val a = area(params(e))
            listOf("imageTextInfoLeft", "imageTextInfoRight").forEach { key ->
                assertFalse(a.getJSONObject(key).getJSONObject("textInfo").getBoolean("narrowFont"))
            }
        }
    }
}
