package com.aritxonly.deadliner.ui.quickadd

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aritxonly.deadliner.CategoryManagementActivity
import com.aritxonly.deadliner.ui.category.CategoryPickerSheet
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
    val activity = context as? FragmentActivity
    val categories = rememberActiveCategories()
    var showCategory by remember { mutableStateOf(false) }
    var editAttribute by remember { mutableStateOf<String?>(null) }
    val importCalendar = rememberQuickAddCalendarImport(state::importEvent)
    val category = categories.firstOrNull { it.uid == state.categoryUid }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.proposal.ready && !state.needsAI) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.proposal.kind == "TASK") {
                    state.explicitStartAt?.let { start ->
                        QuickAddAttributeChip("schedule", "开始 ${compactDateTime(start.toString())}", !state.busy,
                            onRemove = state::removeStart) {
                            activity?.let { DateTimePickers.showDateTimePicker(it.supportFragmentManager,
                                onDateTimeSelected = { state.startAt = it }) }
                        }
                    }
                    state.explicitDueAt?.let { due ->
                        QuickAddAttributeChip("event", "截止 ${compactDateTime(due)}", !state.busy,
                            onRemove = state::removeDue) {
                            activity?.let { DateTimePickers.showDateTimePicker(it.supportFragmentManager,
                                afterDateTime = state.effectiveStartAt, onDateTimeSelected = { state.dueAt = it }) }
                        }
                    }
                } else {
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
                category?.let {
                    QuickAddAttributeChip("folder", it.name, !state.busy,
                        onRemove = { state.categoryUid = null }) { showCategory = true }
                }
                if (state.calendarEventId != null) QuickAddAttributeChip("calendar_month", "日历事件", !state.busy,
                    onRemove = { state.calendarEventId = null }, onClick = importCalendar)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            QuickAddRoundAction("folder", category?.name?.let { "分类：$it" } ?: "分类", !state.busy) { showCategory = true }
            QuickAddRoundAction("calendar_month", "日历导入", !state.busy, onClick = importCalendar)
            if (state.text.isNotBlank()) {
                QuickAddAIAction(state)
            }
            Spacer(Modifier.weight(1f))
            QuickAddRoundAction("arrow_upward", "添加草案", state.canSubmit, emphasized = true) { state.submit(onSaved) }
        }
    }
    if (showCategory) CategoryPickerSheet(categories, state.categoryUid, { state.categoryUid = it },
        { context.startActivity(CategoryManagementActivity.newIntent(context)) }, { showCategory = false })
    editAttribute?.let { attribute ->
        QuickAddAttributeDialog(state, attribute, onDismiss = { editAttribute = null })
    }
}

private fun compactDateTime(value: String): String = runCatching {
    LocalDateTime.parse(value.replace(' ', 'T')).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
}.getOrDefault(value)

@Composable
private fun QuickAddRoundAction(symbol: String, accessibilityLabel: String, enabled: Boolean,
                                emphasized: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val color = (if (emphasized) colors.onPrimary else colors.onSurface).copy(alpha = if (enabled) 1f else 0.38f)
    Box(Modifier.size(40.dp).clip(CircleShape)
        .background(if (emphasized) colors.primary.copy(alpha = if (enabled) 1f else 0.38f)
            else colors.onSurface.copy(alpha = 0.06f))
        .clickable(enabled = enabled, onClickLabel = accessibilityLabel, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(materialSymbolPainter(symbol), accessibilityLabel, Modifier.size(20.dp), tint = color)
    }
}

@Composable
private fun QuickAddAIAction(state: QuickAddState) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.canRecognize) 1f else 0.38f)
    Row(Modifier.height(40.dp).clip(CircleShape)
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
                                  onRemove: () -> Unit, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    Row(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f, fill = false).clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(materialSymbolPainter(symbol), null, Modifier.size(16.dp), tint = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(40.dp).clickable(enabled = enabled, onClick = onRemove), contentAlignment = Alignment.Center) {
            Icon(materialSymbolPainter("close"), "移除$label", Modifier.size(16.dp), tint = color)
        }
    }
}
