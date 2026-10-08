package com.aritxonly.deadliner.notification

import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.domain.task.Task
import com.deadliner.platform.NotificationService
import com.deadliner.platform.TaskNotificationConfig
import com.deadliner.platform.UpcomingLiveUpdateConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NotificationDeliveryTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test fun actualCoreAlarmResolvesToTheManifestReceiver() {
        val service = NotificationService(context)
        val due = Instant.now().plusSeconds(7200).toString()
        service.setTaskNotification(TaskNotificationConfig("delivery-test", "Task", due, reminderMinutes = 60))
        service.setUpcomingLiveUpdate(UpcomingLiveUpdateConfig("delivery-test", "Task", due))
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertEquals(2, alarms.size)
        alarms.forEach { alarm ->
            val intent = shadowOf(alarm.operation).savedIntent
            assertEquals("deadliner-notification", intent.data!!.scheme)
            assertReceiver(intent)
        }
    }

    @Test fun allActionsResolveWithNewIdentitiesAndLegacyAlarms() {
        listOf(NotificationService.ACTION_TASK, NotificationService.ACTION_HABIT,
            NotificationService.ACTION_DAILY, NotificationService.ACTION_LIVE).forEach { action ->
            val intent = Intent(action).setPackage(context.packageName)
            assertReceiver(intent)
            assertReceiver(Intent(intent).setData(Uri.parse("deadliner-notification://${context.packageName}/$action/uid")))
        }
        val wrongHost = Intent(NotificationService.ACTION_LIVE).setPackage(context.packageName)
            .setData(Uri.parse("deadliner-notification://another.app/task/uid"))
        assertTrue(context.packageManager.queryBroadcastReceivers(wrongHost, 0).isEmpty())
    }

    @Test fun existingTaskInTheWindowRequestsServiceWithoutWaitingForAlarm() {
        GlobalUtils.init(context)
        GlobalUtils.deadlineNotification = true
        GlobalUtils.liveUpdatesInAdvance = 30
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        ReflectionHelpers.setStaticField(KmpV6Runtime::class.java, "database", db)
        try {
            val now = Instant.now()
            val task = Task("near-task", "Real task", dueAt = now.plusSeconds(900).toString(),
                createdAt = now.toString(), updatedAt = now.toString())
            db.tasks.create(task)
            val gateway = AndroidNotificationGateway(context)
            gateway.setUpcomingLiveUpdate(UpcomingLiveUpdateConfig(task.uid, task.title, task.dueAt!!))
            val launch = shadowOf(context).nextStartedService
            assertNotNull(launch)
            assertEquals(UpcomingLiveUpdateService::class.java.name, launch.component!!.className)
            assertEquals(task.uid, launch.getStringExtra("task_uid"))
            val controller = org.robolectric.Robolectric.buildService(UpcomingLiveUpdateService::class.java).create()
            try {
                controller.get().onStartCommand(launch, 0, 1)
                val placeholder = shadowOf(controller.get()).lastForegroundNotification
                assertFalse(placeholder.extras.containsKey("miui.focus.param"))
                val posted = shadowOf(context.getSystemService(android.app.NotificationManager::class.java))
                    .getNotification(520_001)
                assertTrue(posted.extras.getString(android.app.Notification.EXTRA_TITLE)!!.contains(task.title))
            } finally { controller.destroy() }
            val moved = task.copy(dueAt = now.plusSeconds(7200).toString())
            db.tasks.update(moved)
            gateway.setUpcomingLiveUpdate(UpcomingLiveUpdateConfig(moved.uid, moved.title, moved.dueAt!!))
            assertNull(shadowOf(context).nextStartedService)
        } finally {
            db.close()
            ReflectionHelpers.setStaticField(KmpV6Runtime::class.java, "database", null)
        }
    }

    @Test fun disabledBypassStillPublishesExactlyOnce() {
        context.getSharedPreferences("island_bypass", 0).edit().clear().commit()
        var calls = 0
        IslandWhitelistBypass.publish(context, "test") { calls++ }
        assertEquals(1, calls)
    }

    @Test fun systemHookModePublishesDirectlyWithoutAnAuthorizationWindow() {
        context.getSharedPreferences("island_bypass", 0).edit().putString("mode", "SYSTEM_HOOK").commit()
        var calls = 0
        IslandWhitelistBypass.publish(context, "system-hook") { calls++ }
        assertEquals(1, calls)
        assertFalse(context.getSharedPreferences("island_bypass", 0).contains("pending_uid"))
    }

    private fun assertReceiver(intent: Intent) {
        assertEquals(listOf(KmpNotificationReceiver::class.java.name),
            context.packageManager.queryBroadcastReceivers(intent, 0).map { it.activityInfo.name })
    }
}
