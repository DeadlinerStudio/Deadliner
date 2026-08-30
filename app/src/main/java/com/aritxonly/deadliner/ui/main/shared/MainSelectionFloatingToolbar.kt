package com.aritxonly.deadliner.ui.main.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.iconResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainSelectionFloatingToolbar(
    expanded: Boolean,
    selectedPage: DeadlineType,
    onDoneClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onReminderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        expandedShadowElevation = 1.dp,
        collapsedShadowElevation = 1.dp,
        modifier = modifier.padding(2.dp),
    ) {
        IconButton(onClick = onDoneClick) {
            Icon(iconResource(R.drawable.ic_done), contentDescription = null)
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (selectedPage == DeadlineType.TASK) {
            IconButton(onClick = onArchiveClick) {
                Icon(iconResource(R.drawable.ic_archiving), contentDescription = null)
            }
        } else {
            IconButton(onClick = onReminderClick) {
                Icon(iconResource(R.drawable.ic_notification_add), contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onDeleteClick) {
            Icon(iconResource(R.drawable.ic_delete), contentDescription = null)
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onEditClick) {
            Icon(iconResource(R.drawable.ic_edit), contentDescription = null)
        }
    }
}
