package com.aritxonly.deadliner.ui.detail

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskSubtask
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as darkMiuixColors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeadlineDetailLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()
    private fun label(id: Int) = app.getString(id)
    private val category = CategoryPresentation("study", "学习", "education", "#7C68DD", false, 0)
    private val task = Task("detail", "扫描就读证明", "后天之前完成", "2026-10-17T10:58:44",
        "2026-10-19T09:00:00", categoryUid = "study", createdAt = "", updatedAt = "")

    @Before fun initialize() { GlobalUtils.init(app) }

    @Composable private fun TestTheme(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme()) { MiuixTheme(colors = darkMiuixColors(), content = content) }
    }

    @Test fun badgesShareHeightAndEditHasCompactInkAndUsableTouchTarget() {
        var edits = 0
        compose.setContent { TestTheme {
            Column(Modifier.width(360.dp).testTag("meta")) {
                DetailTaskMetaCard(task, listOf(category), onEdit = { edits++ })
            }
        } }
        val state = compose.onNodeWithTag("detail-state-badge").getUnclippedBoundsInRoot()
        val categoryBounds = compose.onNodeWithTag("detail-category-badge").getUnclippedBoundsInRoot()
        assertEquals((state.bottom-state.top).value, (categoryBounds.bottom-categoryBounds.top).value, .5f)
        val edit = compose.onNodeWithText(label(R.string.edit))
        val ink = compose.onNodeWithText(label(R.string.edit), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val heading = compose.onNodeWithText(label(R.string.detail_task_info_title), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(((heading.top + heading.bottom) / 2).value, ((ink.top + ink.bottom) / 2).value, .5f)
        val start = compose.onNodeWithText(app.getString(R.string.label_start_time,
            java.time.LocalDateTime.parse(task.startAt).format(java.time.format.DateTimeFormatter
                .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM).withLocale(java.util.Locale.getDefault()))))
            .getUnclippedBoundsInRoot()
        assertEquals(18f, start.left.value, .5f)
        edit.performClick()
        assertEquals(1, edits)
        capture("meta")
    }

    @Test fun composerUsesInlineIconsAndKeepsContinuousAdding() {
        val tasks = mutableStateListOf<TaskSubtask>()
        compose.setContent { TestTheme {
            DetailPlanCard("detail", tasks, onAdd = {
                tasks.add(TaskSubtask("${tasks.size}", "detail", it, createdAt = "", updatedAt = ""))
            }, onToggle = {}, onDelete = {}, onUpdate = { _, _ -> })
        } }
        compose.onNodeWithText(label(R.string.detail_plan_add)).performClick()
        compose.onNodeWithText(label(R.string.cancel)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.detail_plan_add)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("绘制原型")
        val field = compose.onNode(hasSetTextAction()).getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithContentDescription(label(R.string.detail_plan_add)).getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithContentDescription(label(R.string.cancel)).getUnclippedBoundsInRoot()
        assertEquals(((field.top+field.bottom)/2).value, ((confirm.top+confirm.bottom)/2).value, 1f)
        assertEquals(((confirm.top+confirm.bottom)/2).value, ((cancel.top+cancel.bottom)/2).value, 1f)
        capture("composer")
        compose.onNodeWithContentDescription(label(R.string.detail_plan_add)).performClick()
        compose.onNodeWithText("绘制原型").assertExists()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("整理评审")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals(listOf("绘制原型", "整理评审"), tasks.map { it.content })
        compose.onNodeWithContentDescription(label(R.string.cancel)).performClick()
        compose.onNodeWithText(label(R.string.detail_plan_add)).assertExists()
    }

    @Test fun detailShowsPrototypeBarAndMovesSecondaryActionsIntoMore() {
        compose.setContent { TestTheme {
            DeadlineDetailScreen(app, app, task, {}, {}, listOf(category), {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})
        } }
        compose.onNodeWithText(label(R.string.mark_complete)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.detail_focus)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.give_up_task)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.delete)).assertDoesNotExist()
        val plan = compose.onNodeWithText(label(R.string.detail_plan_title), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val note = compose.onNodeWithText(label(R.string.detail_note_title), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals((note.bottom - note.top).value, (plan.bottom - plan.top).value, .5f)
        capture("detail")
        compose.onNodeWithContentDescription(label(R.string.detail_more_actions)).performClick()
        compose.onNodeWithText(label(R.string.give_up_task)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.delete)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.save_and_add_to_calendar)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.give_up_task)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val menu = compose.onNode(SemanticsMatcher.expectValue(
            SemanticsProperties.PaneTitle, label(R.string.detail_more_actions)))
        val image = menu.captureToImage().asAndroidBitmap()
        File("build/outputs/detail-review/dropdown.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText(label(R.string.give_up_task)).performClick()
        compose.onNodeWithText(label(R.string.confirm_give_up_title)).assertIsDisplayed()
    }

    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val path = File("build/outputs/detail-review/$name.png")
        path.parentFile!!.mkdirs()
        path.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
