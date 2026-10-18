package com.aritxonly.deadliner.ui.lifi

import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.LocalNavigationOverlayContentPadding
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.Scaffold
import com.aritxonly.deadliner.ui.base.FloatingNavigationBottomSpacing
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingTabBarDefaults
import com.aritxonly.deadliner.ui.quickadd.quickAddPanelGapAboveControls
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.ProgressiveTopBarMaterial
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.navigation.LocalFloatingNavigationBottomLift
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
fun LifiAIPage(navigationOverlaysContent: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var showHistory by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }
    val readOnly = LifiSession.viewing != null
    val allMessages = if (readOnly) LifiSession.history.firstOrNull { it.id == LifiSession.viewing }?.messages.orEmpty() else LifiSession.messages.toList()
    // Old saved approval cards are never actionable or automatically replayed.
    val visible = allMessages.filter { it.kind != LifiKind.ACTION || it.state == LifiActionState.FAILED }
    val density = LocalDensity.current
    var inputStackHeight by remember { mutableStateOf(200.dp) }
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    var headerHeight by remember { mutableStateOf(64.dp + statusBarHeight) }
    val topBarProgress by remember(density, listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / with(density) { 80.dp.toPx() }).coerceIn(0f, 1f)
        }
    }
    val actionMaterialProgress by animateFloatAsState(
        targetValue = topBarProgress,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "lifiTopBarActions",
    )
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val bottomSpace = if (imeVisible) 12.dp else if (navigationOverlaysContent) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            FloatingNavigationBottomSpacing + MiuixFloatingTabBarDefaults.Height +
            quickAddPanelGapAboveControls(MiuixFloatingTabBarDefaults.Height) +
            LocalFloatingNavigationBottomLift.current + LocalNavigationOverlayContentPadding.current
    } else 20.dp
    val backdrop = rememberLayerBackdrop { drawRect(colors.surface); drawContent() }
    LaunchedEffect(Unit) { LifiSession.initialize() }
    LaunchedEffect(LifiSession.viewing) { listState.scrollToItem(0) }
    LaunchedEffect(visible.size, visible.lastOrNull()?.text) {
        if (!readOnly && visible.isNotEmpty() && (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= visible.size - 3) {
            listState.animateScrollToItem(visible.lastIndex)
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize().background(colors.surface).imePadding(),
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            val wide = maxWidth >= 760.dp
            CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop, LocalTopBarScrollProgress provides topBarProgress, LocalTopBarButtonMaterialProgress provides actionMaterialProgress) {
                Row(Modifier.fillMaxSize()) {
                    if (wide) LifiHistoryRail(Modifier.width(240.dp).fillMaxHeight().padding(top = headerHeight), LifiSession.ready)
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        // Composer and header are outside this recorder; no glass self-sampling.
                        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(colors.surface)) {
                            Canvas(Modifier.fillMaxSize()) {
                                val center = Offset(size.width * .5f, size.height * .92f)
                                val radius = size.width * .8f
                                drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha = .09f), colors.primary.copy(alpha = 0f)), center, radius), radius, center)
                            }
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = if (wide) 36.dp else 22.dp, end = if (wide) 36.dp else 22.dp, top = headerHeight + 12.dp, bottom = bottomSpace + inputStackHeight + 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (LifiSession.storageError.isNotBlank()) item {
                                    Column { Text(LifiSession.storageError, color = colors.error); if (!LifiSession.ready) LifiActionButton("重新读取", LifiSession::initialize) }
                                }
                                if (visible.isEmpty()) item { LifiWelcome(LifiSession::editDraft, LifiSession.ready && !readOnly, autoAdvance = !showHistory && !showMemory && LifiSession.draft.isBlank()) }
                                items(visible, key = { it.id }) { message ->
                                    LifiConversationItem(message, readOnly, LifiSession.running) { LifiSession.submit(context, retry = true) }
                                }
                            }
                        }
                        // Measure the input stack inside its outer dock clearance; reserve both below.
                        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 720.dp).fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = bottomSpace).onSizeChanged { inputStackHeight = with(density) { it.height.toDp() } }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (readOnly) LifiActionButton("返回当前对话", { LifiSession.view(null) }, Modifier.fillMaxWidth(), primary = true)
                            else {
                                LifiMemoryButton(onClick = { showMemory = true }, modifier = Modifier.align(Alignment.CenterHorizontally))
                                LifiComposer(LifiSession.draft, LifiSession::editDraft, LifiSession.ready && !LifiSession.busy, LifiSession.running, { LifiSession.submit(context) }, LifiSession::stop)
                            }
                        }
                    }
                }
                ProgressiveTopBarMaterial(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
                ) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (readOnly) Text("历史对话 · 只读", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        else if (LifiSession.running) Text("正在处理", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.weight(1f))
                        TopBarIconButton(onClick = { showHistory = true }) { Icon(iconResource(R.drawable.msr_history_wght400), "对话历史") }
                        Spacer(Modifier.width(8.dp))
                        TopBarIconButton(onClick = LifiSession::newConversation, enabled = !LifiSession.busy && LifiSession.ready) { Icon(iconResource(R.drawable.ic_lifi_new_conversation), "新建对话") }
                    }
                }
                LifiMemorySheet(showMemory) { showMemory = false }
                LifiHistorySheet(showHistory) { showHistory = false }
            }
        }
    }
}

@Composable
private fun LifiHistoryRail(modifier: Modifier, enabled: Boolean) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LifiActionButton("新对话", LifiSession::newConversation, Modifier.fillMaxWidth(), enabled = enabled && !LifiSession.busy, primary = true)
        LifiActionButton("当前对话", { LifiSession.view(null) }, Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(LifiSession.history.toList(), key = { it.id }) { item ->
                Column(Modifier.fillMaxWidth().clickable { LifiSession.view(item.id) }.padding(8.dp)) {
                    Text(item.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                    Text(java.text.DateFormat.getDateInstance().format(java.util.Date(item.createdAt)), Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
