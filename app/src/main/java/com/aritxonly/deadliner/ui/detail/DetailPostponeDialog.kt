package com.aritxonly.deadliner.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.picker.DeadlinerNumberPicker
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskDeadlineDelay
import com.deadliner.domain.task.TaskDeadlineShift
import com.deadliner.domain.task.TaskDeadlineUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Uses the app's MIUIX NumberPicker fork, including its live-value and first-layout fixes. */
@Composable
internal fun DetailPostponeDialog(task: Task, onDismiss: () -> Unit, onConfirm: (TaskDeadlineDelay) -> Unit) {
    var unit by rememberSaveable(task.uid) { mutableStateOf(TaskDeadlineUnit.DAYS) }
    var hours by rememberSaveable(task.uid) { mutableIntStateOf(24) }
    var days by rememberSaveable(task.uid) { mutableIntStateOf(1) }
    val amount = if (unit == TaskDeadlineUnit.HOURS) hours else days
    val delay = TaskDeadlineDelay(amount, unit)
    fun setAmount(value: Int) {
        if (unit == TaskDeadlineUnit.HOURS) hours = value.coerceIn(1, delay.maximum)
        else days = value.coerceIn(1, delay.maximum)
    }
    val labels = listOf(stringResource(R.string.detail_postpone_hours), stringResource(R.string.detail_postpone_days))
    val preview = TaskDeadlineShift.postponed(task.dueAt, delay)
    val formatted = preview?.let(GlobalUtils::safeParseDateTime)?.format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    val amountLabel = stringResource(R.string.detail_postpone_amount)
    val unitLabel = stringResource(R.string.detail_postpone_unit)
    AlertDialog(
        show = true,
        modifier = Modifier.testTag("postpone-dialog"),
        onDismissRequest = onDismiss,
        miuixTitle = stringResource(R.string.detail_postpone_title),
        miuixSummary = stringResource(R.string.detail_postpone_limit),
        renderTextContentInMiuix = true,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(amountLabel, style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        key(unit) {
                            DeadlinerNumberPicker(
                                value = amount, onValueChange = ::setAmount, range = 1..delay.maximum,
                                visibleItemCount = 3, wrapAround = false,
                                modifier = Modifier.testTag("postpone-amount").semantics {
                                    contentDescription = amountLabel
                                    stateDescription = amount.toString()
                                    progressBarRangeInfo = ProgressBarRangeInfo(amount.toFloat(), 1f..delay.maximum.toFloat(), delay.maximum - 2)
                                    setProgress { setAmount(it.roundToInt()); true }
                                },
                            )
                        }
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(unitLabel, style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        DeadlinerNumberPicker(
                            value = unit.ordinal, onValueChange = { unit = TaskDeadlineUnit.entries[it] }, range = 0..1,
                            label = { labels[it] }, visibleItemCount = 3, wrapAround = false,
                            modifier = Modifier.testTag("postpone-unit").semantics {
                                contentDescription = unitLabel
                                stateDescription = labels[unit.ordinal]
                                progressBarRangeInfo = ProgressBarRangeInfo(unit.ordinal.toFloat(), 0f..1f)
                                setProgress { unit = TaskDeadlineUnit.entries[it.roundToInt().coerceIn(0, 1)]; true }
                            },
                        )
                    }
                }
                Text(
                    text = if (formatted != null) stringResource(R.string.detail_postpone_until, formatted)
                        else stringResource(R.string.detail_postpone_invalid_deadline),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("postpone-preview"),
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (formatted != null) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (delay.isValid && preview != null) onConfirm(delay) }, enabled = delay.isValid && preview != null) {
                Text(stringResource(R.string.detail_postpone_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
