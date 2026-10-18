package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun SoftGlassDropdownOptions(
    items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, enabled: Boolean,
    itemLeading: (@Composable (Int) -> Unit)? = null,
    itemTextColor: ((Int) -> Color?)? = null,
    itemRole: Role = Role.RadioButton,
) {
    val colors = DropdownDefaults.dropdownColors()
    val scroll = rememberScrollState()
    val initialFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    LaunchedEffect(enabled) { if (enabled) initialFocus.requestFocus() }
    val itemShape = remember { RoundedCornerShape(GlassDropdownMotion.HoverCornerRadius) }
    Box(Modifier.padding(GlassDropdownMotion.ItemInset).width(IntrinsicSize.Max).verticalScroll(scroll)) {
        Column(Modifier.fillMaxWidth().then(if (itemRole == Role.RadioButton) Modifier.selectableGroup() else Modifier)) {
            items.forEachIndexed { index, label ->
                val interactions = remember { MutableInteractionSource() }
                val pressed by interactions.collectIsPressedAsState()
                val focused by interactions.collectIsFocusedAsState()
                val active = enabled && (pressed || (focused && inputMode.inputMode == InputMode.Keyboard))
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .then(if (index == (selectedIndex.takeIf { it in items.indices } ?: 0))
                        Modifier.focusRequester(initialFocus) else Modifier)
                    .background(if (active) colors.contentColor.copy(alpha = GlassDropdownMotion.HoverAlpha)
                        else Color.Transparent, itemShape)
                    .then(if (itemRole == Role.Button) Modifier.clickable(enabled = enabled,
                        interactionSource = interactions, indication = null,
                        role = Role.Button, onClick = { onSelect(index) }) else Modifier.selectable(
                        selected = index == selectedIndex, enabled = enabled,
                        interactionSource = interactions, indication = null,
                        role = itemRole, onClick = { onSelect(index) }))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    itemLeading?.let { leading ->
                        leading(index)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp,
                        color = itemTextColor?.invoke(index)
                            ?: if (index == selectedIndex) colors.selectedContentColor else colors.contentColor)
                    if (itemRole != Role.Button) Spacer(Modifier.width(12.dp))
                    if (itemRole != Role.Button && index == selectedIndex) {
                        Icon(com.aritxonly.deadliner.ui.materialSymbolPainter("check"), contentDescription = null,
                            modifier = Modifier.size(DropdownDefaults.CheckIconSize)
                                .testTag("dropdown-selected-indicator"),
                            tint = colors.selectedIndicatorColor)
                    } else if (itemRole != Role.Button) Spacer(Modifier.size(DropdownDefaults.CheckIconSize))
                }
            }
        }
    }
}
