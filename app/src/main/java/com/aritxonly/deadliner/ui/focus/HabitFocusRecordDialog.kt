package com.aritxonly.deadliner.ui.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.deadliner.viewmodel.focus.FocusTimerUiState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keeps input open when a countdown finishes; the saved session UID is the idempotence key. */
@Composable
internal fun HabitFocusRecordDialog(state: FocusTimerUiState, sessionUid: String?, expectedVersion: String?,
    onDismiss: () -> Unit, onConfirm: (String, Int?, String?) -> Unit) {
    var amount by rememberSaveable(sessionUid) { mutableStateOf("") }
    val context = state.habitRecordContext
    val value = amount.toIntOrNull()?.takeIf { it > 0 }
    LaunchedEffect(state.lastCompletedSession?.uid, state.lastCompletedSession?.habitRecordedAmount) {
        val saved = state.lastCompletedSession
        if (sessionUid != null && saved?.uid == sessionUid && saved.habitRecordedAmount != null) onDismiss()
    }
    AlertDialog(show = sessionUid != null, onDismissRequest = { if (!state.isBusy) onDismiss() },
        miuixTitle = stringResource(R.string.habit_focus_amount),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                context?.let { Text(stringResource(R.string.habit_focus_record_date, it.occurredOn),
                    style = MiuixTheme.textStyles.footnote1) }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(10) },
                    modifier = Modifier.fillMaxWidth().testTag("habit-focus-amount"), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    miuixLabel = context?.unit ?: stringResource(R.string.habit_focus_amount))
                if (expectedVersion != null && context?.habitUpdatedAt != expectedVersion) {
                    Text(stringResource(R.string.habit_focus_record_failed), color = MiuixTheme.colorScheme.error)
                }
                Text(stringResource(if (amount.isNotBlank() && value == null) R.string.habit_focus_amount_invalid else R.string.habit_focus_amount_hint),
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }, renderTextContentInMiuix = true,
        confirmButton = { Button(modifier = Modifier.testTag("habit-focus-record-confirm"), enabled = value != null && !state.isBusy && context?.canRecord == true && (expectedVersion == null || expectedVersion == context.habitUpdatedAt),
            onClick = { if (sessionUid != null && value != null) onConfirm(sessionUid, value, expectedVersion) }) {
            Text(stringResource(if (state.timer == null) R.string.habit_focus_record else R.string.habit_focus_finish_record))
        } },
        dismissButton = { Button(enabled = !state.isBusy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
