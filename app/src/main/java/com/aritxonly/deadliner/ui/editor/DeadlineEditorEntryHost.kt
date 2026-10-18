package com.aritxonly.deadliner.ui.editor

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.IntentCompat
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.quickadd.QuickAddEditorEntry
import com.aritxonly.deadliner.ui.quickadd.QuickAddSheet

/** Existing extras are a compatibility payload, never a reason to start an editor Activity. */
class DeadlineEditorEntry internal constructor(private val show: (Intent, () -> Unit) -> Unit) {
    fun open(request: Intent, onSaved: () -> Unit = {}) = show(request, onSaved)
}

val LocalDeadlineEditorEntry = staticCompositionLocalOf<DeadlineEditorEntry> {
    error("DeadlineEditorEntryHost must be provided by DeadlinerTheme")
}

object DeadlineEditorIntents {
    const val ActionAdd = "com.aritxonly.deadliner.ADD"
    private const val Request = "deadliner.editor.request"
    internal const val AllowQuickAdd = "deadliner.editor.allowQuickAdd"

    fun launch(context: Context, request: Intent = Intent(), allowQuickAdd: Boolean = true): Intent =
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(Request, Intent(request).putExtra(AllowQuickAdd, allowQuickAdd))
        }

    fun consume(intent: Intent): Intent? {
        val request = IntentCompat.getParcelableExtra(intent, Request, Intent::class.java)
        intent.removeExtra(Request)
        if (request != null) return request
        if (intent.action != ActionAdd) return null
        intent.action = Intent.ACTION_MAIN
        return Intent().putExtra(AllowQuickAdd, true)
    }
}

internal fun useQuickEditorEntry(
    enabled: Boolean, allowed: Boolean, editing: Boolean,
    generated: Boolean, prefilled: Boolean, expanded: Boolean,
): Boolean = enabled && allowed && !editing && !generated && !prefilled && !expanded

/** Mounted under the theme's existing popup and modal-backdrop hosts, once per Activity. */
@Composable
fun DeadlineEditorEntryHost(
    launchRequest: Intent? = null,
    onLaunchRequestConsumed: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var request by rememberSaveable { mutableStateOf<Intent?>(null) }
    var session by rememberSaveable { mutableIntStateOf(0) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var savedCallback by remember { mutableStateOf<() -> Unit>({}) }
    val entry = remember {
        DeadlineEditorEntry { next, onSaved ->
            request = Intent(next)
            savedCallback = onSaved
            visible = true
            session++
        }
    }
    LaunchedEffect(launchRequest) {
        if (launchRequest != null) {
            entry.open(launchRequest)
            onLaunchRequestConsumed()
        }
    }
    CompositionLocalProvider(LocalDeadlineEditorEntry provides entry) {
        content()
        request?.let { current ->
            key(session) {
                val quick = remember {
                    useQuickEditorEntry(GlobalUtils.quickAddEnabled,
                        current.getBooleanExtra(DeadlineEditorIntents.AllowQuickAdd, false),
                        current.hasExtra(AddDDLActivity.EXTRA_EDIT_TASK_UID) || current.hasExtra(AddDDLActivity.EXTRA_EDIT_HABIT_UID),
                        current.hasExtra("EXTRA_GENERATE_DDL"),
                        !current.getStringExtra(AddDDLActivity.EXTRA_PREFILL_TEXT).isNullOrBlank(),
                        QuickAddEditorEntry.initialData(current) != null)
                }
                if (quick) {
                    QuickAddSheet(
                        initialKind = if (current.getIntExtra("EXTRA_CURRENT_TYPE", 0) == 1) "HABIT" else "TASK",
                        onDismiss = { request = null },
                        onExpand = { _, _ -> },
                        onExpandIntent = { entry.open(it, savedCallback) },
                        onSaved = { savedCallback(); request = null },
                    )
                } else {
                    DeadlineEditorSheet(current, visible,
                        onDismiss = { visible = false },
                        onDismissFinished = { request = null },
                        onSaved = { savedCallback(); visible = false })
                }
            }
        }
    }
}
