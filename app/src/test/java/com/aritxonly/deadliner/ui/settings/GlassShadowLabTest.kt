package com.aritxonly.deadliner.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.GlassShadowPreset
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import com.aritxonly.deadliner.ui.material.glass.*
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassShadowLabTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()

    @Before fun initialize() {
        GlobalUtils.init(app)
        GlobalUtils.updateAppearance(commit = true) { it.copy(glassShadowPreset = GlassShadowPreset.Clean) }
    }
    @After fun reset() {
        GlobalUtils.preferences.edit().remove("glass_shadow_preset").commit()
        GlobalUtils.init(app)
    }

    @Test fun dropdownSelectionPersistsAndUpdatesTheDetachedTopShadowHost() {
        var host: TopBarGlassShadowState? = null
        compose.setContent {
            val appearance by GlobalUtils.appearanceFlow.collectAsState()
            MaterialTheme { MiuixTheme { ProvideMiuixPressFeedback {
                CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(
                    enabled = true, shadowPreset = appearance.glassShadowPreset)) {
                    Column {
                        TopBarGlassShadowHost {
                            host = LocalTopBarGlassShadowHost.current
                            Box(Modifier.padding(32.dp).size(44.dp)
                                .softGlassShadow(CircleShape, false, profile = SoftGlassShadowProfile.TopBarControl)
                                .background(Color.White, CircleShape))
                        }
                        GlassShadowLab(appearance.glassShadowPreset, true) { preset ->
                            GlobalUtils.updateAppearance(commit = true) { it.copy(glassShadowPreset = preset) }
                        }
                    }
                }
            } } }
        }
        compose.onNodeWithText(app.getString(R.string.settings_glass_shadow_preset)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(app.getString(R.string.glass_shadow_balanced_diffuse)).performTouchInput { click() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(GlassShadowPreset.BalancedDiffuse, host!!.shadows.values.single().preset)
            GlobalUtils.init(app)
            assertEquals(GlassShadowPreset.BalancedDiffuse, GlobalUtils.appearancePreferences.glassShadowPreset)
        }
    }

    @Test fun unknownStoredPresetFallsBackWithoutChangingOtherMaterialSettings() {
        val old = GlobalUtils.appearancePreferences
        GlobalUtils.preferences.edit().putString("glass_shadow_preset", "removed-experiment").commit()
        val restored = GlobalUtils.appearancePreferences
        assertEquals(GlassShadowPreset.Default, restored.glassShadowPreset)
        assertEquals(old.advancedMaterialFineTuning, restored.advancedMaterialFineTuning)
        assertEquals(old.advancedMaterialBlurRadius, restored.advancedMaterialBlurRadius)
    }

    @Test
    @Config(qualifiers = "zh-rCN-w800dp-h1200dp-mdpi")
    fun renderEveryPresetOnWhiteUsingTheProductionSample() {
        compose.setContent {
            MaterialTheme { MiuixTheme {
                Column(Modifier.width(760.dp).background(Color(0xFFF3F3F3)).padding(12.dp)) {
                    GlassShadowPreset.entries.chunked(2).forEach { pair ->
                        Row {
                            pair.forEach { preset ->
                                Column(Modifier.weight(1f).padding(4.dp)) {
                                    Text(stringResource(preset.labelRes), style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(start = 20.dp))
                                    GlassShadowSample(preset)
                                }
                            }
                        }
                    }
                }
            } }
        }
        val output = File("build/reports/soft-glass-shadow").apply { mkdirs() }
        File(output, "all-presets.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
