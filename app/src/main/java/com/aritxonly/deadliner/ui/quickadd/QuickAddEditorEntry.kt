package com.aritxonly.deadliner.ui.quickadd

import android.content.Context
import android.content.Intent
import com.aritxonly.deadliner.AddDDLActivity
import kotlinx.serialization.json.Json
import com.deadliner.domain.quickadd.QuickAddProposal
import java.time.LocalDateTime

/** Full-editor fallback for proposals requiring advanced scheduling. */
object QuickAddEditorEntry {
    private const val Proposal = "QUICK_ADD_PROPOSAL"
    private const val Start = "QUICK_ADD_START"
    private const val Category = "QUICK_ADD_CATEGORY"
    const val Calendar = "QUICK_ADD_CALENDAR"
    fun intent(context: Context, state: QuickAddState) = Intent(context, AddDDLActivity::class.java).apply {
        putExtra("EXTRA_CURRENT_TYPE", if (state.proposal.kind == "HABIT") 1 else 0)
        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, state.text)
        putExtra(Proposal, state.effectiveProposal.toJson())
        putExtra(Start, state.effectiveStartAt.toString())
        putExtra(Category, state.categoryUid)
        state.calendarEventId?.let { putExtra(Calendar, it) }
    }
    fun initialData(intent: Intent): AddEntryInitialData? = runCatching {
        val json = intent.getStringExtra(Proposal) ?: return null
        val p = Json.decodeFromString<QuickAddProposal>(json)
        val raw = intent.getStringExtra(AddDDLActivity.EXTRA_PREFILL_TEXT).orEmpty()
        val start = intent.getStringExtra(Start)?.let(LocalDateTime::parse)
        val category = intent.getStringExtra(Category)
        val task = QuickAddPersistence.taskDraft(p, raw)
        AddEntryInitialData(task.copy(startTime = start ?: task.startTime, categoryUid = category),
            QuickAddPersistence.habitDraft(p, raw).copy(categoryUid = category), raw, false,
            if (intent.hasExtra(Calendar)) intent.getLongExtra(Calendar, 0L) else null)
    }.getOrNull()
}
