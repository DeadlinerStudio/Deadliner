package com.aritxonly.deadliner.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import androidx.compose.ui.test.junit4.createComposeRule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-w411dp-h891dp-mdpi")
class SettingsRowAlignmentTest {
    @get:Rule val compose = createComposeRule()
    @Before fun initialize() { GlobalUtils.init(RuntimeEnvironment.getApplication()) }
    @Test fun settingContentMatchesDropdownInsets() = assertAligned(LayoutDirection.Ltr)
    @Test fun settingContentMatchesDropdownInsetsInRtl() = assertAligned(LayoutDirection.Rtl)

    @Test fun homeKeeps16dpAndOnlyNavigationDetailRowsShowChevron() {
        compose.setContent {
            MaterialTheme { MiuixTheme {
                Column(Modifier.width(360.dp)) {
                    SettingItem("Home", "Home summary", horizontalPadding = 16.dp)
                    SettingItem("Child", "Child summary")
                    SettingsDetailTextButtonItem(headlineText = "Next page", supportingText = "Navigation",
                        navigates = true, onClick = {})
                    SettingsDetailTextButtonItem(headlineText = "Run action", supportingText = "Action",
                        onClick = {})
                }
            } }
        }
        assertEquals(16f, compose.onNodeWithText("Home", useUnmergedTree = true)
            .getUnclippedBoundsInRoot().left.value, .5f)
        assertEquals(20f, compose.onNodeWithText("Child", useUnmergedTree = true)
            .getUnclippedBoundsInRoot().left.value, .5f)
        compose.onAllNodesWithTag("settings-navigation-chevron", useUnmergedTree = true).assertCountEquals(3)
    }

    private fun assertAligned(direction: LayoutDirection) {
        val app = RuntimeEnvironment.getApplication()
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MaterialTheme { MiuixTheme {
                    Column(Modifier.width(360.dp).testTag("panel").verticalScroll(rememberScrollState())) {
                        SettingsDropdownItem("Dropdown", listOf(RadioOption(0, R.string.sync_interval_manual)), 0, {})
                        SettingItem("Detail row", "Detail summary", trailingContent = {
                            Box(Modifier.size(20.dp).testTag("detail-end"))
                        })
                        SettingsSwitchItem(R.string.settings_enable_cloud_sync, false, {})
                        SettingsDetailSwitchItem(R.string.settings_web_wifi_only,
                            supportingText = R.string.settings_support_web_wifi_only, checked = false, onCheckedChange = {})
                        SettingsDetailSwitchItem(R.string.settings_web_charging_only,
                            supportingRawText = "Charging summary", checked = false, onCheckedChange = {})
                        SettingsDetailTextButtonItem(headlineText = "Button row", supportingText = "Button summary", onClick = {})
                        SettingsDetailTextButtonItem(headline = R.string.settings_sync_provider,
                            supporting = R.string.settings_support_webdav, onClick = {})
                        SettingsTextButtonItem(text = R.string.save, onClick = {})
                        SettingsSliderItemWithLabel(R.string.settings_tombstone_retention, 30f, 0f..90f, onValueChange = {})
                        SettingsRadioGroupItem(listOf(RadioOptionText(0, "Radio row")), 0, {}, showDivider = false)
                        SettingsRadioGroupItem(listOf(RadioOption(0, R.string.settings_more)), 0, {}, showDivider = false)
                    }
                } }
            }
        }
        val dropdown = compose.onNodeWithText("Dropdown", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag("panel").getUnclippedBoundsInRoot()
        if (direction == LayoutDirection.Ltr) assertEquals(20f, (dropdown.left - panel.left).value, .5f)
        else assertEquals(20f, (panel.right - dropdown.right).value, .5f)
        val labels = listOf("Detail row", "Detail summary", "Button row", "Button summary", "Charging summary", "Radio row") +
            listOf(R.string.settings_enable_cloud_sync, R.string.settings_web_wifi_only,
                R.string.settings_web_charging_only, R.string.settings_sync_provider, R.string.save,
                R.string.settings_tombstone_retention, R.string.settings_more).map(app::getString)
        labels.forEach { label ->
            val bounds = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
            if (direction == LayoutDirection.Ltr) assertEquals(label, dropdown.left.value, bounds.left.value, .5f)
            else assertEquals(label, dropdown.right.value, bounds.right.value, .5f)
        }
        val dropdownEnd = compose.onNodeWithTag("dropdown-source-value", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val rowEnd = compose.onNodeWithTag("detail-end", useUnmergedTree = true).getUnclippedBoundsInRoot()
        if (direction == LayoutDirection.Ltr) assertEquals(20f, (panel.right - dropdownEnd.right).value, .5f)
        else assertEquals(20f, (dropdownEnd.left - panel.left).value, .5f)
        if (direction == LayoutDirection.Ltr) assertEquals(dropdownEnd.right.value, rowEnd.right.value, .5f)
        else assertEquals(dropdownEnd.left.value, rowEnd.left.value, .5f)
    }
}
