package com.aritxonly.deadliner.widgets.glance.theme

import android.app.Application
import android.content.res.Configuration
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetColorScheme
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetThemeResolverTest {
    @Test
    fun `forced modes override system mode for every color scheme`() {
        val app = RuntimeEnvironment.getApplication()
        for (systemDark in listOf(false, true)) {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (systemDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val context = app.createConfigurationContext(config)
            for (scheme in TaskWidgetColorScheme.entries) {
                val light = TaskWidgetThemeResolver.resolve(context, TaskWidgetVisualState.NORMAL, scheme, TaskWidgetThemeMode.LIGHT)
                val dark = TaskWidgetThemeResolver.resolve(context, TaskWidgetVisualState.NORMAL, scheme, TaskWidgetThemeMode.DARK)
                val follow = TaskWidgetThemeResolver.resolve(context, TaskWidgetVisualState.NORMAL, scheme)
                assertEquals(false, light.isDark)
                assertEquals(true, dark.isDark)
                assertEquals(if (systemDark) dark else light, follow)
                assertNotEquals(light.backgroundBase, dark.backgroundBase)
                assertNotEquals(light.textPrimary, dark.textPrimary)
                assertNotEquals(light.normalRow.base, dark.normalRow.base)
            }
        }
    }
}
