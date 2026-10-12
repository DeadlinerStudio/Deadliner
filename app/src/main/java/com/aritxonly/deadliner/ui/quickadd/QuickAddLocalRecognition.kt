package com.aritxonly.deadliner.ui.quickadd

import com.deadliner.domain.quickadd.QuickAddParser
import com.deadliner.domain.quickadd.QuickAddProposal
import com.deadliner.domain.quickadd.model.QuickAddRecognitionPolicy
import java.time.LocalDateTime

internal data class QuickAddLocalResult(val proposal: QuickAddProposal, val startAt: LocalDateTime? = null)

/** Explicit start/range phrases reuse Core's conservative single-date parser. */
internal fun QuickAddParser.recognizeLocal(text: String, kind: String): QuickAddLocalResult {
    val input = text.trim()
    val match = Regex("^从(.+?)(?:到|至)(.+)$").matchEntire(input)
        ?: Regex("^(?:从)?(.+?)(?:开始|起始)[，,\\s]*(.+)$").matchEntire(input)
        ?: return QuickAddLocalResult(QuickAddRecognitionPolicy().fallback(text, kind))
    val start = parse(match.groupValues[1] + " 起始占位", "TASK")
    val rest = QuickAddRecognitionPolicy().fallback(match.groupValues[2], "TASK")
    val date = start.dueAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    if (!start.ready || start.title != "起始占位" || date == null || !rest.ready || rest.kind != "TASK") {
        return QuickAddLocalResult(QuickAddRecognitionPolicy().fallback(text, kind))
    }
    return QuickAddLocalResult(rest, date)
}
