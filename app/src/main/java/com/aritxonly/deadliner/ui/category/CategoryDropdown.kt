package com.aritxonly.deadliner.ui.category

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.DropdownSourceValue
import com.aritxonly.deadliner.ui.base.GlassDropdownMotion
import com.aritxonly.deadliner.ui.base.SoftGlassDropdownPopup
import com.aritxonly.deadliner.ui.base.formContainerBackground
import com.aritxonly.deadliner.ui.settings.SettingsItemDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One UID-based menu for full editor rows and compact quick-add anchors. */
@Composable
internal fun CategoryDropdownMenu(
    categories: List<CategoryPresentation>,
    categoryUid: String?,
    onSelected: (String?) -> Unit,
    onManageCategories: () -> Unit,
    enabled: Boolean = true,
    content: @Composable (anchor: Modifier, open: () -> Unit, active: Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var sourceHidden by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Rect.Zero) }
    var openingAnchor by remember { mutableStateOf(Rect.Zero) }
    var manageAfterDismiss by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val items = listOf(stringResource(R.string.category_uncategorized)) + categories.map { it.name } +
        stringResource(R.string.category_manage)
    val selectedIndex = categories.indexOfFirst { it.uid == categoryUid } + 1
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    // Run after composition has removed the collapsed Dialog, not during its exit coroutine.
    LaunchedEffect(manageAfterDismiss, sourceHidden) {
        if (manageAfterDismiss && !sourceHidden) {
            manageAfterDismiss = false
            onManageCategories()
        }
    }
    content(Modifier.onGloballyPositioned {
        anchor = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
    }.graphicsLayer { alpha = GlassDropdownMotion.sourceAlpha(progress.value) }
        .then(if (sourceHidden) Modifier.clearAndSetSemantics {} else Modifier), {
        if (enabled && !sourceHidden) {
            keyboard?.hide()
            focus.clearFocus()
            openingAnchor = anchor
            sourceHidden = true
            expanded = true
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }, sourceHidden)
    SoftGlassDropdownPopup(
        show = expanded,
        title = stringResource(R.string.category_select),
        anchor = openingAnchor,
        items = items,
        selectedIndex = selectedIndex,
        progress = progress,
        itemLeading = { index ->
            CategoryDropdownIcon(categories.getOrNull(index - 1), management = index == items.lastIndex)
        },
        itemTextColor = { index -> categories.getOrNull(index - 1)?.let {
            CategoryPresentationRegistry.color(it.colorToken)
        } },
        onDismiss = { expanded = false },
        onDismissFinished = { sourceHidden = false },
        onSelect = { index ->
            if (expanded) {
                expanded = false
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                when {
                    index == items.lastIndex -> manageAfterDismiss = true
                    index == 0 -> if (categoryUid != null) onSelected(null)
                    else -> categories.getOrNull(index - 1)?.let {
                        if (it.uid != categoryUid) onSelected(it.uid)
                    }
                }
            }
        },
    )
}

@Composable
internal fun CategoryDropdownField(
    categoryUid: String?,
    categories: List<CategoryPresentation>,
    onSelected: (String?) -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = categories.firstOrNull { it.uid == categoryUid }
    val value = category?.name ?: stringResource(R.string.category_uncategorized)
    CategoryDropdownMenu(categories, categoryUid, onSelected, onManageCategories) { anchor, open, active ->
        BasicComponent(
            title = stringResource(R.string.category_select),
            modifier = modifier.fillMaxWidth()
                .formContainerBackground(dimensionResource(R.dimen.item_corner_radius))
                .semantics { stateDescription = value },
            insideMargin = SettingsItemDefaults.InsideMargin,
            role = Role.DropdownList,
            holdDownState = active,
            onClick = open,
            endActions = {
                DropdownSourceValue(value,
                    category?.let { CategoryPresentationRegistry.color(it.colorToken) }
                        ?: MiuixTheme.colorScheme.onSurfaceVariantActions,
                    Modifier.weight(1f, fill = false).then(anchor),
                    leading = { CategoryDropdownIcon(category) })
            },
        )
    }
}
