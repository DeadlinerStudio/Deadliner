package com.aritxonly.deadliner.ui.editor

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.ui.quickadd.QuickAddState
import com.deadliner.domain.quickadd.QuickAddProposal
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class EditorQuickAddCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun inlineCardOnlyOffersAiWhenRecognitionNeedsItAndNeverOffersSaveOrAttributes() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope) { _, _ -> QuickAddProposal(title = "交报告", source = "AI", ready = true) }
            state.open("TASK")
            compose.setContent { MaterialTheme { EditorQuickAddCard(state) } }
            compose.onNode(hasSetTextAction()).assertIsDisplayed()
            compose.onNodeWithContentDescription("AI 识别").assertDoesNotExist()
            compose.runOnIdle { state.edit("123"); Snapshot.sendApplyNotifications() }
            compose.onNodeWithText("AI").assertIsDisplayed()
            compose.onNodeWithText("AI").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("AI").assertDoesNotExist()
            listOf("填写表单", "添加草案", "分类", "日历导入").forEach {
                compose.onNodeWithContentDescription(it).assertDoesNotExist()
                compose.onNodeWithText(it).assertDoesNotExist()
            }
        } finally { scope.cancel() }
    }
}
