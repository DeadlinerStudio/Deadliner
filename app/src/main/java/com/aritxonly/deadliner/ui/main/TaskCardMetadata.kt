package com.aritxonly.deadliner.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.InterfaceIconPack
import com.aritxonly.deadliner.ui.LocalInterfaceIconPack
import com.aritxonly.deadliner.ui.iconResource

@Composable
internal fun TaskCardTitleRow(title: String, remaining: String, starred: Boolean, starColor: Color) {
    val hybrid = LocalInterfaceIconPack.current == InterfaceIconPack.HybridMiuix
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (starred) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(iconResource(R.drawable.msr_star_fill_wght400), null, tint = starColor,
                    modifier = Modifier.size(24.dp).graphicsLayer {
                        scaleX = if (hybrid) 0.9f else 1f
                        scaleY = if (hybrid) 0.9f else 1f
                    }.testTag("task-card-star"))
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(end = 4.dp))
        Text(remaining, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun TaskCardMetadataRow(
    note: String,
    categoryContent: (@Composable () -> Unit)?,
    completedSubtasks: Int,
    totalSubtasks: Int,
) {
    val hasSubtasks = totalSubtasks > 0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        categoryContent?.let { category ->
            Box(Modifier.widthIn(max = 128.dp)) { category() }
        }
        if (hasSubtasks) {
            if (categoryContent != null) Spacer(Modifier.width(6.dp))
            val completed = completedSubtasks.coerceIn(0, totalSubtasks)
            val description = stringResource(R.string.task_card_subtask_progress, completed, totalSubtasks)
            Text("$completed/$totalSubtasks", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.testTag("task-card-subtasks")
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp).semantics { contentDescription = description })
        }
        if (note.isNotEmpty()) {
            if (categoryContent != null || hasSubtasks) Spacer(Modifier.width(6.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}
