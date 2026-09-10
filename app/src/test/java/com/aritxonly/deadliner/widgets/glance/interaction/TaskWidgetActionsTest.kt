package com.aritxonly.deadliner.widgets.glance.interaction

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.action.StartActivityIntentAction
import com.aritxonly.deadliner.LauncherActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetActionsTest {
    @Test
    fun `widget launch uses an explicit per-instance launcher intent`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val action = TaskWidgetActions.openAppAction(context, 42)

        assertTrue(action is StartActivityIntentAction)
        action as StartActivityIntentAction
        assertEquals(LauncherActivity::class.java.name, action.intent.component?.className)
        assertEquals("deadliner://widget/open/42", action.intent.dataString)
        assertTrue(action.intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(action.intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(action.intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertNull(action.activityOptions)
        val second = TaskWidgetActions.openAppAction(context, 43) as StartActivityIntentAction
        assertNotEquals(action.intent.data, second.intent.data)
    }

    @Test
    fun `collection click template is mutable and unique per widget`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val first = TaskWidgetActions.openCollectionPendingIntent(context, 42)
        val second = TaskWidgetActions.openCollectionPendingIntent(context, 43)

        assertFalse(first.isImmutable)
        assertNotEquals(first, second)
    }
}
