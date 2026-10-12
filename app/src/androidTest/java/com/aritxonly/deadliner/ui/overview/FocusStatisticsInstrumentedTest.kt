package com.aritxonly.deadliner.ui.overview

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.domain.focus.*
import com.deadliner.domain.statistics.FocusStatisticsEngine
import com.deadliner.domain.habit.*
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId

/** Isolated read-only emulator; cleanup only touches uniquely named fixtures. */
@RunWith(AndroidJUnit4::class)
class FocusStatisticsInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<OverviewActivity>()

    @Test fun fusedCardsShowBothMetricsWithoutSwitchingAndReactToEdits() = runBlocking {
        val core = KmpV6Runtime.database
        val uid = "focus-fusion-ui-" + System.nanoTime()
        val title = "Write the project proposal"
        val habitUid = "$uid-habit"
        val habitTitle = "Read research papers"
        val sessions = mutableListOf<String>()
        val taskUids = mutableListOf(uid)
        val now = java.time.Instant.now().toString()
        val today = LocalDate.now()
        check(core.focusTimerNotificationInfo().snapshot == null) { "Requires an idle test timer" }
        try {
            core.tasks.create(Task(uid, title, createdAt = now, updatedAt = now))
            core.habits.create(Habit(habitUid, habitTitle,
                schedule = HabitSchedule(HabitScheduleKind.WEEKLY_DAYS, "2026-01-01", weekdaysMask = HabitWeekdays.ALL),
                goal = HabitGoal(), createdAt = now, updatedAt = now))
            val timer = core.createFocusTimerService()
            listOf(today.minusMonths(2).withDayOfMonth(1) to 300L,
                today.minusMonths(1).withDayOfMonth(1) to 600L,
                today.minusMonths(1).withDayOfMonth(3) to 1_500L,
                today.minusDays(6) to 900L,
                today.minusDays(2) to 1_800L,
                today to 1_200L).forEach { (date, seconds) ->
                val start = date.atStartOfDay(ZoneId.systemDefault()).plusHours(9).toInstant()
                timer.execute(FocusTimerCommand.Start(uid, FocusTimerMode.STOPWATCH), Instant.parse(start.toString()))
                val result = timer.execute(FocusTimerCommand.Finish, Instant.parse(start.plusSeconds(seconds).toString()))
                sessions += requireNotNull(result.session).uid
            }
            listOf(today to 900L, today.minusMonths(1).withDayOfMonth(2) to 1_200L).forEach { (date, seconds) ->
                val start = date.atStartOfDay(ZoneId.systemDefault()).plusHours(11).toInstant()
                timer.execute(FocusTimerCommand.StartHabit(habitUid, FocusTimerMode.STOPWATCH), Instant.parse(start.toString()))
                sessions += requireNotNull(timer.execute(FocusTimerCommand.Finish,
                    Instant.parse(start.plusSeconds(seconds).toString())).session).uid
            }
            listOf(today.minusDays(6), today.minusDays(2), today,
                today.minusMonths(1).withDayOfMonth(3)).forEachIndexed { index, date ->
                val completed = date.atStartOfDay(ZoneId.systemDefault()).plusHours(10).toInstant().toString()
                val taskUid = "$uid-result-$index"
                taskUids += taskUid
                core.tasks.create(Task(taskUid, "Completed task $index", dueAt = completed,
                    state = TaskState.COMPLETED, completedAt = completed, createdAt = completed, updatedAt = now))
            }
            // The legacy Task screen reads on entry; reload it after fixtures are ready.
            ui.activityRule.scenario.recreate()
            waitForText(text(R.string.focus_stats_today_duration))
            ui.onNodeWithText(text(R.string.focus_stats_today_overview)).assertIsDisplayed()
            ui.onNodeWithText(text(R.string.focus_stats_today_duration)).assertIsDisplayed()
            ui.onNodeWithTag("focus-today-types").assertIsDisplayed().assertTextEquals(
                focusTargetTimeBrief(ui.activity, core.focusStatistics.overview().today.durationByTarget)!!)
            capture("fusion-overview")
            ui.onNodeWithTag("overview-focus-overview-list").performScrollToNode(hasText(title))
            waitForText(title)
            val renamed = "Revise the project proposal"
            core.tasks.update(core.tasks.find(uid)!!.copy(title = renamed, updatedAt = now))
            waitForText(renamed)
            ui.onNodeWithText(renamed).assertIsDisplayed()
            val habitLabel = "$habitTitle · ${text(R.string.habit_focus_target)}"
            ui.onNodeWithTag("overview-focus-overview-list").performScrollToNode(hasText(habitLabel))
            ui.onNodeWithText(habitLabel).assertIsDisplayed()
            capture("fusion-distribution")
            core.focusSessions.adjustDuration(sessions.last(), 600, now)
            ui.onNodeWithTag("overview-focus-overview-list").performScrollToIndex(0)
            ui.waitUntil(10_000) {
                ui.onAllNodesWithText(focusDuration(ui.activity, core.focusStatistics.overview().today.totalDurationSeconds))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            ui.onNodeWithText(text(R.string.tab_trend)).performClick()
            waitForText(text(R.string.contribution_heatmap))
            showSection("overview-focus-trend-list", text(R.string.focus_stats_recent_days))
            ui.onNodeWithTag("task-trend-daily").assertIsDisplayed()
            ui.onNodeWithTag("task-total-daily").assertIsDisplayed()
                .assertTextEquals(ui.activity.getString(R.string.focus_stats_task_count, 3))
            ui.onNodeWithTag("focus-trend-daily").assertIsDisplayed()
            ui.onNodeWithTag("focus-trend-daily-types").assertIsDisplayed().assertTextEquals(
                focusTargetTimeBrief(ui.activity, core.focusStatistics.overview().recentDaysSummary!!.durationByTarget)!!)
            capture("fusion-trend-daily")
            showSection("overview-focus-trend-list", text(R.string.monthly_trend))
            ui.onNodeWithTag("task-trend-monthly").assertIsDisplayed()
            ui.onNodeWithTag("focus-trend-monthly").assertIsDisplayed()
            ui.onNodeWithTag("focus-trend-monthly-types").assertIsDisplayed().assertTextEquals(
                focusTargetTimeBrief(ui.activity, core.focusStatistics.overview().recentMonthsSummary!!.durationByTarget)!!)
            capture("fusion-trend-monthly")
            showSection("overview-focus-trend-list", text(R.string.prev4weeks))
            ui.onNodeWithTag("task-trend-weekly").assertIsDisplayed()
            ui.onNodeWithTag("focus-trend-weekly").assertIsDisplayed()
            ui.onNodeWithTag("focus-trend-weekly-types").assertIsDisplayed().assertTextEquals(
                focusTargetTimeBrief(ui.activity, core.focusStatistics.overview().recentWeeksSummary!!.durationByTarget)!!)
            capture("fusion-trend-weekly")
            ui.onNodeWithText(text(R.string.tab_summary)).performClick()
            waitForText(text(R.string.last_month_summary))
            capture("fusion-last-month-header")
            showSection("overview-focus-month-list", text(R.string.focus_stats_duration))
            ui.onNodeWithText(text(R.string.focus_stats_days)).assertIsDisplayed()
            ui.onAllNodesWithText(focusTargetTimeBrief(ui.activity,
                core.focusStatistics.overview().lastMonth.current.summary.durationByTarget)!!).onFirst().assertIsDisplayed()
            capture("fusion-last-month-metrics")
            ui.onNodeWithTag("overview-focus-month-list").performScrollToNode(hasText(text(R.string.last_month_activity_frequency)))
            showSection("overview-focus-month-list", text(R.string.last_month_activity_frequency))
            ui.onNodeWithText(text(R.string.last_month_activity_frequency)).assertIsDisplayed()
            ui.onNodeWithTag("focus-last-month-daily").assertIsDisplayed()
            ui.onNodeWithTag("focus-last-month-daily-types").assertIsDisplayed().assertTextEquals(
                focusTargetTimeBrief(ui.activity, core.focusStatistics.overview().lastMonth.current.summary.durationByTarget)!!)
            capture("fusion-last-month-activity")
        } finally {
            sessions.forEach { core.focusSessions.delete(it, now) }
            taskUids.forEach { core.tasks.delete(it, now) }
            core.habits.delete(habitUid, now)
        }
    }

    @Test fun habitOnlyShowsSourceWithoutInventingTaskResults() {
        val today = LocalDate.now()
        fun session(uid: String, date: LocalDate, seconds: Long): FocusSession {
            val start = date.atStartOfDay(java.time.ZoneOffset.UTC).plusHours(9).toInstant()
            return FocusSession(uid, "", "Reading", FocusTimerMode.STOPWATCH,
                startedAt = start.toString(), endedAt = start.plusSeconds(seconds).toString(), durationSeconds = seconds,
                completionKind = FocusCompletionKind.FOCUS_ONLY, createdAt = start.toString(),
                target = FocusTargetRef(FocusTargetKind.HABIT, "habit-only"), habitOccurredOn = date.toString())
        }
        val data = FocusStatisticsEngine(listOf(session("today", today, 600),
            session("month", today.minusMonths(1).withDayOfMonth(1), 480)), emptyList(), emptyList(), TimeZone.UTC)
            .overview(kotlinx.datetime.LocalDate.parse(today.toString()))
        val state = FocusOverviewState(data = data, isLoading = false)
        ui.activity.runOnUiThread {
            ui.activity.setContent { DeadlinerTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ActiveStatsCard(linkedMapOf("Completed" to 0, "Pending" to 0), state)
                    FocusTrendSection(state, data.daily, FocusChartKind.DAILY, "habit-only-chart", data.recentDaysSummary)
                    focusMonthlyMetrics(ui.activity, data.lastMonth).forEach { metric ->
                        androidx.compose.material3.Text(metric.label + ": " + metric.value)
                        metric.supportingText?.let { androidx.compose.material3.Text(it) }
                    }
                }
            } }
        }
        waitForText(text(R.string.focus_stats_today_duration))
        ui.onNodeWithTag("focus-today-types").assertIsDisplayed()
            .assertTextEquals(focusTargetTimeBrief(ui.activity, data.today.durationByTarget)!!)
        ui.onNodeWithTag("habit-only-chart-types").performScrollTo().assertIsDisplayed()
            .assertTextEquals(focusTargetTimeBrief(ui.activity, data.recentDaysSummary!!.durationByTarget)!!)
        ui.onNodeWithTag("habit-only-chart").assertIsDisplayed()
        ui.onNodeWithText(focusTargetTimeBrief(ui.activity, data.lastMonth.current.summary.durationByTarget)!!)
            .performScrollTo().assertIsDisplayed()
        capture("habit-only-metrics")
    }

    @Test fun emptyFocusIsInlineAndDoesNotAllocateBlankCharts() {
        val empty = FocusOverviewState(data = FocusStatisticsEngine(emptyList(), emptyList(), emptyList(), TimeZone.UTC)
            .overview(kotlinx.datetime.LocalDate.parse(LocalDate.now().toString())), isLoading = false)
        ui.activity.runOnUiThread {
            ui.activity.setContent {
                DeadlinerTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ActiveStatsCard(linkedMapOf("Completed" to 0, "Pending" to 0), empty)
                        FocusTrendSection(empty, empty.data!!.daily, FocusChartKind.DAILY, "empty-focus-chart")
                        focusMonthlyMetrics(ui.activity, empty.data.lastMonth).forEach { metric ->
                            androidx.compose.material3.Text(metric.label + ": " + metric.value)
                            metric.change?.let { androidx.compose.material3.Text(it) }
                        }
                    }
                }
            }
        }
        waitForText(text(R.string.focus_stats_empty_period))
        ui.onNodeWithText(text(R.string.focus_stats_today_duration)).assertIsDisplayed()
        ui.onNodeWithTag("empty-focus-chart").assertDoesNotExist()
        ui.onNodeWithTag("empty-focus-chart-empty").assertIsDisplayed()
        ui.onNodeWithTag("focus-today-types").assertDoesNotExist()
        capture("fusion-empty")
        ui.onNodeWithText(text(R.string.focus_stats_both_months_empty)).performScrollTo().assertIsDisplayed()
    }

    private fun showSection(listTag: String, heading: String) {
        ui.onNodeWithTag(listTag).performScrollToNode(hasText(heading))
        val viewport = ui.onRoot().fetchSemanticsNode().size.height
        val top = ui.onNodeWithText(heading).fetchSemanticsNode().boundsInRoot.top
        ui.onNodeWithTag(listTag).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, top - viewport * 0.2f) }
        ui.waitForIdle()
    }

    private fun text(id: Int) = ui.activity.getString(id)
    private fun waitForText(value: String) {
        ui.waitUntil(10_000) { ui.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        FileOutputStream(File(instrumentation.targetContext.cacheDir, "$name.png")).use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }
}
