package com.aritxonly.deadliner.ui.lifi

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.iconResource

@Composable
internal fun LifiHistorySheet(show: Boolean, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val history = LifiSession.history.toList()
    MiuixBottomSheet(show = show, onDismissRequest = onDismiss, title = "对话历史",
        endAction = { TopBarIconButton(onClick = onDismiss) { Icon(iconResource(R.drawable.msr_close_wght400), "关闭对话历史") } }) { sheetPadding ->
        val sheetListState = rememberLazyListState()
        LazyColumn(state = sheetListState, modifier = Modifier.syncTopBarMaterialScroll(sheetListState).fillMaxWidth(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = sheetPadding.calculateTopPadding() + 12.dp, bottom = sheetPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("从上次的想法接着走。历史记录仅供查看。", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            item { LifiActionButton("返回当前对话", { LifiSession.view(null); onDismiss() }, Modifier.fillMaxWidth()) }
            item { Text("历史对话 · ${history.size}", style = MaterialTheme.typography.titleSmall) }
            if (history.isEmpty()) item {
                Text("还没有历史对话。开始新对话时，上一段会留在这里。",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(18.dp), color = colors.onSurfaceVariant)
            }
            items(history, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable(role = Role.Button) { LifiSession.view(item.id); onDismiss() }.padding(10.dp)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        Text(java.text.DateFormat.getDateInstance().format(java.util.Date(item.createdAt)) + " · ${item.messages.size} 条",
                            Modifier.padding(top = 6.dp), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    LifiActionButton("删除", { LifiSession.deleteHistory(item.id) })
                }
            }
        }
    }
}
