package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.TextButton

@Composable
internal fun QuickAddAttributeDialog(state: QuickAddState, attribute: String, onDismiss: () -> Unit) {
    val schedule = attribute == "schedule"
    var value by remember(attribute) { mutableStateOf(if (schedule) scheduleText(state) else state.proposal.note) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(show = true, onDismissRequest = onDismiss, miuixTitle = if (schedule) "重复规则" else "备注",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value, { value = it; error = false }, Modifier.fillMaxWidth(),
                    label = { Text(if (schedule) "例如：每周一、三 2次" else "备注") })
                if (error) Text("暂未识别此规则，请调整输入")
            }
        },
        confirmButton = { TextButton(onClick = {
            if (schedule) { if (state.setSchedule(value)) onDismiss() else error = true }
            else { state.setNote(value); onDismiss() }
        }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun scheduleText(state: QuickAddState): String {
    val p = state.proposal
    val period = when (p.period) {
        "WEEKLY" -> "每周"
        "MONTHLY" -> "每月"
        else -> if (p.weekdaysMask == 127) "每天" else "每周" + listOf("一", "二", "三", "四", "五", "六", "日")
            .filterIndexed { index, _ -> p.weekdaysMask and (1 shl index) != 0 }.joinToString("、")
    }
    return "$period ${p.targetCount}次"
}
