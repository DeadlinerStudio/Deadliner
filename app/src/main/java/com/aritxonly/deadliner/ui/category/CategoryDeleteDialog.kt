package com.aritxonly.deadliner.ui.category

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton

@Composable
internal fun CategoryDeleteDialog(
    categoryName: String?,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.category_delete_confirm_title, categoryName.orEmpty())
    val summary = stringResource(R.string.category_delete_confirm_message)
    val cancelText = stringResource(R.string.cancel)
    val deleteText = stringResource(R.string.category_delete)

    AlertDialog(
        show = categoryName != null,
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(summary) },
        miuixTitle = title,
        miuixSummary = summary,
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(cancelText)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                useMiuixDialogActionColors = false,
            ) {
                Text(
                    text = deleteText,
                )
            }
        },
    )
}
