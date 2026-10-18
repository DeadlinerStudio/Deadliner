package com.aritxonly.deadliner.ui.lifi

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritxonly.deadliner.OverviewActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aritxonly.deadliner.ui.iconResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationDock
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationAction
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingTabItem
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingTabLayout
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UiStyle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs on the disposable read-only emulator. Never sends prompts or writes business data. */
@RunWith(AndroidJUnit4::class)
class LifiPageInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<OverviewActivity>()

    @Test fun independentPageKeepsDraftAndExpandsTabsWhenTheAddActionHides() {
        val original = GlobalUtils.appearanceFlow.value
        try {
            ui.runOnUiThread {
                GlobalUtils.updateAppearance { it.copy(uiStyle = UiStyle.Miuix, useAdvancedMaterial = true) }
                ui.activity.setContent { DeadlinerTheme { LifiTestScene() } }
            }
            waitText(ui.activity.getString(R.string.lifi))
            ui.onNodeWithText(ui.activity.getString(R.string.lifi)).performClick()
            waitText("把想法告诉我，\n我们一起往前走。")
            ui.waitUntil(10_000) { LifiSession.ready }
            ui.waitForIdle()
            ui.onNodeWithContentDescription(ui.activity.getString(R.string.add_task)).assertDoesNotExist()
            val tabs = ui.onNodeWithTag("floating-navigation-tabs").fetchSemanticsNode().boundsInRoot
            val root = ui.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue("Expanded tab capsule must be centered", kotlin.math.abs(tabs.center.x - root.center.x) < 3f)
            capture("native-lifi-page")
            ui.onNodeWithText("安排一个任务").performClick()
            ui.onNode(hasSetTextAction()).assertTextEquals("明天下午 3 点和产品开评审会")
            ui.onNodeWithText(ui.activity.getString(R.string.main_section_list)).performClick()
            ui.waitForIdle()
            ui.onNodeWithContentDescription(ui.activity.getString(R.string.add_task)).assertExists()
            ui.onNodeWithText(ui.activity.getString(R.string.lifi)).performClick()
            ui.waitForIdle()
            ui.onNode(hasSetTextAction()).assertTextEquals("明天下午 3 点和产品开评审会")
            ui.onNodeWithContentDescription("对话历史").performClick()
            waitText("对话历史")
            ui.onNodeWithContentDescription("关闭对话历史").performClick()
            ui.onNodeWithText("记忆与偏好  ›").performClick()
            waitText("记忆与偏好")
            capture("native-lifi-memory")
            ui.onNodeWithContentDescription("关闭记忆与偏好").performClick()
        } finally {
            ui.runOnUiThread { LifiSession.editDraft(""); GlobalUtils.updateAppearance { original } }
        }
    }
    private fun waitText(text: String) {
        ui.waitUntil(15_000) { ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.cacheDir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}

@Composable
private fun LifiTestScene() {
    var selected by remember { mutableStateOf("list") }
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val items = listOf("list" to R.string.main_section_list, "overview" to R.string.overview, "capture" to R.string.capture_title, "lifi" to R.string.lifi).map { (key, label) ->
        val icon = iconResource(if (key == "lifi") R.drawable.ic_lifi else R.drawable.msr_menu_wght400)
        MiuixFloatingTabItem(key, context.getString(label), icon, icon)
    }
    Box(Modifier.fillMaxSize()) {
        if (selected == "lifi") LifiAIPage(navigationOverlaysContent = true)
        else Text("测试列表", Modifier.align(Alignment.Center))
        MiuixFloatingNavigationDock(items, selected, { selected = it.key },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            detachedAction = MiuixFloatingNavigationAction(R.drawable.msr_add_wght400, context.getString(R.string.add_task), colors.primary, colors.onPrimary, onClick = {}, visible = selected != "lifi"),
            layout = MiuixFloatingTabLayout.Stacked,
        )
    }
}
