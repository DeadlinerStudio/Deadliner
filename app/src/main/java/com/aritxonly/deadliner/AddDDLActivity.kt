package com.aritxonly.deadliner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.aritxonly.deadliner.ui.editor.DeadlineEditorIntents

/** Compatibility trampoline for already-pinned shortcuts and integrations. No editor UI lives here. */
class AddDDLActivity : android.app.Activity() {
    companion object {
        const val EXTRA_PREFILL_TEXT = "EXTRA_PREFILL_TEXT"
        const val EXTRA_AUTO_RUN_AI = "EXTRA_AUTO_RUN_AI"
        const val EXTRA_EDIT_HABIT_UID = "EXTRA_EDIT_HABIT_UID"
        const val EXTRA_EDIT_TASK_UID = "EXTRA_EDIT_TASK_UID"

        fun newEditHabitIntent(context: Context, habitUid: String): Intent =
            Intent(context, AddDDLActivity::class.java).putExtra(EXTRA_EDIT_HABIT_UID, habitUid)

        fun newEditIntent(context: Context, taskUid: String): Intent =
            Intent(context, AddDDLActivity::class.java).putExtra(EXTRA_EDIT_TASK_UID, taskUid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(DeadlineEditorIntents.launch(this, intent))
        finish()
    }
}
