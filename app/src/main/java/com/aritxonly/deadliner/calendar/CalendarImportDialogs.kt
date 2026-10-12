package com.aritxonly.deadliner.calendar

import android.widget.Toast
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.base.Checkbox
import com.aritxonly.deadliner.ui.base.TextButton

/** The calendar picker shared by the regular editor and quick add. */
@Composable
fun CalendarImportDialog(events: List<CalendarEvent>, query: String, selectedId: Long?,
                         eventLabel: (CalendarEvent) -> String, onQueryChange: (String) -> Unit,
                         onSelect: (Long) -> Unit, onFilter: () -> Unit,
                         onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val filteredCalendarEvents = events.filter { query.isBlank() || eventLabel(it).contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        show = true,
        onDismissRequest = { onDismiss() },
        renderTextContentInMiuix = true,
        title = { Text(stringResource(R.string.select_calendar_to_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = { onFilter() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.filter_calendar_account))
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search)) },
                    miuixLabel = context.getString(R.string.search),
                    singleLine = true
                )
                if (filteredCalendarEvents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_task_add_calendar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCalendarEvents.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(event.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedId == event.id,
                                    onClick = { onSelect(event.id) }
                                )
                                Text(
                                    text = eventLabel(event),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        miuixTitle = context.getString(R.string.select_calendar_to_import),
        miuixSummary = context.getString(R.string.settings_import),
        confirmButton = {
            TextButton(onClick = {
                if (selectedId == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_event_select),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onConfirm()
                }
            }) {
                Text(stringResource(R.string.settings_import))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun CalendarAccountFilterDialog(accounts: List<String>, selected: Set<String>,
                                onSelectionChange: (Set<String>) -> Unit,
                                onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        show = true,
        onDismissRequest = { onDismiss() },
        renderTextContentInMiuix = true,
        title = { Text(stringResource(R.string.select_calendar_account_to_hide)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    val checked = selected.contains(account)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectionChange(if (checked) {
                                    selected - account
                                } else {
                                    selected + account
                                })
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                onSelectionChange(if (isChecked) {
                                    selected + account
                                } else {
                                    selected - account
                                })
                            }
                        )
                        Text(
                            text = account,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        miuixTitle = context.getString(R.string.select_calendar_account_to_hide),
        miuixSummary = context.getString(R.string.filter_calendar_account),
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/** Keeps event labels identical in both entry points. */
fun formatCalendarImportEvent(context: android.content.Context, event: CalendarEvent): String {
    val time = com.aritxonly.deadliner.localutils.GlobalUtils.parseDateTime(
        event.startMillis.toDateTimeString())
    val formatted = time?.let { com.aritxonly.deadliner.ui.editor.formatEditorDateTime(it) }
        ?: context.getString(R.string.parse_failed)
    return "${event.title.takeIf(String::isNotBlank) ?: context.getString(R.string.untitled)} - $formatted"
}
