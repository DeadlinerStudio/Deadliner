package com.aritxonly.deadliner.widgets.glance

import android.app.Application
import android.content.res.Configuration
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetRenderStateStore
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetSystemThemeObserverTest {
    @Test
    fun `application callbacks refresh unchanged widget data without an activity`() {
        val app = RuntimeEnvironment.getApplication()
        val light = Configuration(app.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        }
        val dark = Configuration(light).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        }
        val store = TaskWidgetRenderStateStore()
        val snapshot = TaskWidgetSnapshot(1, 0, 0, "1 task", emptyList())
        store.publish(snapshot)
        val observer = TaskWidgetSystemThemeObserver(light) {
            store.publish(snapshot, forceRevision = true)
        }
        app.registerComponentCallbacks(observer)
        try {
            app.onConfigurationChanged(Configuration(light).apply { orientation = Configuration.ORIENTATION_LANDSCAPE })
            assertEquals(1L, store.state.value.revision)
            app.onConfigurationChanged(dark)
            assertEquals(2L, store.state.value.revision)
            assertEquals(snapshot, store.state.value.snapshot)
            app.onConfigurationChanged(Configuration(dark))
            assertEquals(2L, store.state.value.revision)
            app.onConfigurationChanged(light)
            assertEquals(3L, store.state.value.revision)
        } finally {
            app.unregisterComponentCallbacks(observer)
        }
    }
}
