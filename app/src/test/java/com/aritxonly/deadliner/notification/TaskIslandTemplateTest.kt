package com.aritxonly.deadliner.notification

import android.app.Application
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.domain.task.Task
import org.json.JSONObject
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskIslandTemplateTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val now = Instant.now()
    private val task = Task("template-task", "Submit report", dueAt = now.plusSeconds(900).toString(),
        createdAt = now.toString(), updatedAt = now.toString())

    @Test fun taskTemplateContainsBlueSummaryAbsoluteDeadlineAndTwoExecutableButtons() {
        val extras = XiaomiIslandExtras.createUpcoming(context, task, now)
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        val island = params.getJSONObject("param_island")
        assertEquals(TaskIslandPresentation.BLUE, island.getString("highlightColor"))
        val big = island.getJSONObject("bigIslandArea")
        val left = big.getJSONObject("imageTextInfoLeft")
        assertEquals(5, left.getInt("type"))
        assertEquals(context.getString(R.string.notification_island_near), left.getJSONObject("textInfo").getString("title"))
        assertTrue(left.getJSONObject("textInfo").getBoolean("showHighlightColor"))
        assertEquals(TaskIslandPresentation.deadline(context, task, now), big.getJSONObject("textInfo").getString("title"))
        assertFalse(big.getJSONObject("textInfo").getBoolean("showHighlightColor"))
        assertEquals(task.title, params.getJSONObject("iconTextInfo").getString("title"))
        assertFalse(params.toString().contains("progressInfo"))
        val buttons = params.getJSONArray("textButton")
        assertEquals(2, buttons.length())
        assertEquals(context.getString(R.string.task_swipe_give_up), buttons.getJSONObject(0).getString("actionTitle"))
        assertEquals(context.getString(R.string.mark_complete), buttons.getJSONObject(1).getString("actionTitle"))
        val pictures = extras.getBundle("miui.focus.pics")!!
        for (index in 0..1) {
            val button = buttons.getJSONObject(index)
            assertEquals(1, button.getInt("actionIntentType"))
            val intent = Intent.parseUri(button.getString("actionIntent"), Intent.URI_INTENT_SCHEME)
            assertEquals(task.uid, intent.getStringExtra("task_uid"))
            assertEquals(NotificationTaskActionActivity::class.java.name, intent.component!!.className)
            assertTrue(pictures.containsKey(button.getString("actionIcon")))
        }
        val key = left.getJSONObject("picInfo").getString("pic")
        assertEquals(Icon.TYPE_BITMAP, pictures.getParcelable(key, Icon::class.java)!!.type)
        assertNotEquals(buttons.getJSONObject(0).getString("actionIntent"), buttons.getJSONObject(1).getString("actionIntent"))
    }

    @Test fun directPublicationReportsReadyForStaticDeduplication() {
        context.getSharedPreferences("island_bypass", 0).edit().putString("mode", "SYSTEM_HOOK").commit()
        var posted = false
        var accepted = false
        IslandWhitelistBypass.publish(context, "template-status", onPublication = {
            assertTrue(posted)
            accepted = it
        }) { posted = true }
        assertTrue(accepted)
    }

    @Test fun staticIslandDoesNotRepublishUntilTaskChanges() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")
        context.getSharedPreferences("island_bypass", 0).edit().putString("mode", "SYSTEM_HOOK").commit()
        GlobalUtils.init(context)
        GlobalUtils.deadlineNotification = true
        GlobalUtils.liveUpdatesInAdvance = 30
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        ReflectionHelpers.setStaticField(KmpV6Runtime::class.java, "database", db)
        val controller = Robolectric.buildService(UpcomingLiveUpdateService::class.java).create()
        try {
            db.tasks.create(task)
            val service = controller.get()
            service.onStartCommand(Intent().putExtra("task_uid", task.uid), 0, 1)
            val publication = ReflectionHelpers.getField<Any>(service, "lastIslandPublication")
            assertNotNull(publication)
            ReflectionHelpers.callInstanceMethod<Unit>(service, "refresh")
            assertSame(publication, ReflectionHelpers.getField<Any>(service, "lastIslandPublication"))
            service.onStartCommand(Intent().putExtra("task_uid", task.uid), 0, 2)
            val posted = org.robolectric.Shadows.shadowOf(context.getSystemService(android.app.NotificationManager::class.java))
                .getNotification(520_001)
            assertTrue(posted.extras.containsKey("miui.focus.param"))
            assertEquals(2, posted.actions.size)
            db.tasks.update(task.copy(title = "Updated report", updatedAt = "new-version"))
            ReflectionHelpers.callInstanceMethod<Unit>(service, "refresh")
            assertNotSame(publication, ReflectionHelpers.getField<Any>(service, "lastIslandPublication"))
        } finally {
            controller.destroy()
            db.close()
            ReflectionHelpers.setStaticField(KmpV6Runtime::class.java, "database", null)
        }
    }
}
