package com.aritxonly.deadliner.ui.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry

/** Only allow actual HTTP(S) download targets to reach the platform installer. */
internal fun updateDownloadUri(state: UpdateState.Available): Uri? = state.downloadUrl.trim().toUri()
    .takeIf { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() }

@Composable
internal fun UpdateActions(state: UpdateState, onCheck: () -> Unit, onInstall: (String) -> Unit, onOpenBrowser: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val materialButton = MaterialButtonDefaults.buttonColors()
    val available = state as? UpdateState.Available
    val download = available?.let(::updateDownloadUri)
    val checking = state == UpdateState.Loading
    val enabled = !checking && (available == null || download != null)
    val material = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val blurColors = material.rememberBlurColors(listOf(
        BlendColorEntry(colors.surfaceContainer.copy(alpha = material.navigationTintAlpha * 0.55f)),
    ))
    // Sample the page recorder supplied by AdaptiveMaterialScaffold; never record this bar.
    val background = if (material.enabled && backdrop != null) Modifier.advancedTextureBlur(
        advancedMaterial = material, backdrop = backdrop, shape = RectangleShape, colors = blurColors,
    ) else Modifier.background(colors.surface)
    val density = LocalDensity.current
    val navigationInsets = WindowInsets.navigationBars
    // Gesture navigation leaves room for a thin handle. Keep the full navigation inset
    // when it contains tappable system buttons; only trim excess clearance for gestures.
    val gestureInsetReduction = if (WindowInsets.tappableElement.getBottom(density) > 0) 0.dp
        else minOf(8.dp, with(density) { navigationInsets.getBottom(this).toDp() } / 2)
    Column(Modifier.fillMaxWidth().then(background)
        .windowInsetsPadding(navigationInsets.exclude(WindowInsets(bottom = gestureInsetReduction)))
        .padding(start = SettingsItemDefaults.HorizontalPadding, end = SettingsItemDefaults.HorizontalPadding, top = 4.dp)
        .testTag("update-actions"), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { if (available != null) download?.let { onInstall(it.toString()) } else onCheck() },
            enabled = enabled,
            modifier = Modifier.widthIn(max = 568.dp).fillMaxWidth().heightIn(min = 48.dp).testTag("update-primary")
                .miuixPressFeedback(enabled = enabled, overlay = false),
            colors = materialButton,
        ) {
            Text(stringResource(when {
                checking -> R.string.update_checking_button
                available != null -> R.string.update_and_install
                state is UpdateState.Error -> R.string.update_retry
                else -> R.string.settings_check_for_updates
            }), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                color = if (enabled) materialButton.contentColor else materialButton.disabledContentColor)
        }
        TextButton(
            onClick = { onOpenBrowser(download?.toString() ?: DeadlinerReleasesUrl) },
            enabled = !checking,
            modifier = Modifier.widthIn(max = 568.dp).fillMaxWidth().heightIn(min = 44.dp).testTag("update-secondary")
                .miuixPressFeedback(enabled = !checking, overlay = false),
            colors = MaterialButtonDefaults.textButtonColors(containerColor = Color.Transparent,
                contentColor = colors.primary, disabledContainerColor = Color.Transparent,
                disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.38f)),
        ) {
            Text(stringResource(when {
                available != null && download != null -> R.string.download_from_browser
                state is UpdateState.Error || available != null -> R.string.update_open_releases
                else -> R.string.update_release_history
            }), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
