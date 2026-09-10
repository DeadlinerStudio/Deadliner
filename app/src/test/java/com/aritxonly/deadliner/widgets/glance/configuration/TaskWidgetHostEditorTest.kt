package com.aritxonly.deadliner.widgets.glance.configuration

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetHostEditorTest {
    @Test
    fun `standard configuration extra resolves widget id`() {
        val intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42)

        assertEquals(42, TaskWidgetHostEditor.resolveAppWidgetId(intent))
    }

    @Test
    fun `miui edit uri resolves widget id`() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "deadliner://com.aritxonly.deadliner/widget/TaskWidgetConfigurationActivity" +
                    "?miuiWidgetId=73",
            ),
        )

        assertEquals(73, TaskWidgetHostEditor.resolveAppWidgetId(intent))
    }

    @Test
    fun `missing widget id remains invalid`() {
        assertEquals(
            AppWidgetManager.INVALID_APPWIDGET_ID,
            TaskWidgetHostEditor.resolveAppWidgetId(Intent()),
        )
    }

    @Test
    fun `host is the default editor source`() {
        assertEquals(
            TaskWidgetEditorSource.HOST,
            TaskWidgetHostEditor.resolveEditorSource(Intent()),
        )
    }

    @Test
    fun `settings edit intent includes id and settings source`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()

        val intent = TaskWidgetHostEditor.settingsEditIntent(context, 91)

        assertEquals(91, TaskWidgetHostEditor.resolveAppWidgetId(intent))
        assertEquals(
            TaskWidgetEditorSource.SETTINGS,
            TaskWidgetHostEditor.resolveEditorSource(intent),
        )
        assertEquals(
            TaskWidgetConfigurationActivity::class.java.name,
            intent.component?.className,
        )
    }

    @Test
    fun `installed widgets are de-duplicated and sorted by kind then id`() {
        assertEquals(
            listOf(
                TaskWidgetInstance(7, TaskWidgetKind.SQUARE),
                TaskWidgetInstance(12, TaskWidgetKind.SQUARE),
                TaskWidgetInstance(3, TaskWidgetKind.CAPSULE),
            ),
            TaskWidgetHostEditor.mergeInstalledWidgets(
                squareIds = intArrayOf(12, AppWidgetManager.INVALID_APPWIDGET_ID, 7),
                capsuleIds = intArrayOf(3, 7),
            ),
        )
    }

    @Test
    fun `only a host editor that owns its task removes the task on finish`() {
        assertTrue(
            TaskWidgetHostEditor.shouldRemoveEditorTask(
                TaskWidgetEditorSource.HOST,
                isTaskRoot = true,
            ),
        )
        assertFalse(
            TaskWidgetHostEditor.shouldRemoveEditorTask(
                TaskWidgetEditorSource.HOST,
                isTaskRoot = false,
            ),
        )
        assertFalse(
            TaskWidgetHostEditor.shouldRemoveEditorTask(
                TaskWidgetEditorSource.SETTINGS,
                isTaskRoot = true,
            ),
        )
    }
}
