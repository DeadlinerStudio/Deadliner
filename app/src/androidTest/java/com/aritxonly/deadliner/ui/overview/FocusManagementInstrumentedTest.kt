@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aritxonly.deadliner.ui.overview

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.focus.FocusTimerSetupSheet
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.domain.focus.*
import com.deadliner.domain.focus.management.*
import com.deadliner.domain.task.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Read-only native component scene. Does not start timers or mutate the device's business data. */
@RunWith(AndroidJUnit4::class)
class FocusManagementInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<OverviewActivity>()
    private val data by lazy {
        val stamp = "2026-10-14T08:00:00Z"
        val session = FocusSession("fixture", "fixture-target", "原名称", FocusTimerMode.STOPWATCH,
            startedAt = stamp, endedAt = "2026-10-14T08:40:00Z", durationSeconds = 2400,
            completionKind = FocusCompletionKind.FOCUS_ONLY, createdAt = stamp)
        FocusManagementEngine(listOf(session), listOf(Task("fixture-target", "产品方案", createdAt = stamp, updatedAt = stamp)),
            emptyList(), emptyList(), TimeZone.UTC).snapshot(LocalDate.parse("2026-10-14"))
    }
    @Test fun glassFabSelectsBoundTargetAndKeepsModeAcrossCustomDialog() {
        mount()
        ui.onNodeWithTag("fixture-glass-fab").performClick()
        ui.waitUntil(8000) { ui.onAllNodesWithText("产品方案").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("产品方案").performClick()
        ui.onNodeWithText(ui.activity.getString(R.string.focus_countdown_mode)).performClick()
        ui.onNodeWithText("25:00").assertIsDisplayed()
        capture("focus-native-setup")
        ui.onNodeWithText(ui.activity.getString(R.string.focus_custom_minutes, 25)).performClick()
        ui.onNode(hasSetTextAction()).performTextReplacement("181")
        ui.onNodeWithText(ui.activity.getString(R.string.accept)).assertIsNotEnabled()
        ui.onNode(hasSetTextAction()).performTextReplacement("45")
        ui.onNodeWithText(ui.activity.getString(R.string.accept)).performClick()
        ui.onNodeWithText("45:00").assertIsDisplayed()
    }
    @Test fun historicalDetailKeepsTitleAndRejectsInflatingDuration() {
        mount()
        ui.onNodeWithText("产品方案").performScrollTo().performClick()
        ui.waitUntil(8000) { ui.onAllNodesWithText(ui.activity.getString(R.string.focus_hub_record_detail)).fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("原名称").assertIsDisplayed()
        capture("focus-native-record")
        ui.onNodeWithText(ui.activity.getString(R.string.focus_adjust)).performClick()
        ui.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("41")
        ui.onNodeWithTag("focus-management-save").assertIsNotEnabled() // The form remains available; no real mutation is submitted.
        capture("focus-native-adjust")
    }
    private fun mount() {
        ui.runOnUiThread { ui.activity.setContent { DeadlinerTheme { Scene() } } }
        ui.waitUntil(8000) { ui.onAllNodesWithTag("fixture-glass-fab").fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
        capture("focus-native-overview")
    }
    @Composable private fun Scene() {
        val color = MaterialTheme.colorScheme.surface
        val backdrop = if (LocalAdvancedMaterialSpec.current.enabled) rememberLayerBackdrop { drawRect(color) } else null
        var show by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf<FocusStartTargetOption?>(null) }
        var detail by remember { mutableStateOf(false) }
        AdaptiveMaterialScaffold(topBar = { TopAppBar("专注管理", mode = TopAppBarStyle.LARGE) }) { padding ->
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier), contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp, bottom = 116.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { FocusManagementToday(data) }; item { FocusManagementTotal(data) }; item { FocusManagementWeek(data) }
                    item { FocusManagementRecordRow(data.records.single()) { detail = true } }
                }
                SoftGlassFloatingActionButton(onClick = { show = true }, modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(20.dp).testTag("fixture-glass-fab"), size = 64.dp, backdrop = backdrop) {
                    Icon(iconResource(R.drawable.msr_add_wght400), contentDescription = "开始专注")
                }
                FocusManagementRecordSheet(data.records.single(), detail, { detail = false }, {})
            }
            FocusTimerSetupSheet(show, selected?.target?.uid.orEmpty(), selected?.title.orEmpty(), emptyList(),
                false, null, false, {}, { _, _ -> }, { show = false }, {}, managementTargets = data.startTargets,
                onSelectManagementTarget = { selected = it })
        }
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(ui.activity.getExternalFilesDir(null), "focus-management-acceptance").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
