package com.aritxonly.deadliner.widgets.glance.ui

import android.appwidget.AppWidgetHostView
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Parcel
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.ImageView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfiguration
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.interaction.TaskWidgetActions
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetRowColors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetCollectionPayloadTest {
    @Test
    fun `scrollable list binds an adapter in the widget host`() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val remoteViews = ScrollableListTestWidget().compose(
            context = context,
            size = DpSize(160.dp, 160.dp),
        )
        val root = remoteViews.apply(context, AppWidgetHostView(context))
        val list = root.findListView()!!
        assertNotNull("Task list adapter was rejected inside nested RemoteViews", list.adapter)
        assertEquals(
            TaskWidgetLayoutMetrics.scrollableVisibleTaskCount(TEST_SNAPSHOT.items.size),
            list.adapter.count,
        )
        assertEquals(
            TEST_SNAPSHOT.items[list.adapter.count - 1].stableId,
            list.adapter.getItemId(list.adapter.count - 1),
        )
    }

    @Test
    fun `host reapply updates task rows without losing the adapter`() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val host = AppWidgetHostView(context)
        val original = ScrollableListTestWidget().compose(context = context, size = DpSize(160.dp, 160.dp))
        val root = original.apply(context, host)
        host.addView(root)
        val updatedSnapshot = TEST_SNAPSHOT.copy(
            totalCount = 1,
            items = listOf(TaskWidgetItem(999L, "Updated task", "2h", TaskWidgetStatus.NEAR)),
        )
        val updated = ScrollableListTestWidget(snapshot = updatedSnapshot)
            .compose(context = context, size = DpSize(160.dp, 160.dp))
        updated.reapply(context, root)

        val list = root.findListView()!!
        assertNotNull(list.adapter)
        assertEquals(1, list.adapter.count)
        assertEquals(999L, list.adapter.getItemId(0))
        val row = list.adapter.getView(0, null, list)
        assertEquals("Updated task", row.findViewById<TextView>(R.id.widget_task_collection_row_title).text)
        assertEquals("2h", row.findViewById<TextView>(R.id.widget_task_collection_row_time).text)
    }

    @Test
    fun `scrollable list keeps card payload bounded`() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val remoteViews = ScrollableListTestWidget().compose(
            context = context,
            size = DpSize(160.dp, 160.dp),
        )
        val parcel = Parcel.obtain()
        try {
            remoteViews.writeToParcel(parcel, 0)
            assertTrue(
                "Scrollable task RemoteViews was ${parcel.dataSize()} bytes",
                parcel.dataSize() < MAX_REMOTE_VIEWS_BYTES,
            )
        } finally {
            parcel.recycle()
        }

        val root = remoteViews.apply(context, AppWidgetHostView(context))
        val list = root.findListView()
        assertNotNull("Scrollable task RemoteViews did not contain a ListView", list)
        val backgroundImage = root.findImageViewWithAlpha(
            TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).backgroundAlpha(),
        )
        assertNotNull(
            "Background image did not retain the official Glance alpha",
            backgroundImage,
        )
        assertTrue(
            "The visible background image subtree should own the card click",
            backgroundImage!!.hasClickableAncestor(),
        )
        assertEquals(
            "The layered card should expose one non-collection click target",
            1,
            root.countViewsWithOnClickListeners(),
        )
    }

    @Test
    fun `thousand short titled tasks keep widget payload bounded`() = runBlocking<Unit> {
        assertLargeListPayloadBounded(title = { index -> "Task $index" })
    }

    @Test
    fun `thousand long titled tasks keep widget payload bounded`() = runBlocking<Unit> {
        val longTitle = "Long task title with many words 长标题任务 ".repeat(40)
        val root = assertLargeListPayloadBounded(title = { index -> "$index $longTitle" })
        val list = root.findListView()!!
        val title = list.adapter.getView(0, null, list)
            .findViewById<TextView>(R.id.widget_task_collection_row_title).text.toString()
        assertEquals(TaskWidgetLayoutMetrics.boundedRowTitle("0 $longTitle"), title)
        assertTrue(title.length <= TaskWidgetLayoutMetrics.ROW_TITLE_CHAR_LIMIT + 1)
    }

    private suspend fun assertLargeListPayloadBounded(title: (Int) -> String): View {
        val context: Context = RuntimeEnvironment.getApplication()
        val snapshot = TaskWidgetSnapshot(
            totalCount = LARGE_TASK_COUNT, nearCount = 0, overdueCount = 0, summary = "1000 tasks",
            items = List(LARGE_TASK_COUNT) { index ->
                TaskWidgetItem(
                    stableId = index.toLong() + 10,
                    title = title(index),
                    timeDescription = "12d",
                    status = TaskWidgetStatus.entries[index % TaskWidgetStatus.entries.size],
                    categoryColorArgb = 0xFF000000.toInt() or index,
                )
            },
        )
        val remoteViews = ScrollableListTestWidget(snapshot = snapshot).compose(
            context = context,
            size = DpSize(160.dp, 160.dp),
        )
        val parcel = Parcel.obtain()
        try {
            remoteViews.writeToParcel(parcel, 0)
            println("Large scrollable task RemoteViews: ${parcel.dataSize()} bytes")
            assertTrue(
                "Scrollable task RemoteViews was ${parcel.dataSize()} bytes",
                parcel.dataSize() < SAFE_UPDATE_BYTES,
            )
        } finally {
            parcel.recycle()
        }

        val root = remoteViews.apply(context, AppWidgetHostView(context))
        val list = root.findListView()!!
        assertNotNull(list.adapter)
        assertEquals(TaskWidgetLayoutMetrics.SCROLLABLE_TASK_LIMIT, list.adapter.count)
        assertEquals(10L, list.adapter.getItemId(0))
        assertEquals(
            TaskWidgetLayoutMetrics.SCROLLABLE_TASK_LIMIT + 9L,
            list.adapter.getItemId(list.adapter.count - 1),
        )
        return root
    }

    @Test
    fun `custom transparency keeps content opaque and background clickable`() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        for (kind in TaskWidgetKind.entries) {
            for (percent in listOf(0, 50, 100)) {
                val configuration = TaskWidgetConfiguration.defaultFor(kind)
                    .copy(backgroundTransparencyPercent = percent)
                val remoteViews = ScrollableListTestWidget(configuration).compose(
                    context = context,
                    size = DpSize(160.dp, 160.dp),
                )
                val root = remoteViews.apply(context, AppWidgetHostView(context))
                val background = root.findImageViewWithAlpha(configuration.backgroundAlpha())
                assertNotNull("Missing background for $kind at $percent%", background)
                assertTrue(background!!.hasClickableAncestor())
                assertEquals(1, root.countViewsWithOnClickListeners())
                var content: View? = root.findListView()
                assertNotNull(content)
                while (content != null) {
                    assertEquals("Content ancestor must remain opaque", 1f, content.alpha, 0.001f)
                    content = content.parent as? View
                }
            }
        }
    }

    private fun View.findListView(): ListView? {
        if (this is ListView) return this
        if (this !is ViewGroup) return null
        repeat(childCount) { index ->
            getChildAt(index).findListView()?.let { return it }
        }
        return null
    }

    private fun View.findImageViewWithAlpha(expectedAlpha: Float): ImageView? {
        val expectedImageAlpha = (expectedAlpha * 255).roundToInt()
        if (
            this is ImageView &&
            (
                abs(alpha - expectedAlpha) < 0.001f ||
                    imageAlpha == expectedImageAlpha ||
                    drawable?.alpha == expectedImageAlpha
                )
        ) {
            return this
        }
        if (this !is ViewGroup) return null
        repeat(childCount) { index ->
            getChildAt(index).findImageViewWithAlpha(expectedAlpha)?.let { return it }
        }
        return null
    }

    private fun View.countViewsWithOnClickListeners(): Int {
        var count = if (hasOnClickListeners()) 1 else 0
        if (this is ViewGroup) {
            repeat(childCount) { index ->
                count += getChildAt(index).countViewsWithOnClickListeners()
            }
        }
        return count
    }

    private fun View.hasClickableAncestor(): Boolean {
        var current = parent as? View
        while (current != null) {
            if (current.hasOnClickListeners()) return true
            current = current.parent as? View
        }
        return false
    }

    private class ScrollableListTestWidget(
        private val configuration: TaskWidgetConfiguration = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE),
        private val snapshot: TaskWidgetSnapshot = TEST_SNAPSHOT,
    ) : GlanceAppWidget(errorUiLayout = 0) {
        override val sizeMode: SizeMode = SizeMode.Exact

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideContent {
                val openAppAction = TaskWidgetActions.openAppAction(context, TEST_APP_WIDGET_ID)
                TaskWidgetLayeredCard(
                    background = ImageProvider(
                        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.WHITE)
                        },
                    ),
                    backgroundAlpha = configuration.backgroundAlpha(),
                    cornerRadiusDp = 22,
                    openAppAction = openAppAction,
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        ScrollableCompactTaskList(
                            snapshot = snapshot,
                            colors = TEST_COLORS,
                            appWidgetId = TEST_APP_WIDGET_ID,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val TEST_SNAPSHOT = TaskWidgetSnapshot(
            totalCount = 100, nearCount = 0, overdueCount = 0, summary = "100 tasks",
            items = List(100) { index ->
                TaskWidgetItem(index.toLong() + 10, "Task $index", "1h", TaskWidgetStatus.NORMAL)
            },
        )
        const val MAX_REMOTE_VIEWS_BYTES = 1024 * 1024
        // The 1 MiB Binder buffer is shared by the whole process, so one update keeps a wide margin.
        const val SAFE_UPDATE_BYTES = 256 * 1024
        const val LARGE_TASK_COUNT = 1_000
        const val TEST_APP_WIDGET_ID = 42
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
