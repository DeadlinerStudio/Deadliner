package com.aritxonly.deadliner.ui.base.feedback

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.aritxonly.deadliner.ui.base.LocalTopBarButtonMaterialHosted
import top.yukonga.miuix.kmp.utils.MiuixIndication
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

internal val DefaultClickFeedback = SinkFeedback()

/** Rows keep their geometry and use the official animated, non-radial state layer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProvideMiuixPressFeedback(content: @Composable () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface
    val indication = remember(color) { MiuixIndication(color) }
    CompositionLocalProvider(
        LocalIndication provides indication,
        LocalRippleConfiguration provides null,
        content = content,
    )
}

/** Reuse one source for Sink and the optional state layer, without adding a click handler. */
@Composable
internal fun Modifier.miuixPressFeedback(
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    overlay: Boolean = true,
): Modifier = if (LocalTopBarButtonMaterialHosted.current) this
else miuixControlFeedback(enabled, shape, overlay)

/** For hosts that already contain a clickable child, including glass navigation buttons. */
@Composable
internal fun Modifier.miuixControlFeedback(
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    overlay: Boolean = true,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val color = MaterialTheme.colorScheme.onSurface
    val indication = remember(color) { MiuixIndication(color) }
    return pressable(source, indication = DefaultClickFeedback, enabled = enabled)
        .then(if (overlay) Modifier.clip(shape).indication(source, indication) else Modifier)
}
