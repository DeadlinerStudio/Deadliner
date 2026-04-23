package com.aritxonly.deadliner.ui.main.shared

import android.app.Activity
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme
import com.aritxonly.deadliner.model.DDLItem

@Composable
fun DeadlinerUrlIntake(
    activity: Activity,
    snackbarHostState: SnackbarHostState,
    pendingUrl: String?,
    onPendingUrlChange: (String?) -> Unit,
    onDecodedItemReady: (DDLItem) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(android.content.ClipboardManager::class.java)
    val view = LocalView.current
    var lastHandledUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        consumeDeadlinerUrl(activity)?.let { url ->
            runCatching {
                DeadlinerURLScheme.decodeWithPassphrase(url, "deadliner-2025".toCharArray())
            }.onSuccess { item ->
                onDecodedItemReady(item)
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(R.string.share_link_parse_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    LaunchedEffect(pendingUrl) {
        val url = pendingUrl ?: return@LaunchedEffect
        if (url == lastHandledUrl) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.detect_share_link),
            actionLabel = context.getString(R.string.add),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )

        if (result == SnackbarResult.ActionPerformed) {
            runCatching {
                DeadlinerURLScheme.decodeWithPassphrase(url, "deadliner-2025".toCharArray())
            }.onSuccess { item ->
                onDecodedItemReady(item)
            }
        }
        lastHandledUrl = url
    }

    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) return@OnWindowFocusChangeListener

            val clipText = clipboardManager.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.trim()
                .orEmpty()

            if (isDeadlinerUrl(clipText)) {
                onPendingUrlChange(clipText)
            }
        }

        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
        }
    }
}

private fun consumeDeadlinerUrl(activity: Activity): String? {
    val data = activity.intent?.dataString ?: return null
    if (isDeadlinerUrl(data)) {
        activity.intent?.data = null
        return data
    }
    return null
}

private fun isDeadlinerUrl(text: String): Boolean {
    if (text.isBlank()) return false
    return text.startsWith(DeadlinerURLScheme.DEADLINER_URL_SCHEME_PREFIX) ||
        text.startsWith(DeadlinerURLScheme.DEADLINER_URL_SCHEME_PREFIX_LEGACY)
}
