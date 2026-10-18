package com.aritxonly.deadliner.ui.focus

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Reuses the project's FAB, including its press motion and material fallback. */
@Composable
internal fun FocusRoundAction(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    size: Dp = 56.dp,
    contentDescription: String = label,
    // These actions live inside captured page content, so use the canonical no-backdrop
    // material. Sampling the page here would record the FAB into its own RenderNode.
    backdrop: LayerBackdrop? = null,
) {
    val colors = MiuixTheme.colorScheme
    val container = if (primary) colors.primary else colors.surfaceContainer
    val foreground = if (primary) colors.onPrimary else colors.onSurfaceContainer
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SoftGlassFloatingActionButton(
            onClick = onClick, enabled = enabled, size = size,
            containerColor = container.copy(alpha = if (enabled) 1f else 0.4f),
            contentColor = foreground, backdrop = backdrop,
        ) {
            androidx.compose.animation.Crossfade(icon, animationSpec = FocusVisuals.motion(), label = "focus-action-symbol") { resource ->
                Icon(iconResource(resource), contentDescription = contentDescription,
                    tint = foreground.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(if (primary) 30.dp else 24.dp))
            }
        }
        Text(label, style = MiuixTheme.textStyles.footnote2.copy(
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal), textAlign = TextAlign.Center,
            color = colors.onSurfaceVariantSummary.copy(alpha = if (enabled) 1f else 0.4f))
    }
}

/** Compact choices use Foundation selection semantics, including disabled states. */
@Composable
internal fun FocusChoiceRow(
    choices: List<String>, selectedIndex: Int, enabled: Boolean, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, segmented: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    Row(modifier.fillMaxWidth().clip(CircleShape)
        .background(if (segmented) colors.surface else Color.Transparent)
        .padding(if (segmented) 4.dp else 0.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        choices.forEachIndexed { index, title ->
            val selected = selectedIndex == index
            val background by animateColorAsState(
                if (selected) colors.primary.copy(alpha = 0.14f) else Color.Transparent,
                animationSpec = FocusVisuals.motion(), label = "focus-choice-$index")
            Box(Modifier.weight(1f).clip(CircleShape).background(background)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
                    onClick = { onSelect(index) })
                .heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(title, style = MiuixTheme.textStyles.footnote1.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal), textAlign = TextAlign.Center,
                    color = (if (selected) colors.primary else colors.onSurfaceVariantSummary)
                        .copy(alpha = if (enabled) 1f else 0.4f))
            }
        }
    }
}
