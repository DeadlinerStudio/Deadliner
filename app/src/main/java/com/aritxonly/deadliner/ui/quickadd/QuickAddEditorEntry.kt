package com.aritxonly.deadliner.ui.quickadd

import android.content.Context
import android.content.Intent
import androidx.compose.ui.geometry.Rect
import com.aritxonly.deadliner.AddDDLActivity
import kotlinx.serialization.json.Json
import com.deadliner.domain.quickadd.QuickAddProposal
import java.time.LocalDateTime

/** Full-editor fallback for proposals requiring advanced scheduling. */
object QuickAddEditorEntry {
    private const val Proposal = "QUICK_ADD_PROPOSAL"
    private const val SourceBounds = "QUICK_ADD_SOURCE_BOUNDS"
    private const val Start = "QUICK_ADD_START"
    private const val Category = "QUICK_ADD_CATEGORY"
    const val Calendar = "QUICK_ADD_CALENDAR"
    fun intent(context: Context, state: QuickAddState) = Intent(context, AddDDLActivity::class.java).apply {
        putExtra("EXTRA_CURRENT_TYPE", if (state.proposal.kind == "HABIT") 1 else 0)
        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, state.text)
        putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, state.recognizingAI)
        if (state.panelBounds.width > 0f && state.panelBounds.height > 0f) {
            val bounds = state.panelBounds
            putExtra(SourceBounds, floatArrayOf(bounds.left, bounds.top, bounds.right, bounds.bottom))
        }
        putExtra(Proposal, state.effectiveProposal.toJson())
        putExtra(Start, state.effectiveStartAt.toString())
        putExtra(Category, state.categoryUid)
        state.calendarEventId?.let { putExtra(Calendar, it) }
    }
    fun sourceBounds(intent: Intent): Rect? {
        val bounds = intent.getFloatArrayExtra(SourceBounds) ?: return null
        if (bounds.size != 4 || bounds.any { !it.isFinite() }) return null
        return Rect(bounds[0], bounds[1], bounds[2], bounds[3]).takeIf { it.width > 0f && it.height > 0f }
    }
    fun restoreDraft(state: QuickAddState, intent: Intent): Boolean = runCatching {
        val json = intent.getStringExtra(Proposal) ?: return false
        state.restoreDraft(Json.decodeFromString<QuickAddProposal>(json),
            intent.getStringExtra(AddDDLActivity.EXTRA_PREFILL_TEXT).orEmpty(),
            intent.getStringExtra(Start)?.let(LocalDateTime::parse), intent.getStringExtra(Category),
            if (intent.hasExtra(Calendar)) intent.getLongExtra(Calendar, 0L) else null)
        true
    }.getOrDefault(false)

    /** Complete editor draft with fallback times; inline recognition only merges explicit fields. */
    fun formData(state: QuickAddState): AddEntryInitialData = AddEntryInitialData(
        QuickAddPersistence.taskDraft(state.effectiveProposal, state.text).copy(
            startTime = state.effectiveStartAt, categoryUid = state.categoryUid),
        QuickAddPersistence.habitDraft(state.effectiveProposal, state.text).copy(categoryUid = state.categoryUid),
        state.text, false, state.calendarEventId)

    fun initialData(intent: Intent): AddEntryInitialData? = runCatching {
        val json = intent.getStringExtra(Proposal) ?: return null
        val p = Json.decodeFromString<QuickAddProposal>(json)
        val raw = intent.getStringExtra(AddDDLActivity.EXTRA_PREFILL_TEXT).orEmpty()
        val start = intent.getStringExtra(Start)?.let(LocalDateTime::parse)
        val category = intent.getStringExtra(Category)
        val task = QuickAddPersistence.taskDraft(p, raw)
        AddEntryInitialData(task.copy(startTime = start ?: task.startTime, categoryUid = category),
            QuickAddPersistence.habitDraft(p, raw).copy(categoryUid = category), raw,
            intent.getBooleanExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, false),
            if (intent.hasExtra(Calendar)) intent.getLongExtra(Calendar, 0L) else null)
    }.getOrNull()
}
