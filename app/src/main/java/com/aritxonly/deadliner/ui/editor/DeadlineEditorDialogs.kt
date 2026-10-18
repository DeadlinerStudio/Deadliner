package com.aritxonly.deadliner.ui.editor

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.SettingsRoute

@Composable
internal fun DeadlineEditorState.Dialogs() {
    if (showDonatePrompt) {
        AlertDialog(
            show = true,
            onDismissRequest = { showDonatePrompt = false },
            title = { Text(stringResource(R.string.deadliner_donate_plan_title)) },
            text = { Text(stringResource(R.string.deadliner_donate_plan_message)) },
            miuixTitle = getString(R.string.deadliner_donate_plan_title),
            miuixSummary = getString(R.string.deadliner_donate_plan_message),
            confirmButton = {
                TextButton(
                    onClick = {
                        showDonatePrompt = false
                        startActivity(
                            Intent(context, SettingsActivity::class.java).apply {
                                putExtra(SettingsActivity.EXTRA_INITIAL_ROUTE, SettingsRoute.Donate.route)
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.deadliner_donate_go))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonatePrompt = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    if (showCalendarEmptyDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { showCalendarEmptyDialog = false },
            title = { Text(stringResource(R.string.select_calendar_to_import)) },
            text = { Text(stringResource(R.string.no_task_add_calendar)) },
            miuixTitle = getString(R.string.select_calendar_to_import),
            miuixSummary = getString(R.string.no_task_add_calendar),
            confirmButton = {
                TextButton(onClick = {
                    showCalendarEmptyDialog = false
                    showCalendarFilterDialog()
                }) {
                    Text(stringResource(R.string.filter_calendar_account))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarEmptyDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showCalendarImportDialog) {
        com.aritxonly.deadliner.calendar.CalendarImportDialog(
            events = calendarImportEvents, query = calendarImportSearchQuery,
            selectedId = selectedCalendarEventId, eventLabel = ::calendarEventDisplayText,
            onQueryChange = { calendarImportSearchQuery = it },
            onSelect = { selectedCalendarEventId = it }, onFilter = ::showCalendarFilterDialog,
            onConfirm = ::applySelectedCalendarEvent,
            onDismiss = { showCalendarImportDialog = false },
        )
    }

    if (showCalendarAccountFilterDialog) {
        com.aritxonly.deadliner.calendar.CalendarAccountFilterDialog(
            accounts = calendarAccountOptions, selected = selectedCalendarAccounts,
            onSelectionChange = { selectedCalendarAccounts = it },
            onConfirm = ::saveCalendarAccountFilterSelection,
            onDismiss = { showCalendarAccountFilterDialog = false },
        )
    }
    datePicker.request?.let { request ->
        com.aritxonly.deadliner.ui.picker.MiuixDateTimePicker(request, datePicker::dismiss, datePicker::confirm)
    }
}
