package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.deadliner.domain.quickadd.QuickAddProposal

/** Compatibility entry for tile/shortcut routes that already opened AddDDLActivity. */
@Composable
fun QuickAddSheet(initialKind: String, onDismiss: () -> Unit,
                  onExpand: (QuickAddProposal, String) -> Unit,
                  onSaved: () -> Unit) {
    val state = rememberQuickAddState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { state.open(initialKind) }
    BackHandler(enabled = !state.busy, onBack = onDismiss)
    Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(16.dp),
        contentAlignment = Alignment.BottomEnd) {
        QuickAddContainer(state, onSaved, Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            onClose = onDismiss, onExpand = {
                (context as? Activity)?.intent?.putExtras(QuickAddEditorEntry.intent(context, state))
                onExpand(state.effectiveProposal, state.text)
            })
    }
}
