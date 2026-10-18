package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.aritxonly.deadliner.ui.base.LocalAdvancedMaterialDialogBlurHostState
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.deadliner.domain.quickadd.QuickAddProposal

/** External entries share the homepage lens and sample its registered page-only backdrop. */
@Composable
fun QuickAddSheet(initialKind: String, onDismiss: () -> Unit,
                  onExpand: (QuickAddProposal, String) -> Unit,
                  onSaved: () -> Unit,
                  onExpandIntent: ((android.content.Intent) -> Unit)? = null) {
    val state = rememberQuickAddState()
    val context = LocalContext.current
    val host = LocalAdvancedMaterialDialogBlurHostState.current
    LaunchedEffect(Unit) { state.open(initialKind) }
    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides
        (LocalAdvancedMaterialBackdrop.current ?: host?.modalBackdrop)) {
        QuickAddOverlay(state, onSaved, onDismissFinished = onDismiss, onExpand = { expanded ->
            if (onExpandIntent != null) onExpandIntent(expanded) else {
                (context as? Activity)?.intent?.putExtras(expanded)
                onExpand(state.effectiveProposal, state.text)
            }
        })
    }
}
