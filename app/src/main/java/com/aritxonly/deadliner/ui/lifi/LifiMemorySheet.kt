package com.aritxonly.deadliner.ui.lifi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.iconResource
import com.deadliner.domain.memory.MemoryFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shared sheet with readable SurfaceContainer cards and no nested glass controls. */
@Composable
internal fun LifiMemorySheet(show: Boolean, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var fragments by remember { mutableStateOf<List<MemoryFragment>>(emptyList()) }
    var profile by remember { mutableStateOf("") }
    var savedProfile by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    suspend fun refresh(loadProfile: Boolean = false) {
        loading = true
        val loaded = runCatching { withContext(Dispatchers.IO) {
            KmpV6Runtime.database.memory.list().filterNot { it.isDeleted } to
                if (loadProfile) KmpV6Runtime.database.memory.getUserProfile() else null
        } }
        loaded.onSuccess { (items, preferences) ->
            fragments = items
            if (preferences != null) { profile = preferences; savedProfile = preferences }
            error = ""
        }.onFailure { error = it.message ?: "记忆读取失败" }
        loading = false
    }
    LaunchedEffect(show) { if (show) refresh(loadProfile = true) }
    MiuixBottomSheet(
        show = show,
        title = "记忆与偏好",
        onDismissRequest = onDismiss,
        endAction = {
            TopBarIconButton(onClick = onDismiss) {
                Icon(iconResource(R.drawable.msr_close_wght400), "关闭记忆与偏好")
            }
        },
    ) { sheetPadding ->
        val sheetListState = rememberLazyListState()
        LazyColumn(state = sheetListState, modifier = Modifier.syncTopBarMaterialScroll(sheetListState).fillMaxWidth(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = sheetPadding.calculateTopPadding() + 12.dp, bottom = sheetPadding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("这些内容帮助 Lifi 理解你的习惯和偏好。", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (loading) item { Text("正在读取…", color = colors.onSurfaceVariant) }
            if (error.isNotBlank()) item {
                Column(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(18.dp)) {
                    Text(error, color = colors.error)
                    LifiActionButton("重新读取", { scope.launch { refresh(loadProfile = true) } }, enabled = !loading)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("偏好", style = MaterialTheme.typography.titleMedium)
                    BasicTextField(profile, { profile = it }, enabled = !loading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface), cursorBrush = SolidColor(colors.primary),
                        decorationBox = { inner -> Box { if (profile.isEmpty()) Text("例如：工作日晚上安排学习，提前 30 分钟提醒", color = colors.onSurfaceVariant); inner() } })
                    LifiActionButton("保存偏好", {
                        val value = profile
                        scope.launch {
                            loading = true
                            runCatching { withContext(Dispatchers.IO) { KmpV6Runtime.database.memory.updateUserProfile(value) } }
                                .onSuccess { savedProfile = value; error = "" }.onFailure { error = it.message ?: "偏好保存失败" }
                            loading = false
                        }
                    }, Modifier.fillMaxWidth(), enabled = !loading && profile != savedProfile, primary = true)
                }
            }
            item { Text("记忆", style = MaterialTheme.typography.titleMedium) }
            if (fragments.isEmpty() && !loading && error.isBlank()) item {
                Text("还没有记忆。随着交流，Lifi 会逐渐了解你。", Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(20.dp), color = colors.onSurfaceVariant)
            }
            items(fragments, key = { it.uid }) { fragment ->
                Column(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(fragment.content, color = colors.onSurface)
                    LifiActionButton("删除这条记忆", {
                        scope.launch {
                            loading = true
                            runCatching { withContext(Dispatchers.IO) { KmpV6Runtime.database.memory.delete(fragment.uid, java.time.Instant.now().toString()) } }
                                .onSuccess { refresh() }.onFailure { error = it.message ?: "记忆删除失败"; loading = false }
                        }
                    }, enabled = !loading)
                }
            }
        }
    }
}
