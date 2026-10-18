package com.aritxonly.deadliner.ui.focus

import top.yukonga.miuix.kmp.squircle.squircleSurface
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.aritxonly.deadliner.ui.base.formContainerBackground
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.iconResource
import com.deadliner.domain.focus.FocusTargetKind
import com.deadliner.domain.focus.management.FocusStartTargetOption
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Targets share a lazy grid; controls remain restricted to the start picker. */
internal fun LazyListScope.managementTargetPicker(
    targets: List<FocusStartTargetOption>, kind: String, query: String, busy: Boolean,
    onKind: (String) -> Unit, onQuery: (String) -> Unit, onSelect: (FocusStartTargetOption) -> Unit,
    selectedUid: String = "", selectedIsHabit: Boolean = false, columns: Int = 2,
) {
    val matches = targets.filter { it.target.kind.name == kind && it.title.contains(query.trim(), ignoreCase = true) }
    item("management-target-controls") {
        Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FocusTargetTabs(kind, enabled = !busy, onKind = onKind)
            FocusTargetSearch(query, onQuery, enabled = !busy)
        }
    }
    item("management-target-summary") {
        Text(stringResource(R.string.focus_hub_target_count, matches.size), Modifier.padding(start = 4.dp, bottom = 12.dp),
            style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
    if (matches.isEmpty()) item("management-target-empty") {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(iconResource(R.drawable.msr_search_wght400), null, Modifier.size(28.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Text(stringResource(if (query.isBlank()) R.string.focus_hub_no_targets else R.string.focus_hub_no_search_results),
                style = MiuixTheme.textStyles.body2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
    val rows = matches.chunked(columns.coerceIn(1, 2))
    items(rows, key = { row -> "targets-${row.first().target.kind}-${row.first().target.uid}" }) { row ->
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { option ->
                val selected = option.target.uid == selectedUid && (option.target.kind == FocusTargetKind.HABIT) == selectedIsHabit
                FocusTargetTile(option, selected, !busy, Modifier.weight(1f).fillMaxHeight()) {
                    focusManager.clearFocus(); keyboard?.hide(); onSelect(option)
                }
            }
            repeat(columns.coerceIn(1, 2) - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun FocusTargetTile(option: FocusStartTargetOption, selected: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(FocusVisuals.Corner)
    FocusPanel(modifier.heightIn(min = 88.dp).squircleSurface(androidx.compose.ui.graphics.Color.Transparent, FocusVisuals.Corner)
        .border(BorderStroke(1.dp, if (selected) colors.primary.copy(alpha = 0.65f) else androidx.compose.ui.graphics.Color.Transparent), shape)
        .semantics { this.selected = selected }.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        padding = 14.dp, emphasized = selected) {
        Box(Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.CenterStart) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(option.title, style = MiuixTheme.textStyles.body2.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = (if (selected) colors.primary else colors.onSurfaceContainer).copy(alpha = if (enabled) 1f else 0.4f))
                if (selected) Text(stringResource(R.string.focus_hub_selected), style = MiuixTheme.textStyles.footnote2,
                    color = colors.primary.copy(alpha = if (enabled) 0.8f else 0.4f))
            }
        }
    }
}

/** Lightweight text tabs: the selected underline carries the emphasis, without a capsule. */
@Composable
private fun FocusTargetTabs(kind: String, enabled: Boolean, onKind: (String) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val tabs = listOf(FocusTargetKind.TASK to stringResource(R.string.task), FocusTargetKind.HABIT to stringResource(R.string.habit))
    Row(Modifier.fillMaxWidth().padding(start = 4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        tabs.forEach { (targetKind, title) ->
            val selected = targetKind.name == kind
            val textColor by animateColorAsState(if (selected) colors.onSurface else colors.onSurfaceVariantSummary,
                animationSpec = FocusVisuals.motion(), label = "focus-target-tab-text")
            val indicatorColor by animateColorAsState(if (selected) colors.primary else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = FocusVisuals.motion(), label = "focus-target-tab-underline")
            Column(Modifier.widthIn(min = 56.dp).selectable(selected = selected, enabled = enabled, role = Role.Tab,
                onClick = { onKind(targetKind.name) }).heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MiuixTheme.textStyles.title4.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = textColor.copy(alpha = if (enabled) 1f else 0.4f), maxLines = 1)
                Box(Modifier.width(18.dp).height(3.dp).clip(CircleShape).background(indicatorColor.copy(alpha = if (enabled) indicatorColor.alpha else indicatorColor.alpha * 0.4f)))
            }
        }
    }
}

/** Keep results as lazy siblings; SearchBar owns only input focus and its native back behavior. */
@Composable
private fun FocusTargetSearch(query: String, onQuery: (String) -> Unit, enabled: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    SearchBar(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = DpSize(0.dp, 0.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        inputField = {
            InputField(query = query, onQueryChange = onQuery,
                modifier = Modifier.formContainerBackground(28.dp),
                color = androidx.compose.ui.graphics.Color.Transparent,
                onSearch = { keyboard?.hide() },
                expanded = expanded, onExpandedChange = { expanded = it },
                label = stringResource(R.string.focus_hub_search_target), enabled = enabled)
        },
        outsideEndAction = {
            TextButton(onClick = { expanded = false; onQuery(""); keyboard?.hide() }, enabled = enabled) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {}
}
