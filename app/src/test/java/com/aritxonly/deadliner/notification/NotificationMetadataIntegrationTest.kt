package com.aritxonly.deadliner.notification

import android.app.Application
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.habit.*
import com.deadliner.platform.*
import com.deadliner.service.NotificationMetadataPlanner
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NotificationMetadataIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val due = "2026-10-07T12:00:00Z"
    private val task = Task("metadata-task", "Report", dueAt = due, createdAt = due, updatedAt = due)
    private fun plan(now: String, task: Task = this.task, policy: NotificationPolicy = NotificationPolicy()) =
        NotificationMetadataPlanner.plan(listOf(task), emptyList(), { emptyList() }, policy,
            now = Instant.parse(now), zone = TimeZone.UTC)

    @Test fun alarmIdentityDiffsDoNotCollideAndSwitchesCancelOnlyTheirOwnSchedules() {
        val scheduler = AndroidNotificationMetadataScheduler(context)
        scheduler.apply(plan("2026-10-06T10:00:00Z"))
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(3, alarms.scheduledAlarms.size)
        val identities = alarms.scheduledAlarms.map { shadowOf(it.operation).savedIntent.data.toString() }
        assertEquals(3, identities.toSet().size)
        scheduler.apply(plan("2026-10-06T10:00:00Z"))
        assertEquals(3, alarms.scheduledAlarms.size)
        scheduler.apply(plan("2026-10-06T10:00:00Z", policy = NotificationPolicy(taskRealtime = false)))
        assertEquals(2, alarms.scheduledAlarms.size)
        scheduler.apply(plan("2026-10-06T10:00:00Z", policy = NotificationPolicy(false, false, true)))
        assertEquals(1, alarms.scheduledAlarms.size)
        scheduler.cancelAll()
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test fun reconciliationRetiresOrphanCardsButPreservesUnrelatedNotifications() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val note = androidx.core.app.NotificationCompat.Builder(context, "test").setSmallIcon(com.aritxonly.deadliner.R.mipmap.ic_launcher).build()
        manager.notify("realtime:deleted", 1, note)
        manager.notify("unrelated", 1, note)
        AndroidNotificationMetadataScheduler(context).apply(NotificationMetadata(emptyList(), emptyList(), null))
        assertEquals(listOf("unrelated"), manager.activeNotifications.map { it.tag })
        manager.notify("live:old-generation", 1, note)
        AndroidNotificationGateway(context).cancelAll()
        assertEquals(listOf("unrelated"), manager.activeNotifications.map { it.tag })
    }

    @Test fun deliveredOrdinaryNotificationIsDeduplicatedAndRemovedAfterCompletion() {
        val scheduler = AndroidNotificationMetadataScheduler(context)
        val entry = plan("2026-10-06T10:00:00Z").alarms.first()
        assertTrue(scheduler.deliver(entry))
        assertFalse(AndroidNotificationMetadataScheduler(context).deliver(entry))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertEquals(1, manager.allNotifications.size)
        scheduler.apply(plan("2026-10-07T11:30:00Z", task.copy(state = com.deadliner.domain.task.TaskState.COMPLETED)))
        assertTrue(manager.allNotifications.isEmpty())
    }

    @Test fun realtimePresentationSeparatesClockAndAppIconWithShortTimeStateColorsAndRestoreButton() {
        val entry = plan("2026-10-07T11:30:00Z").realtime.single()
        listOf(NotificationPhase.UPCOMING, NotificationPhase.OVERDUE, NotificationPhase.COMPLETED, NotificationPhase.ABANDONED).forEach { phase ->
            val value = entry.copy(phase = phase, actions = if (phase in listOf(NotificationPhase.COMPLETED, NotificationPhase.ABANDONED)) listOf(NotificationAction.RESTORE_ACTIVE) else entry.actions)
            val extras = XiaomiIslandExtras.createRealtime(context, value, "Deadline")
            val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
            val island = params.getJSONObject("param_island")
            assertEquals(RealtimeNotificationRenderer.color(phase), island.getString("highlightColor"))
            assertEquals(5, island.getJSONObject("bigIslandArea").getJSONObject("imageTextInfoRight").getJSONObject("textInfo").getString("title").length)
            val bigArea = island.getJSONObject("bigIslandArea")
            assertEquals(1, bigArea.getJSONObject("imageTextInfoLeft").getInt("type"))
            assertEquals(2, bigArea.getJSONObject("imageTextInfoRight").getInt("type"))
            assertFalse(bigArea.has("textInfo"))
            val rightText = bigArea.getJSONObject("imageTextInfoRight").getJSONObject("textInfo")
            assertTrue(!rightText.has("frontTitle") || rightText.getString("frontTitle").isNotEmpty())
            assertFalse(params.toString().contains("progressInfo"))
            val pictures = extras.getBundle("miui.focus.pics")!!
            val clock = params.getJSONObject("iconTextInfo").getJSONObject("animIconInfo")
            val appPicture = params.getJSONObject("picInfo").getString("pic")
            val collapsed = island.getJSONObject("bigIslandArea").getJSONObject("imageTextInfoLeft")
                .getJSONObject("picInfo").getString("pic")
            assertEquals("miui.focus.pic_deadliner_clock", clock.getString("src"))
            assertEquals(clock.getString("src"), clock.getString("srcDark"))
            assertEquals("miui.focus.pic_deadliner", appPicture)
            assertEquals("miui.land.pic_deadliner_state", collapsed)
            assertEquals(collapsed, island.getJSONObject("smallIslandArea").getJSONObject("combinePicInfo")
                .getJSONObject("picInfo").getString("pic"))
            assertNotEquals(appPicture, clock.getString("src"))
            assertTrue(listOf(appPicture, clock.getString("src"), collapsed).all(pictures::containsKey))
            assertEquals(value.actions.size, params.optJSONArray("textButton")?.length() ?: 0)
        }
        assertNotEquals(RealtimeNotificationRenderer.color(NotificationPhase.UPCOMING), RealtimeNotificationRenderer.color(NotificationPhase.OVERDUE))
    }

    @Test fun realtimeNativeContentUsesFixedTimeAndNeverSchedulesSystemCountdown() {
        val entry = plan("2026-10-07T11:30:00Z").realtime.single()
        val note = RealtimeNotificationRenderer.build(context, entry)
        assertEquals(0L, note.`when`)
        assertFalse(note.extras.getBoolean(android.app.Notification.EXTRA_SHOW_WHEN))
        assertFalse(note.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertFalse(note.extras.getBoolean(android.app.Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(entry.title, note.extras.getString(android.app.Notification.EXTRA_TITLE))
        val text = note.extras.getString(android.app.Notification.EXTRA_TEXT)!!
        assertTrue(text.contains(RealtimeNotificationRenderer.status(context, entry.phase)))
        assertTrue(text.contains(RealtimeNotificationRenderer.deadline(context, entry)))
        assertEquals(0, note.extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX))
        assertFalse(note.extras.getBoolean(android.app.Notification.EXTRA_PROGRESS_INDETERMINATE))
        assertEquals(entry.actions.size, note.actions.size)
    }

    @Test fun taskResultReceiptsSurviveRestartAndCannotBeActionedAgain() {
        val terminal = task.copy(state = com.deadliner.domain.task.TaskState.COMPLETED, updatedAt = "new")
        val receipt = TaskNotificationResult(task.uid, "new", NotificationPhase.COMPLETED, "2026-10-07T11:30:00Z")
        NotificationInteractionStore.result(context, receipt)
        val metadata = NotificationMetadataPlanner.plan(listOf(terminal), emptyList(), { emptyList() }, NotificationPolicy(),
            NotificationInteractionStore.read(context), Instant.parse("2026-10-07T11:31:00Z"), TimeZone.UTC)
        assertEquals(NotificationPhase.COMPLETED, metadata.realtime.single().phase)
        val active = plan("2026-10-07T11:30:00Z").realtime.single()
        val intent = NotificationMetadataActions.intent(context, active, NotificationAction.COMPLETE)
        assertNull(NotificationMetadataActions.validate(context, intent, metadata.realtime))
    }

    @Test fun capabilitiesRejectForgedStaleAndRepeatedHabitClicks() {
        val habit = Habit("action-habit", "Read", schedule = HabitSchedule(HabitScheduleKind.WEEKLY_DAYS,
            "2026-10-01", weekdaysMask = HabitWeekdays.ALL), goal = HabitGoal(), reminder = HabitReminder("12:00"), createdAt = due)
        val policy = NotificationPolicy(habitMode = HabitNotificationMode.REALTIME)
        fun metadata(state: NotificationInteractionState = NotificationInteractionState()) = NotificationMetadataPlanner.plan(emptyList(), listOf(habit),
            { emptyList() }, policy, state, Instant.parse("2026-10-07T12:30:00Z"), TimeZone.UTC)
        val entry = metadata().realtime.single()
        val intent = NotificationMetadataActions.intent(context, entry, NotificationAction.CHECK_IN)
        val roundTrip = Intent.parseUri(intent.toUri(Intent.URI_INTENT_SCHEME), Intent.URI_INTENT_SCHEME)
        assertNotNull(NotificationMetadataActions.validate(context, roundTrip, metadata().realtime))
        assertNull(NotificationMetadataActions.validate(context, Intent(intent).apply { removeExtra("capability") }, metadata().realtime))
        assertNull(NotificationMetadataActions.validate(context, Intent(intent).putExtra("version", "stale"), metadata().realtime))
        NotificationInteractionStore.dismiss(context, entry.key)
        assertNull(NotificationMetadataActions.validate(context, intent, metadata(NotificationInteractionStore.read(context)).realtime))
        val tomorrow = NotificationMetadataPlanner.plan(emptyList(), listOf(habit), { emptyList() }, policy, NotificationInteractionStore.read(context),
            Instant.parse("2026-10-08T12:30:00Z"), TimeZone.UTC)
        assertEquals(1, tomorrow.realtime.size)
    }

    @Test fun habitCheckInUsesAddAndNeverTogglesCompletedRecordsBackToZero() {
        val today = java.time.LocalDate.now().toString()
        val now = java.time.Instant.now().toString()
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            val habit = Habit("quantity", "Read", schedule = HabitSchedule(HabitScheduleKind.WEEKLY_DAYS,
                today, weekdaysMask = HabitWeekdays.ALL), goal = HabitGoal(targetCount = 3), createdAt = now)
            db.habits.create(habit)
            db.habits.addRecordAmount(habit.uid, today, 1, now)
            db.habits.addRecordAmount(habit.uid, today, 1, now)
            assertEquals(2, db.habits.records(habit.uid).filterNot { it.isDeleted }.sumOf { it.count })
        } finally { db.close() }
    }

    @Test fun committedCrudUsesMetadataPathAndRegistersTerminalExpiry() {
        GlobalUtils.init(context)
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        var interaction = NotificationInteractionState()
        var last: NotificationMetadata? = null
        val gateway = object : NotificationGateway by AndroidNotificationGateway(context) {
            override fun applyNotificationMetadata(metadata: NotificationMetadata) {
                last = metadata
                AndroidNotificationMetadataScheduler(context).apply(metadata)
            }
        }
        try {
            db.configureNotificationUpdates(gateway, { NotificationPolicy() }, { interaction })
            val now = java.time.Instant.now()
            val active = task.copy(uid = "crud-result", dueAt = now.plusSeconds(1800).toString(), updatedAt = now.toString())
            db.tasks.create(active)
            assertEquals(NotificationPhase.UPCOMING, last!!.realtime.single().phase)
            val result = db.tasks.applyAction(active.uid, TaskAction.MARK_COMPLETE, now.toString())
            assertTrue(last!!.realtime.isEmpty())
            interaction = NotificationInteractionState(listOf(TaskNotificationResult(active.uid, result.task!!.updatedAt,
                NotificationPhase.COMPLETED, now.toString())))
            db.triggerNotificationInfoUpdate()
            assertEquals(NotificationPhase.COMPLETED, last!!.realtime.single().phase)
            assertEquals(now.plusSeconds(180).toString(), last!!.realtime.single().endsAt)
            val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
            assertEquals(1, alarms.scheduledAlarms.size)
            db.triggerNotificationInfoUpdate(force = true)
            assertEquals(1, alarms.scheduledAlarms.size)
        } finally { db.close() }
    }

    @Test fun undoCompletionAndAbandonmentRestoresCurrentPhaseAndRejectsRepeatedClick() {
        GlobalUtils.init(context)
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            listOf(TaskAction.MARK_COMPLETE, TaskAction.MARK_GIVE_UP).forEachIndexed { index, command ->
                val now = java.time.Instant.now()
                val active = task.copy(uid = "undo-$index", dueAt = now.plusSeconds(if (index == 0) 1800 else -1800).toString(), updatedAt = now.toString())
                db.tasks.create(active)
                val result = db.tasks.applyAction(active.uid, command, now.plusMillis(1).toString()).task!!
                val phase = if (index == 0) NotificationPhase.COMPLETED else NotificationPhase.ABANDONED
                NotificationInteractionStore.result(context, TaskNotificationResult(result.uid, result.updatedAt, phase, now.toString()))
                fun metadata() = NotificationMetadataPlanner.plan(listOf(db.tasks.find(active.uid)!!), emptyList(), { emptyList() },
                    NotificationPolicy(), NotificationInteractionStore.read(context), Instant.parse(now.plusSeconds(1).toString()), TimeZone.UTC)
                val terminal = metadata().realtime.single()
                assertEquals(listOf(NotificationAction.RESTORE_ACTIVE), terminal.actions)
                val intent = NotificationMetadataActions.intent(context, terminal, NotificationAction.RESTORE_ACTIVE)
                val validated = NotificationMetadataActions.validate(context, intent, metadata().realtime)!!
                db.tasks.applyAction(validated.first.ownerUid, NotificationMetadataActions.taskAction(validated.second), now.plusMillis(2).toString())
                NotificationInteractionStore.clearResult(context, active.uid)
                assertEquals(com.deadliner.domain.task.TaskState.ACTIVE, db.tasks.find(active.uid)!!.state)
                val restored = metadata().realtime.single()
                assertEquals(if (index == 0) NotificationPhase.UPCOMING else NotificationPhase.OVERDUE, restored.phase)
                assertEquals(listOf(NotificationAction.GIVE_UP, NotificationAction.COMPLETE), restored.actions)
                assertNull(NotificationMetadataActions.validate(context, intent, metadata().realtime))
                assertTrue(NotificationInteractionStore.read(context).taskResults.none { it.taskUid == active.uid })
                val outsideWindow = NotificationMetadataPlanner.plan(listOf(db.tasks.find(active.uid)!!), emptyList(), { emptyList() },
                    NotificationPolicy(), now = Instant.parse(now.plusSeconds(7200).toString()), zone = TimeZone.UTC)
                assertTrue(outsideWindow.realtime.isEmpty())
            }
        } finally { db.close() }
    }

    @Test fun independentPolicyMigrationPreservesFormerTaskOptOut() {
        GlobalUtils.init(context)
        context.getSharedPreferences("notification_policy_v2", 0).edit().clear().commit()
        GlobalUtils.deadlineNotification = false
        assertEquals(NotificationPolicy(false, false, false), NotificationPolicyPreferences.read(context))
        val policy = NotificationPolicy(true, false, true, HabitNotificationMode.REALTIME)
        NotificationPolicyPreferences.save(context, policy)
        assertEquals(policy, NotificationPolicyPreferences.read(context))
    }
}
