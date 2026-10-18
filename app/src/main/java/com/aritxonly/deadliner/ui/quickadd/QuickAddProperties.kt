package com.aritxonly.deadliner.ui.quickadd

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.colorResource
import com.aritxonly.deadliner.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aritxonly.deadliner.CategoryManagementActivity
import com.aritxonly.deadliner.ui.category.CategoryDropdownMenu
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import com.aritxonly.deadliner.ui.category.CategoryPresentationRegistry
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.category.rememberActiveCategories
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.aritxonly.deadliner.ui.picker.DateTimePickers
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickAddProperties(state: QuickAddState, onSaved: () -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    fun prepareDatePicker() { keyboard?.hide(); focus.clearFocus() }
    val activity = context as? FragmentActivity
    val categories = rememberActiveCategories()
    var editAttribute by remember { mutableStateOf<String?>(null) }
    val importCalendar = rememberQuickAddCalendarImport(state::importEvent)
    val category = categories.firstOrNull { it.uid == state.categoryUid }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Manual attributes remain visible even while recognition is pending or rejected.
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (state.proposal.kind == "TASK") {
                state.explicitStartAt?.let { start ->
                    QuickAddAttributeChip("task_start", "开始 ${compactDateTime(start.toString())}", !state.busy,
                        onRemove = state::removeStart) {
                        activity?.let { prepareDatePicker(); DateTimePickers.showDateTimePicker(it.supportFragmentManager,
                            initialDateTime = start, onDateTimeSelected = state::selectStartAt) }
                    }
                }
                state.explicitDueAt?.let { due ->
                    QuickAddAttributeChip("task_due", "截止 ${compactDateTime(due)}", !state.busy,
                        onRemove = state::removeDue) {
                        activity?.let { prepareDatePicker(); DateTimePickers.showDateTimePicker(it.supportFragmentManager,
                            afterDateTime = state.effectiveStartAt,
                            initialDateTime = state.explicitDueAt?.let { LocalDateTime.parse(it.replace(' ', 'T')) },
                            onDateTimeSelected = state::selectDueAt) }
                    }
                }
            } else if (state.proposal.ready && !state.needsAI) {
                QuickAddAttributeChip("repeat", QuickAddPresentation.detail(state.proposal.copy(reminderTime = null)),
                    !state.busy, onRemove = state::removeSchedule) { editAttribute = "schedule" }
                state.proposal.reminderTime?.let { time ->
                    QuickAddAttributeChip("notifications", "提醒 $time", !state.busy,
                        onRemove = { state.setReminder(null) }) {
                        activity?.let { DateTimePickers.showTimePicker(it.supportFragmentManager,
                            initialTime = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(20, 0)),
                            onTimeSelected = { state.setReminder(it.toString()) }) }
                    }
                }
            }
            if (state.proposal.note.isNotBlank()) QuickAddAttributeChip("notes", state.proposal.note, !state.busy,
                onRemove = { state.setNote("") }) { editAttribute = "note" }
            // Keep the popup owner mounted even when selecting "uncategorized" removes the chip.
            CategoryDropdownMenu(categories, state.categoryUid, { state.categoryUid = it },
                { context.startActivity(CategoryManagementActivity.newIntent(context)) }, enabled = !state.busy) { anchor, open, _ ->
                category?.let {
                    QuickAddCategoryChip(it, !state.busy,
                        onRemove = { state.categoryUid = null }, modifier = anchor, onClick = open)
                }
            }
            if (state.calendarEventId != null) QuickAddAttributeChip("calendar_month", "日历事件", !state.busy,
                onRemove = { state.calendarEventId = null }, onClick = importCalendar)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryDropdownMenu(categories, state.categoryUid, { state.categoryUid = it },
                { context.startActivity(CategoryManagementActivity.newIntent(context)) }, enabled = !state.busy) { anchor, open, _ ->
                QuickAddRoundAction("folder", category?.name?.let { "分类：$it" } ?: "分类", !state.busy,
                    modifier = anchor, category = category, onClick = open)
            }
            QuickAddRoundAction("calendar_month", "日历导入", !state.busy, onClick = importCalendar)
            if (state.proposal.kind == "TASK") {
                val due = state.effectiveProposal.dueAt
                QuickAddDeadlineAction("截止时间：${due?.let(::compactDateTime) ?: "选择时间"}", !state.busy) {
                    activity?.let { prepareDatePicker(); DateTimePickers.showDateTimePicker(it.supportFragmentManager,
                        afterDateTime = state.effectiveStartAt,
                        initialDateTime = state.explicitDueAt?.let { LocalDateTime.parse(it.replace(' ', 'T')) },
                        onDateTimeSelected = state::selectDueAt) }
                }
            }
            if (state.text.isNotBlank()) {
                QuickAddAIAction(state)
            }
            Spacer(Modifier.weight(1f))
            QuickAddRoundAction("arrow_upward", "添加草案", state.canSubmit, emphasized = true) { state.submit(onSaved) }
        }
    }
    editAttribute?.let { attribute ->
        QuickAddAttributeDialog(state, attribute, onDismiss = { editAttribute = null })
    }
}

private fun compactDateTime(value: String): String = runCatching {
    LocalDateTime.parse(value.replace(' ', 'T')).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
}.getOrDefault(value)

@Composable
private fun QuickAddRoundAction(symbol: String, accessibilityLabel: String, enabled: Boolean,
                                emphasized: Boolean = false, modifier: Modifier = Modifier,
                                category: CategoryPresentation? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val categoryColor = category?.let { CategoryPresentationRegistry.color(it.colorToken) }
    val color = (categoryColor ?: if (emphasized) colors.onPrimary else colors.onSurface).copy(alpha = if (enabled) 1f else 0.38f)
    Box(modifier.size(40.dp).miuixPressFeedback(enabled = enabled, overlay = false).clip(CircleShape)
        .background(if (emphasized) colors.primary.copy(alpha = if (enabled) 1f else 0.38f)
            else categoryColor?.copy(alpha = 0.15f) ?: colors.onSurface.copy(alpha = 0.06f))
        .clickable(enabled = enabled, onClickLabel = accessibilityLabel, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(if (category != null) iconResource(CategoryPresentationRegistry.icon(category.iconSemanticId)) else when (symbol) {
            "arrow_upward" -> com.aritxonly.deadliner.ui.originalMaterialSymbolPainter(symbol)
            "calendar_month" -> com.aritxonly.deadliner.ui.calendarImportPainter()
            else -> materialSymbolPainter(symbol)
        }, accessibilityLabel, Modifier.size(20.dp), tint = color)
    }
}

@Composable
internal fun QuickAddAIAction(state: QuickAddState) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.canRecognize) 1f else 0.38f)
    Row(Modifier.height(40.dp).miuixPressFeedback(enabled = state.canRecognize, overlay = false).clip(CircleShape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        .clickable(enabled = state.canRecognize, onClickLabel = "AI 识别", onClick = state::recognizeWithAI)
        .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (state.recognizingAI) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary)
        else Icon(materialSymbolPainter("auto_awesome"), null, Modifier.size(18.dp), tint = color)
        Text("AI", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun QuickAddAttributeChip(symbol: String, label: String, enabled: Boolean,
                                  onRemove: () -> Unit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    Row(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f, fill = false).clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (symbol == "task_start" || symbol == "task_due") {
                QuickAddTimeDot(start = symbol == "task_start", enabled = enabled)
            } else Icon(materialSymbolPainter(symbol), null, Modifier.size(16.dp), tint = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(40.dp).miuixPressFeedback(enabled = enabled, overlay = false).clip(CircleShape).clickable(enabled = enabled, onClick = onRemove), contentAlignment = Alignment.Center) {
            Icon(materialSymbolPainter("close"), "移除$label", Modifier.size(16.dp), tint = color)
        }
    }
}

@Composable
private fun QuickAddDeadlineAction(accessibilityLabel: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.height(40.dp).miuixPressFeedback(enabled = enabled, overlay = false).clip(CircleShape)
        .background(colors.onSurface.copy(alpha = 0.06f))
        .semantics { contentDescription = accessibilityLabel }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        QuickAddTimeDot(start = false, enabled = enabled)
        Text("截止", style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.38f))
    }
}

@Composable
private fun QuickAddTimeDot(start: Boolean, enabled: Boolean) {
    val color = colorResource(if (start) R.color.chart_green else R.color.chart_red)
    Box(Modifier.size(8.dp).background(color.copy(alpha = if (enabled) 1f else 0.38f), CircleShape))
}
