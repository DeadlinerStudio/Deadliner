package com.aritxonly.deadliner.ui.base

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** MIUIX preference row with an application-owned HyperOS glass popup. */
@Composable
internal fun SoftGlassDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Rect.Zero) }
    var openingAnchor by remember { mutableStateOf(Rect.Zero) }
    var sourceHidden by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val actualEnabled = enabled && items.isNotEmpty()
    val haptic = LocalHapticFeedback.current
    val value = items.getOrNull(selectedIndex).orEmpty()
    val color = if (actualEnabled) MiuixTheme.colorScheme.onSurfaceVariantActions
        else MiuixTheme.colorScheme.disabledOnSecondaryVariant
    LaunchedEffect(actualEnabled) { if (!actualEnabled) expanded = false }
    BasicComponent(
        title = title,
        modifier = modifier.semantics { stateDescription = value },
        enabled = actualEnabled,
        insideMargin = com.aritxonly.deadliner.ui.settings.SettingsItemDefaults.InsideMargin,
        role = Role.DropdownList,
        holdDownState = sourceHidden,
        onClick = {
            openingAnchor = anchor
            sourceHidden = true
            expanded = true
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        },
        endActions = {
            DropdownSourceValue(value, color, Modifier.align(Alignment.CenterVertically)
                .weight(1f, fill = false)
                .testTag("dropdown-source-value")
                .onGloballyPositioned {
                    anchor = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
                }
                .graphicsLayer {
                    // Keep the actual glyphs throughout the morph; no cross-window text handoff.
                    alpha = GlassDropdownMotion.sourceAlpha(progress.value)
                }
                .then(if (sourceHidden) Modifier.clearAndSetSemantics {} else Modifier))
        },
    )
    SoftGlassDropdownPopup(
        show = expanded,
        progress = progress,
        title = title,
        anchor = openingAnchor,
        items = items,
        selectedIndex = selectedIndex,
        onDismiss = { expanded = false },
        onDismissFinished = { sourceHidden = false },
        onSelect = { index ->
            if (expanded) {
                expanded = false
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                if (index != selectedIndex) onSelectedIndexChange(index)
            }
        },
    )
}
