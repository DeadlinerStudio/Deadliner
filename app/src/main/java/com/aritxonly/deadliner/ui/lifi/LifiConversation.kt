package com.aritxonly.deadliner.ui.lifi

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults

@Composable
internal fun LifiActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = false) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, minHeight = 44.dp, cornerRadius = 16.dp,
        colors = if (primary) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
}

@Composable
internal fun LifiConversationItem(message: LifiMessage, readOnly: Boolean, running: Boolean, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    when (message.kind) {
        LifiKind.USER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(message.text, modifier = Modifier.widthIn(max = 520.dp).background(colors.primaryContainer.copy(alpha = .7f), RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)).padding(16.dp), color = colors.onPrimaryContainer)
        }
        LifiKind.TEXT -> if (message.text.isNotBlank()) Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Lifi", color = colors.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp)); Text(message.text, style = MaterialTheme.typography.bodyLarge)
        }
        LifiKind.PROCESS -> {
            var expanded by remember(message.id) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainer, RoundedCornerShape(20.dp)).clickable { expanded = !expanded }.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text((when (message.state) { LifiActionState.EXECUTING -> "◌"; LifiActionState.SUCCEEDED -> "✓"; else -> "·" }) + "  ${message.text}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(if (expanded) "收起" else "查看过程", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
                if (message.state == LifiActionState.EXPIRED && !readOnly) LifiActionButton("重新处理", onRetry, Modifier.padding(top = 10.dp), enabled = !running)
                if (expanded) Text(message.detail.ifBlank { "正在连接模型与整理上下文" }, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        LifiKind.ACTION -> {
            val write = !message.tool.startsWith("read_")
            Column(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(lifiToolTitle(message.tool), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(lifiActionSummary(message.tool, message.arguments), style = MaterialTheme.typography.bodyLarge)
                if (message.detail.isNotBlank()) Text(message.detail, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (message.state == LifiActionState.SUCCEEDED && message.tool.startsWith("read_")) LifiReadResults(message.resultPayload, message.tool)
                Text(when (message.state) {
                    LifiActionState.PENDING -> "历史记录 · 无法执行"
                    LifiActionState.EXECUTING -> "正在执行…"
                    LifiActionState.SUCCEEDED -> if (write) "✓ 已完成" else "✓ 已读取"
                    LifiActionState.REJECTED -> "已拒绝"
                    LifiActionState.FAILED -> "执行未完成"
                    LifiActionState.EXPIRED -> "确认已失效"
                }, style = MaterialTheme.typography.labelMedium, color = if (message.state == LifiActionState.FAILED) colors.error else colors.primary)
            }
        }
        LifiKind.RESULT -> Column(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(18.dp)) {
            Text(message.text, style = MaterialTheme.typography.titleSmall)
            if (message.detail.isNotBlank()) Text(message.detail, Modifier.padding(top = 8.dp), color = colors.onSurfaceVariant)
        }
        LifiKind.ERROR -> Column(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(18.dp)) {
            Text(message.text, color = colors.error)
            if (!readOnly) LifiActionButton("重试", onRetry, Modifier.padding(top = 12.dp), enabled = !running)
        }
    }
}

@Composable
private fun LifiReadResults(payload: String, tool: String) {
    var expanded by remember(payload) { mutableStateOf(false) }
    val results = lifiArguments(payload).get(if (tool == "read_tasks") "tasks" else "habits")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { entry -> entry.isJsonObject }?.asJsonObject }.orEmpty()
    Text("读取到 ${results.size} 项", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    results.take(if (expanded) results.size else 3).forEach { result ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(result.label("name"), style = MaterialTheme.typography.bodyMedium)
            val detail = if (tool == "read_tasks") listOf(result.label("due"), result.label("notePreview")).filter(String::isNotBlank).joinToString(" · ") else lifiActionSummary(tool = "create_habit", raw = result.toString()).substringAfter('\n', "")
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (results.size > 3) LifiActionButton(if (expanded) "收起" else "查看全部", { expanded = !expanded })
}
