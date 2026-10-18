package com.aritxonly.deadliner.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.iconResource
import com.deadliner.domain.task.TaskSubtask
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Presentation state stays local; task mutations are owned by TaskDetailViewModel. */
@Composable
internal fun DetailPlanCard(
    taskUid: String,
    subTasks: List<TaskSubtask>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
) {
    var showComposer by rememberSaveable(taskUid) { mutableStateOf(false) }
    var draft by rememberSaveable(taskUid) { mutableStateOf("") }
    var expanded by rememberSaveable(taskUid) { mutableStateOf(false) }
    var editingId by rememberSaveable(taskUid) { mutableStateOf<String?>(null) }
    var editingDraft by rememberSaveable(taskUid) { mutableStateOf("") }
    val focusRequester = remember(taskUid) { FocusRequester() }
    val bringIntoView = remember(taskUid) { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val completed = subTasks.filter { it.isCompleted }
    val pending = subTasks.filterNot { it.isCompleted }
    val progress by animateFloatAsState(
        if (subTasks.isEmpty()) 0f else completed.size.toFloat() / subTasks.size,
        label = "detail_plan_progress",
    )

    LaunchedEffect(showComposer) {
        if (showComposer) {
            focusRequester.requestFocus()
            bringIntoView.bringIntoView()
        }
    }
    LaunchedEffect(imeBottom) {
        if (showComposer && imeBottom > 0) bringIntoView.bringIntoView()
    }

    fun submit() {
        val content = draft.trim()
        if (content.isNotEmpty()) {
            onAdd(content)
            draft = ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MaterialText(stringResource(R.string.detail_plan_title), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f))
            Text(stringResource(R.string.detail_plan_progress, completed.size, subTasks.size),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(progress = progress, height = 4.dp)
        Spacer(Modifier.height(8.dp))
        if (subTasks.isEmpty()) {
            Text(stringResource(R.string.detail_plan_empty),
                modifier = Modifier.padding(vertical = 14.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        pending.forEach { task ->
            key(task.uid) {
                DetailPlanRow(task, onToggle = { onToggle(task.uid) }, onEdit = {
                    editingId = task.uid
                    editingDraft = task.content
                })
            }
        }
        if (showComposer) {
            Row(
                Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView).padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    miuixLabel = stringResource(R.string.detail_plan_compose_hint),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                val addLabel = stringResource(R.string.detail_plan_add)
                IconButton(
                    onClick = ::submit,
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.size(48.dp).semantics {
                        contentDescription = addLabel
                        if (draft.isBlank()) disabled()
                    },
                ) {
                    Icon(painter = iconResource(R.drawable.msr_check_wght400),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (draft.isNotBlank()) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.disabledOnSurface)
                }
                IconButton(onClick = { showComposer = false; draft = "" }, modifier = Modifier.size(48.dp)) {
                    Icon(painter = iconResource(R.drawable.msr_close_wght400),
                        contentDescription = stringResource(R.string.cancel), modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                }
            }
        } else {
            DetailTextAction(
                label = stringResource(R.string.detail_plan_add),
                iconRes = R.drawable.msr_add_wght400,
                onClick = { showComposer = true },
                modifier = Modifier.fillMaxWidth(),
                alignStart = true,
            )
        }
        if (completed.isNotEmpty()) {
            val expandedLabel = stringResource(if (expanded) R.string.detail_plan_expanded else R.string.detail_plan_collapsed)
            DetailTextAction(
                label = stringResource(R.string.detail_plan_completed_count, completed.size),
                iconRes = R.drawable.msr_chevron_right_wght400,
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().semantics { stateDescription = expandedLabel },
                alignStart = true,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                iconRotation = if (expanded) 90f else 0f,
            )
            AnimatedVisibility(visible = expanded) {
                Column {
                    completed.forEach { task ->
                        key(task.uid) {
                            DetailPlanRow(task, onToggle = { onToggle(task.uid) }, onEdit = {
                                editingId = task.uid
                                editingDraft = task.content
                            })
                        }
                    }
                }
            }
        }
    }

    val editingTask = subTasks.firstOrNull { it.uid == editingId }
    // A sync/delete may remove the task while its editor is open.
    LaunchedEffect(editingId, editingTask?.uid) {
        if (editingId != null && editingTask == null) editingId = null
    }
    if (editingTask != null) {
        AlertDialog(
            show = true,
            onDismissRequest = { editingId = null },
            miuixTitle = stringResource(R.string.detail_plan_edit),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingDraft,
                        onValueChange = { editingDraft = it },
                        miuixLabel = stringResource(R.string.detail_plan_content),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5,
                    )
                    TextButton(onClick = { onDelete(editingTask.uid); editingId = null }) {
                        Icon(painter = iconResource(R.drawable.msr_delete_wght400), contentDescription = null,
                            tint = MiuixTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete), color = MiuixTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val content = editingDraft.trim()
                    if (content.isNotEmpty()) {
                        if (content != editingTask.content) onUpdate(editingTask.uid, content)
                        editingId = null
                    }
                }, enabled = editingDraft.isNotBlank()) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun DetailPlanRow(task: TaskSubtask, onToggle: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f).heightIn(min = 58.dp)
                .toggleable(value = task.isCompleted, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                state = if (task.isCompleted) ToggleableState.On else ToggleableState.Off,
                onClick = null,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = task.content,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1.copy(
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
                color = if (task.isCompleted) MiuixTheme.colorScheme.onSurfaceVariantSummary
                    else MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = iconResource(R.drawable.msr_more_vert_wght400),
                contentDescription = stringResource(R.string.detail_plan_edit_item, task.content),
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}
