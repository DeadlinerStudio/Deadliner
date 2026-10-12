package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun QuickAddModelDownloadStatus(state: QuickAddState) {
    val status by QuickAddModelService.status.collectAsState()
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val progress = if (status.totalBytes > 0) (status.downloadedBytes.toFloat() / status.totalBytes).coerceIn(0f, 1f) else 0f
    val label = when (status.phase) {
        "CHECKING" -> "正在检查识别模型缓存"
        "DOWNLOADING" -> "正在下载识别模型 · ${(progress * 100).toInt()}%"
        "LOADING" -> "正在加载识别模型"
        "READY" -> "端侧识别模型已就绪"
        "FAILED" -> "模型不可用，当前使用规则识别"
        "CANCELLED" -> "模型下载已暂停，当前使用规则识别"
        else -> "模型尚未配置，当前使用规则识别"
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (status.phase == "DOWNLOADING") LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        if (status.phase == "FAILED" || status.phase == "CANCELLED") Text("重试", color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.heightIn(min = 48.dp).clickable {
                scope.launch { QuickAddModelService.prepare(context); state.refreshWithModel() }
            }.padding(vertical = 12.dp))
    }
}
