package com.aritxonly.deadliner.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalFloatingNavigationBottomLift = staticCompositionLocalOf { 0.dp }

private val MyHyperModifierSettingsUri =
    Uri.parse("content://com.aritxonly.myhypermodifier.settings")

/** Returns the shared HyperGlassify lift, or zero when the optional module is unavailable. */
@Composable
internal fun rememberMyHyperModifierNavigationLift(enabled: Boolean): Dp {
    val context = LocalContext.current.applicationContext
    var lift by remember(context) { mutableFloatStateOf(0f) }
    LaunchedEffect(context, enabled) {
        lift = if (enabled) withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.call(
                    MyHyperModifierSettingsUri,
                    "get_settings",
                    null,
                    null,
                )?.getFloat("hyper_glassify_hidden_navigation_lift", 0f) ?: 0f
            }.getOrDefault(0f).coerceIn(0f, 48f)
        } else 0f
    }
    return lift.dp
}
