package com.aritxonly.deadliner.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-w411dp-h891dp-mdpi")
class CloudSyncOptionsTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()
    private fun label(id: Int) = app.getString(id)
    @Before fun initialize() {
        GlobalUtils.init(app)
        resetOptions()
    }
    @After fun resetOptions() {
        GlobalUtils.syncIntervalMinutes = 0
        GlobalUtils.syncWifiOnly = false
        GlobalUtils.syncChargingOnly = false
    }
    @Test fun conditionsAndIntervalAreEditableInlineAndPersistImmediately() {
        compose.setContent {
            var wifi by remember { mutableStateOf(false) }
            var charging by remember { mutableStateOf(false) }
            var interval by remember { mutableIntStateOf(15) }
            MaterialTheme { MiuixTheme { Column {
                CloudSyncAutomationOptions(interval, wifi, charging,
                    onIntervalChange = { interval = it; GlobalUtils.syncIntervalMinutes = it },
                    onWifiOnlyChange = { wifi = it; GlobalUtils.syncWifiOnly = it },
                    onChargingOnlyChange = { charging = it; GlobalUtils.syncChargingOnly = it })
            } } }
        }
        compose.onNodeWithText(label(R.string.settings_web_wifi_only)).performClick()
        compose.onNodeWithText(label(R.string.settings_web_charging_only)).performClick()
        compose.runOnIdle { assertTrue(GlobalUtils.syncWifiOnly); assertTrue(GlobalUtils.syncChargingOnly) }
        compose.onNodeWithText(label(R.string.settings_web_auto_sync_time)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(label(R.string.sync_interval_manual)).performTouchInput { click() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, GlobalUtils.syncIntervalMinutes) }
        compose.onNodeWithText(label(R.string.sync_interval_manual)).assertIsDisplayed()
    }
}
