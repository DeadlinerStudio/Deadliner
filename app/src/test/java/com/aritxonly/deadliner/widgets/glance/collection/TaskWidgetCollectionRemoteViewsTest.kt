package com.aritxonly.deadliner.widgets.glance.collection

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfiguration
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetThemeMode
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetRowColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetCollectionRemoteViewsTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `collection root uses a service adapter unique to the widget instance`() {
        val first = TaskWidgetCollectionRemoteViews.adapterIntent(context, 42)
        val second = TaskWidgetCollectionRemoteViews.adapterIntent(context, 43)

        assertEquals(TaskWidgetCollectionService::class.java.name, first.component?.className)
        assertEquals("deadliner://widget-list/42", first.dataString)
        assertNotEquals(first.data, second.data)

        val root = TaskWidgetCollectionRemoteViews.create(context, 42, TEST_COLORS.textSecondary)
            .apply(context, FrameLayout(context))
        assertNotNull(root.findViewById<ListView>(R.id.widget_task_collection_list))
    }

    @Test
    fun `collection service is exported only behind bind remote views permission`() {
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, TaskWidgetCollectionService::class.java),
            0,
        )

        assertTrue(info.exported)
        assertEquals("android.permission.BIND_REMOTEVIEWS", info.permission)
    }

    @Test
    fun `service factory exposes every task with stable ids and styled rows`() {
        val snapshot = TaskWidgetSnapshot(
            totalCount = 2,
            nearCount = 1,
            overdueCount = 0,
            summary = "2 tasks",
            items = listOf(
                TaskWidgetItem(11L, "First", "1h", TaskWidgetStatus.NEAR),
                TaskWidgetItem(12L, "Second", "2h", TaskWidgetStatus.NORMAL),
            ),
        )
        val factory = TaskWidgetCollectionRemoteViewsFactory(
            context = context,
            appWidgetId = 42,
            snapshotLoader = { snapshot },
            colorsResolver = { _, _, _ -> TEST_COLORS },
        )

        factory.onCreate()
        factory.onDataSetChanged()

        assertEquals(2, factory.count)
        assertTrue(factory.hasStableIds())
        assertEquals(11L, factory.getItemId(0))
        val row = factory.getViewAt(0)!!.apply(context, FrameLayout(context))
        assertEquals("First", row.findViewById<TextView>(R.id.widget_task_collection_row_title).text)
        assertEquals("1h", row.findViewById<TextView>(R.id.widget_task_collection_row_time).text)
        assertNotNull(row.findViewById<ImageView>(R.id.widget_task_collection_row_background).drawable)

        factory.onDestroy()
    }

    @Test
    fun `service rows apply stored instance theme mode`() {
        val snapshot = TaskWidgetSnapshot(
            totalCount = 1, nearCount = 0, overdueCount = 0, summary = "1 task",
            items = listOf(TaskWidgetItem(11L, "First", "1h", TaskWidgetStatus.NORMAL)),
        )
        val store = TaskWidgetConfigurationStore.from(context)
        for (mode in listOf(TaskWidgetThemeMode.LIGHT, TaskWidgetThemeMode.DARK)) {
            store.write(42, TaskWidgetKind.SQUARE, TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).copy(themeMode = mode))
            val factory = TaskWidgetCollectionRemoteViewsFactory(context, 42, snapshotLoader = { snapshot })
            factory.onDataSetChanged()
            val row = factory.getViewAt(0)!!.apply(context, FrameLayout(context))
            val expected = TaskWidgetThemeResolver.resolve(context, snapshot.visualState, themeMode = mode)
            assertEquals(expected.textPrimary, row.findViewById<TextView>(R.id.widget_task_collection_row_title).currentTextColor)
            assertEquals(expected.normalRow.timeText, row.findViewById<TextView>(R.id.widget_task_collection_row_time).currentTextColor)
            factory.onDestroy()
        }
        store.delete(intArrayOf(42))
    }

    private companion object {
        val TEST_ROW = TaskWidgetRowColors(
            base = 0xFFFFFFFF.toInt(),
            tint = 0,
            highlight = 0,
            indicator = 0xFF3482FF.toInt(),
            timeText = 0x99000000.toInt(),
        )
        val TEST_COLORS = TaskWidgetColors(
            isDark = false,
            backgroundBase = 0,
            backgroundTint = 0,
            backgroundGlow = 0,
            backgroundHighlight = 0,
            decoration = 0,
            badgeBackground = 0,
            brand = 0xFF3482FF.toInt(),
            brandSoft = 0,
            near = 0,
            overdue = 0,
            completed = 0,
            textPrimary = 0xFF191919.toInt(),
            textSecondary = 0x99000000.toInt(),
            normalRow = TEST_ROW,
            nearRow = TEST_ROW,
            overdueRow = TEST_ROW,
        )
    }
}
