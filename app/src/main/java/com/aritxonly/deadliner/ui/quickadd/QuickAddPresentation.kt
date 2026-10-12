package com.aritxonly.deadliner.ui.quickadd

import com.deadliner.domain.quickadd.QuickAddProposal

object QuickAddPresentation {
    fun dateTime(value: String?): String = value?.let {
        runCatching {
            com.aritxonly.deadliner.ui.editor.formatEditorDateTime(java.time.LocalDateTime.parse(it.replace(' ', 'T')))
        }.getOrDefault(it.replace('T', ' '))
    } ?: "未设置"
    fun detail(p: QuickAddProposal): String {
        val window = when (p.period) {
            "WEEKLY" -> "每周"; "MONTHLY" -> "每月"; "TOTAL" -> "累计"; else -> "每个执行日"
        }
        val days = listOf("一", "二", "三", "四", "五", "六", "日")
            .filterIndexed { index, _ -> p.weekdaysMask and (1 shl index) != 0 }.joinToString("、")
        return window + " " + p.targetCount + " 次 · 周" + days +
            (p.reminderTime?.let { " · 提醒 " + it } ?: "")
    }
}
