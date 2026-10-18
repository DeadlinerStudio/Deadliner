package com.aritxonly.deadliner.ui.lifi

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LifiSuggestions = listOf(
    Triple("安排一个任务", "把要做的事，变成清晰的安排", "明天下午 3 点和产品开评审会"),
    Triple("建立一个习惯", "从一个小小的坚持开始", "帮我建立每周 3 次力量训练的习惯，每次 40 分钟"),
    Triple("帮我理清计划", "一起找到下一步的重点", "帮我把这周的任务排一下优先级"),
)

@Composable
internal fun LifiWelcome(onSuggestion: (String) -> Unit, enabled: Boolean, autoAdvance: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val pager = rememberPagerState(pageCount = { LifiSuggestions.size })
    val scope = rememberCoroutineScope()
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(pager.settledPage, dragging, enabled, autoAdvance, lifecycleOwner) {
        if (!enabled || !autoAdvance || dragging) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(4_500)
                if (!pager.isScrollInProgress) {
                    pager.animateScrollToPage((pager.settledPage + 1) % LifiSuggestions.size)
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(iconResource(R.drawable.ic_lifi), "Lifi", Modifier.size(48.dp), tint = colors.primary)
        Text("你好，欢迎回来", Modifier.padding(top = 8.dp), color = colors.onSurfaceVariant)
        Text("把想法告诉我，\n我们一起往前走。", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("安排任务、建立习惯，或一起理清接下来的计划。", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        HorizontalPager(state = pager, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth()) { index ->
            val item = LifiSuggestions[index]
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainer, RoundedCornerShape(22.dp))
                .clickable(enabled = enabled, role = Role.Button) { onSuggestion(item.third) }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(iconResource(listOf(R.drawable.msr_task_alt_wght400, R.drawable.msr_routine_wght400, R.drawable.msr_lightbulb_wght400)[index]),
                    contentDescription = null, modifier = Modifier.padding(end = 16.dp).size(24.dp), tint = colors.primary)
                Column(Modifier.weight(1f)) {
                    Text(item.first, fontWeight = FontWeight.Medium)
                    Text(item.second, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
                }
                Icon(iconResource(R.drawable.msr_chevron_right_wght400), null, Modifier.size(com.aritxonly.deadliner.ui.interfaceIconSize(20.dp, 16.dp)), tint = colors.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(LifiSuggestions.size) { index ->
                Box(Modifier.size(32.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "查看${LifiSuggestions[index].first}") {
                    scope.launch { pager.animateScrollToPage(index) }
                }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = if (pager.currentPage == index) 18.dp else 6.dp, height = 6.dp)
                        .background(if (pager.currentPage == index) colors.primary else colors.onSurfaceVariant.copy(alpha = .25f), CircleShape))
                }
            }
        }
    }
}


@Composable
internal fun LifiMemoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Text("记忆与偏好  ›", modifier.clip(shape).background(colors.surfaceContainer, shape)
        .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        color = colors.onSurface, style = MaterialTheme.typography.labelMedium)
}
