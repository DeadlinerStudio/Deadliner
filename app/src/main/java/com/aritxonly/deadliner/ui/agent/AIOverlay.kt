package com.aritxonly.deadliner.ui.agent

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.CalendarContract
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.logging.AppLog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ai.IntentType
import com.aritxonly.deadliner.ai.AIToolRequest
import com.aritxonly.deadliner.ai.AgentLifecycleState
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.ai.TaskSummary
import com.aritxonly.deadliner.ai.ToolDecision
import com.aritxonly.deadliner.ai.UserProfile
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.edgeFade
import com.aritxonly.deadliner.ui.iconResource
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.sin

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AIOverlayHost(
    initialText: String,
    onAddDDL: (Intent) -> Unit,
    onRemoveFromWindow: () -> Unit,
    respondIme: Boolean = true
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 8 },
        exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 6 }
    ) {
        AIOverlay(
            initialText = initialText,
            onDismiss = { visibleState.targetState = false },
            onAddDDL = onAddDDL,
            respondIme = respondIme
        )
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            onRemoveFromWindow()
        }
    }
}

internal sealed class ChatDisplayItem(open val id: String) {
    data class UserQuery(val text: String, override val id: String = "user:${System.nanoTime()}") : ChatDisplayItem(id)
    data class AIChat(val text: String, override val id: String = "ai:${System.nanoTime()}") : ChatDisplayItem(id)
    data class AIThinking(val text: String, override val id: String = "thinking:${System.nanoTime()}") : ChatDisplayItem(id)
    data class AIError(val text: String, override val id: String = "error:${System.nanoTime()}") : ChatDisplayItem(id)
    data class AIToolRequestCard(
        val request: AIToolRequest,
        val status: ToolRequestStatus = ToolRequestStatus.Pending,
        override val id: String = "tool:${System.nanoTime()}"
    ) : ChatDisplayItem(id)
    data class AIToolResultCard(
        val summary: TaskSummary,
        override val id: String = "tool-result:${System.nanoTime()}"
    ) : ChatDisplayItem(id)
    data class AITask(val card: UiCard.TaskCard, override val id: String = "task:${System.nanoTime()}") : ChatDisplayItem(id)
    data class AIPlan(val card: UiCard.PlanBlockCard, override val id: String = "plan:${System.nanoTime()}") : ChatDisplayItem(id)
    data class AISteps(val card: UiCard.StepsCard, override val id: String = "steps:${System.nanoTime()}") : ChatDisplayItem(id)
}

internal enum class ToolRequestStatus {
    Pending,
    Approved,
    Rejected,
}

internal data class PendingToolApproval(
    val requestId: String,
    val deferred: CompletableDeferred<ToolDecision>,
)

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AIOverlay(
    initialText: String = "",
    onDismiss: () -> Unit,
    onAddDDL: (Intent) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    borderThickness: Dp = 2.dp,
    glowColors: List<Color> = listOf(Color(0xFF6AA9FF), Color(0xFFFFC36A), Color(0xFFFF6AE6)),
    hintText: String = stringResource(R.string.ai_overlay_enter_questions),
    respondIme: Boolean = true,
    presentation: AIAgentPresentation = AIAgentPresentation.Overlay,
    inputBottomPadding: Dp = 40.dp,
) {
    val chromePolicy = aiAgentChromePolicy(presentation)
    var textState by remember { mutableStateOf(TextFieldValue(initialText)) }
    val session = AIAgentSessionState
    var isLoading by session.loading
    var runJob by session.runJob

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDonatePrompt by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val messages = session.messages
    var pendingToolApproval by session.pendingApproval
    var streamBubbleId by session.streamBubbleId

    val panelAlpha = remember { Animatable(0f) }
    val panelTranslate = remember { Animatable(40f) }
    val glowAlpha = remember { Animatable(0f) }
    val hintAlpha = remember { Animatable(0f) }

    val quickPrompts = remember {
        listOf(
            "明天下午 3 点和产品开评审会",
            "每周 3 次力量训练，每次 40 分钟",
            "把这周要交付的 Deadliner 功能排一下优先级"
        )
    }

    LaunchedEffect(Unit) {
        if (GlobalUtils.shouldShowDeadlinerDonatePrompt()) {
            GlobalUtils.markDeadlinerDonatePromptShown()
            showDonatePrompt = true
        }
    }

    fun appendAssistantCards(cards: List<UiCard>) {
        cards.forEach { card ->
            when (card) {
                is UiCard.TaskCard -> messages.add(ChatDisplayItem.AITask(card))
                is UiCard.PlanBlockCard -> messages.add(ChatDisplayItem.AIPlan(card))
                is UiCard.StepsCard -> messages.add(ChatDisplayItem.AISteps(card))
            }
        }
    }

    if (showDonatePrompt) {
        AlertDialog(
            show = true,
            onDismissRequest = { showDonatePrompt = false },
            title = { Text(stringResource(R.string.deadliner_donate_plan_title)) },
            text = { Text(stringResource(R.string.deadliner_donate_plan_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDonatePrompt = false
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_INITIAL_ROUTE, SettingsRoute.Donate.route)
                        }
                    )
                }) {
                    Text(stringResource(R.string.deadliner_donate_go))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonatePrompt = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    fun appendStreamChunk(chunk: String) {
        if (chunk.isBlank()) return
        val existingId = streamBubbleId
        if (existingId == null) {
            val id = "stream:${System.nanoTime()}"
            streamBubbleId = id
            messages.add(ChatDisplayItem.AIChat(text = chunk, id = id))
            return
        }

        val idx = messages.indexOfFirst { it.id == existingId && it is ChatDisplayItem.AIChat }
        if (idx >= 0) {
            val cur = messages[idx] as ChatDisplayItem.AIChat
            messages[idx] = cur.copy(text = cur.text + chunk)
        } else {
            val id = "stream:${System.nanoTime()}"
            streamBubbleId = id
            messages.add(ChatDisplayItem.AIChat(text = chunk, id = id))
        }
    }

    fun finalizeStreamBubble(finalText: String?) {
        val text = finalText?.trim().orEmpty()
        val id = streamBubbleId ?: return
        val idx = messages.indexOfFirst { it.id == id && it is ChatDisplayItem.AIChat }
        if (idx >= 0) {
            if (text.isNotEmpty()) {
                val cur = messages[idx] as ChatDisplayItem.AIChat
                messages[idx] = cur.copy(text = text)
            } else if ((messages[idx] as ChatDisplayItem.AIChat).text.isBlank()) {
                messages.removeAt(idx)
            }
        }
        streamBubbleId = null
    }

    fun updateToolCardStatus(requestId: String, status: ToolRequestStatus) {
        val index = messages.indexOfFirst {
            it is ChatDisplayItem.AIToolRequestCard && it.request.id == requestId
        }
        if (index >= 0) {
            val current = messages[index] as ChatDisplayItem.AIToolRequestCard
            messages[index] = current.copy(status = status)
        }
    }

    fun summarizeRetrievedTasks(tasks: List<com.aritxonly.deadliner.ai.AITask>): TaskSummary {
        val now = LocalDateTime.now()
        val parsed = tasks.mapNotNull {
            runCatching { LocalDateTime.parse(it.dueTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
        }
        val overdue = parsed.count { it.isBefore(now) }
        val dueSoon24h = parsed.count { !it.isBefore(now) && it.isBefore(now.plusHours(24)) }
        return TaskSummary(
            count = tasks.size,
            overdue = overdue,
            dueSoon24h = dueSoon24h,
        )
    }

    fun buildAssistantSummary(intent: IntentType, cards: List<UiCard>): String {
        val taskCount = cards.count { it is UiCard.TaskCard }
        val planCount = cards.count { it is UiCard.PlanBlockCard }
        val stepCount = cards.count { it is UiCard.StepsCard }
        return when {
            cards.isEmpty() -> context.getString(R.string.ai_empty_hint)
            intent == IntentType.ExtractTasks -> "我帮你识别出了 $taskCount 个任务，已经整理好了。"
            intent == IntentType.PlanDay -> "我帮你生成了 $planCount 段日程安排，可以直接加到日历。"
            intent == IntentType.SplitToSteps -> "我把目标拆成了 $stepCount 份可执行步骤。"
            else -> "我整理好了你的请求结果，可以继续细化。"
        }
    }

    fun updateThinkingBubble(thinkingId: String, label: String) {
        val idx = messages.indexOfFirst { it.id == thinkingId && it is ChatDisplayItem.AIThinking }
        if (idx >= 0) {
            val current = messages[idx] as ChatDisplayItem.AIThinking
            if (current.text != label) {
                messages[idx] = current.copy(text = label)
            }
        }
    }

    fun removeThinkingBubble(thinkingId: String) {
        val idx = messages.indexOfFirst { it.id == thinkingId && it is ChatDisplayItem.AIThinking }
        if (idx >= 0) {
            messages.removeAt(idx)
        }
    }

    fun mapThinkingLabel(agent: String?, phase: String?, fallback: String?): String {
        val name = agent?.trim().takeUnless { it.isNullOrBlank() } ?: "Lifi AI"
        return when (phase?.trim()?.lowercase()) {
            "routing" -> "$name 正在思考"
            "working" -> "$name 正在处理"
            "tool_wait" -> "$name 正在等待工具结果"
            else -> fallback?.takeIf { it.isNotBlank() } ?: "$name 正在思考"
        }
    }

    fun mapLifecycleLabel(stage: String, status: String, state: AgentLifecycleState): String? {
        val s = stage.trim().lowercase()
        val st = status.trim().lowercase()
        return when {
            s == "process_input" && st == "started" -> "Lifi AI 正在思考"
            s == "tool_request" && st == "waiting" -> "Lifi AI 正在等待你确认工具调用"
            s == "submit_tool_result" && st == "started" -> "Lifi AI 正在继续处理"
            state == AgentLifecycleState.WaitingTool -> "Lifi AI 正在等待工具结果"
            state == AgentLifecycleState.Running -> "Lifi AI 正在思考"
            else -> null
        }
    }

    fun submitQuery() {
        val query = textState.text.trim()
        if (query.isBlank() || isLoading) return
        AppLog.d("AIOverlay", "submitQuery")
        val submittedToolsThisTurn = mutableSetOf<String>()
        val hideThinkingProcess = GlobalUtils.aiHideThinkingProcess
        val autoApproveReadTasks = GlobalUtils.aiAutoApproveReadTasks
        val silentTaskAdd = GlobalUtils.aiSilentTaskAdd

        // 防止上一轮遗留的待确认状态阻塞新一轮会话。
        pendingToolApproval?.deferred?.complete(ToolDecision.Reject("stale_session"))
        pendingToolApproval = null

        focusManager.clearFocus()
        textState = TextFieldValue("")
        messages.add(ChatDisplayItem.UserQuery(query))

        val thinking = if (hideThinkingProcess) {
            null
        } else {
            ChatDisplayItem.AIThinking("Lifi AI 正在思考...").also(messages::add)
        }

        runJob = session.scope.launch {
            isLoading = true
            try {
                val (_, json) = AIUtils.generateAutoInteractive(
                    context = context.applicationContext,
                    rawText = query,
                    profile = UserProfile(
                        preferredLang = null,
                        defaultEveningHour = 20,
                        defaultReminderMinutes = listOf(30)
                    ),
                    preferLLM = true,
                    onTextStream = { chunk ->
                        withContext(Dispatchers.Main) {
                            appendStreamChunk(chunk)
                        }
                    },
                    onToolRequest = { request ->
                        val normalizedTool = request.tool.trim()
                            .replace("-", "")
                            .replace("_", "")
                            .lowercase()

                        if (autoApproveReadTasks && (normalizedTool == "readtasks" || normalizedTool == "readhabits")) {
                            submittedToolsThisTurn += normalizedTool
                            return@generateAutoInteractive ToolDecision.Approve
                        }
                        if (silentTaskAdd && normalizedTool == "createtask") {
                            submittedToolsThisTurn += normalizedTool
                            return@generateAutoInteractive ToolDecision.Approve
                        }

                        val deferred = CompletableDeferred<ToolDecision>()
                        withContext(Dispatchers.Main) {
                            messages.add(ChatDisplayItem.AIToolRequestCard(request))
                            pendingToolApproval = PendingToolApproval(request.id, deferred)
                        }
                        val decision = deferred.await()
                        if (decision is ToolDecision.Approve) {
                            submittedToolsThisTurn += request.tool.trim()
                                .replace("-", "")
                                .replace("_", "")
                                .lowercase()
                        }
                        withContext(Dispatchers.Main) {
                            updateToolCardStatus(
                                request.id,
                                if (decision is ToolDecision.Approve) ToolRequestStatus.Approved else ToolRequestStatus.Rejected
                            )
                            pendingToolApproval = null
                        }
                        decision
                    },
                    onThinking = { agent, phase, message ->
                        if (thinking != null) {
                            withContext(Dispatchers.Main) {
                                updateThinkingBubble(
                                    thinking.id,
                                    mapThinkingLabel(agent = agent, phase = phase, fallback = message)
                                )
                            }
                        }
                    },
                    onLifecycle = { evt ->
                        AppLog.d(
                            "AIOverlay",
                            "core lifecycle: stage=${evt.stage}, status=${evt.status}, requestId=${evt.requestId}, state=${evt.state}"
                        )
                        if (thinking != null) {
                            withContext(Dispatchers.Main) {
                                mapLifecycleLabel(evt.stage, evt.status, evt.state)?.let { label ->
                                    updateThinkingBubble(thinking.id, label)
                                }
                            }
                        }
                    },
                )

                val mixed = AIUtils.parseMixedResult(json)
                val (primary, cards0) = mapMixedToUiCards(mixed)
                val shouldSuppressProposalCards = submittedToolsThisTurn.any {
                    it == "readtasks" || it == "readhabits" || it == "createtask"
                }
                val cards = if (shouldSuppressProposalCards) {
                    cards0.filterNot { it is UiCard.TaskCard }
                } else {
                    cards0
                }
                thinking?.let { removeThinkingBubble(it.id) }
                val chat = mixed.chatResponse?.trim().orEmpty()
                if (chat.isNotEmpty()) {
                    if (streamBubbleId != null) finalizeStreamBubble(chat) else messages.add(ChatDisplayItem.AIChat(chat))
                } else {
                    if (streamBubbleId == null) {
                        messages.add(ChatDisplayItem.AIChat(buildAssistantSummary(primary, cards)))
                    } else {
                        finalizeStreamBubble(null)
                    }
                }
                if (mixed.retrievedTasks.isNotEmpty()) {
                    messages.add(ChatDisplayItem.AIToolResultCard(summarizeRetrievedTasks(mixed.retrievedTasks)))
                }
                appendAssistantCards(cards)
            } catch (t: Throwable) {
                thinking?.let { removeThinkingBubble(it.id) }
                finalizeStreamBubble(null)
                val err = if (t is CancellationException) "本轮已停止；已提交的操作不会自动撤销。"
                    else t.message ?: context.getString(R.string.ai_parse_failed)
                messages.add(ChatDisplayItem.AIError(err))
            } finally {
                isLoading = false
                pendingToolApproval = null
            }
        }
    }

    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        panelAlpha.animateTo(1f, tween(320, delayMillis = 60, easing = FastOutSlowInEasing))
        panelTranslate.animateTo(0f, tween(420, delayMillis = 60, easing = FastOutSlowInEasing))
        hintAlpha.animateTo(1f, tween(240, delayMillis = 120, easing = LinearOutSlowInEasing))
        focusRequester.requestFocus()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            runCatching {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    BackHandler(enabled = chromePolicy.handleBackPress) { onDismiss() }

    val density = LocalDensity.current
    var parentHeightPx by remember { mutableIntStateOf(0) }
    var hintBottomPx by remember { mutableFloatStateOf(0f) }
    var closeBottomPx by remember { mutableFloatStateOf(0f) }
    var toolbarHeightPx by remember { mutableIntStateOf(0) }

    val topSafePadding = with(density) {
        val closeBased = closeBottomPx + 8.dp.toPx()
        val closeFallback = 84.dp.toPx()
        val minTop = max(closeBased, closeFallback)
        if (messages.isEmpty() && !isLoading) {
            val hintBased = hintBottomPx + 8.dp.toPx()
            max(minTop, hintBased).toDp()
        } else {
            minTop.toDp()
        }
    }
    val bottomSafePadding = with(density) {
        val padPx = toolbarHeightPx.toFloat() + 4.dp.toPx()
        max(0f, padPx).toDp()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (respondIme) Modifier.imePadding() else Modifier)
            .onGloballyPositioned { parentHeightPx = it.size.height }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        val wobblePx = rememberScreenScaledWobbleDp(fractionOfMinSide = 0.2f)

        GlowScrim(
            modifier = Modifier.align(Alignment.BottomCenter),
            height = 260.dp,
            blur = 60.dp,
            opacity = glowAlpha.value,
            jitterEnabled = true,
            jitterRadius = wobblePx,
            freqBlue = 0.20f,
            freqPink = 0.18f,
            freqAmber = 0.15f,
            freqBreathe = 0.125f
        )

        if (messages.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .graphicsLayer { alpha = hintAlpha.value }
                    .onGloballyPositioned { hintBottomPx = it.boundsInParent().bottom }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_overlay_hint_top),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (chromePolicy.showDismissButton) {
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                },
                modifier = (Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f),
                        shape = CircleShape
                    )
                    .size(36.dp)
                    .onGloballyPositioned { closeBottomPx = it.boundsInParent().bottom }).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)
            ) {
                Icon(
                    painter = iconResource(R.drawable.msr_close_wght400),
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (messages.isEmpty() && !isLoading) {
            InitialGuideView(
                onApplyPrompt = { prompt -> textState = TextFieldValue(prompt) },
                prompts = quickPrompts,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .padding(top = topSafePadding + 20.dp, bottom = bottomSafePadding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 16.dp, end = 16.dp)
                    .padding(top = topSafePadding, bottom = bottomSafePadding)
                    .fillMaxSize()
                    .edgeFade(top = 16.dp, bottom = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { item ->
                    when (item) {
                        is ChatDisplayItem.UserQuery -> UserBubble(item.text)
                        is ChatDisplayItem.AIChat -> AiBubble(item.text)
                        is ChatDisplayItem.AIThinking -> ThinkingBubble(item.text)
                        is ChatDisplayItem.AIError -> ErrorBubble(item.text)
                        is ChatDisplayItem.AIToolRequestCard -> ToolRequestCard(
                            item = item,
                            enabled = pendingToolApproval?.requestId == item.request.id && item.status == ToolRequestStatus.Pending,
                            onApprove = {
                                pendingToolApproval?.takeIf { it.requestId == item.request.id }?.deferred?.complete(ToolDecision.Approve)
                            },
                            onReject = {
                                pendingToolApproval?.takeIf { it.requestId == item.request.id }?.deferred?.complete(
                                    ToolDecision.Reject("user_rejected")
                                )
                                messages.add(ChatDisplayItem.AIChat("好的，我不会读取你的任务列表。你可以再补充一些上下文，我继续帮你。"))
                            }
                        )
                        is ChatDisplayItem.AIToolResultCard -> ToolResultBubble(item.summary)
                        is ChatDisplayItem.AITask -> TaskCardView(
                            card = item.card,
                            onAdd = {
                                item.card.toGeneratedDDLOrNull()?.let { ddl ->
                                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                                        putExtra("EXTRA_CURRENT_TYPE", 0)
                                        putExtra("EXTRA_GENERATE_DDL", ddl)
                                    }
                                    onAddDDL(intent)
                                }
                            },
                            onCopy = {
                                item.card.toGeneratedDDLOrNull()?.let { ddl ->
                                    val ddlItem = GeneratedDDL(
                                        name = ddl.name,
                                        dueTime = ddl.dueTime,
                                        note = ddl.note
                                    )
                                    val url = DeadlinerURLScheme.encodeWithPassphrase(
                                        ddlItem,
                                        "deadliner-2025".toCharArray()
                                    )
                                    clipboard.setText(AnnotatedString(url))
                                }
                            }
                        )
                        is ChatDisplayItem.AIPlan -> PlanBlockCardView(item.card) {
                            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            val start = runCatching {
                                LocalDateTime.parse(item.card.start, fmt)
                            }.getOrNull()
                            val end = runCatching {
                                LocalDateTime.parse(item.card.end, fmt)
                            }.getOrNull()
                            val startMillis = start?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                            val endMillis = end?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, item.card.title)
                                if (!item.card.location.isNullOrBlank()) {
                                    putExtra(CalendarContract.Events.EVENT_LOCATION, item.card.location)
                                }
                                if (!item.card.linkTask.isNullOrBlank()) {
                                    putExtra(
                                        CalendarContract.Events.DESCRIPTION,
                                        context.getString(R.string.linked_task, item.card.linkTask)
                                    )
                                }
                                if (startMillis != null) {
                                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                                }
                                if (endMillis != null) {
                                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                                }
                            }
                            context.startActivity(intent)
                        }
                        is ChatDisplayItem.AISteps -> StepsCardView(
                            card = item.card,
                            onCreateSubtasks = { title, checklist ->
                                clipboard.setText(
                                    AnnotatedString(
                                        (listOf(title) + checklist.mapIndexed { index, value ->
                                            "${index + 1}. $value"
                                        }).joinToString("\n")
                                    )
                                )
                                Toast.makeText(context, R.string.copy_list, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }

        val toolbarShape = RoundedCornerShape(percent = 50)
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    start = 24.dp,
                    top = 40.dp,
                    end = 24.dp,
                    bottom = inputBottomPadding,
                )
                .graphicsLayer {
                    alpha = panelAlpha.value
                    translationY = panelTranslate.value
                }
                .onGloballyPositioned { toolbarHeightPx = it.size.height }
                .glowingWobbleBorder(
                    shape = toolbarShape,
                    colors = glowColors,
                    stroke = borderThickness,
                    wobblePx = wobblePx,
                    breatheAmp = 0.10f
                ),
            trailingContent = {
                IconButton(modifier = Modifier.miuixPressFeedback(enabled = isLoading || textState.text.isNotBlank(), shape = androidx.compose.material3.IconButtonDefaults.standardShape),
                    onClick = {
                        if (isLoading) {
                            runJob?.cancel()
                            pendingToolApproval?.deferred?.complete(ToolDecision.Reject("user_cancelled"))
                        } else submitQuery()
                    },
                    enabled = isLoading || textState.text.isNotBlank()
                ) {
                    Icon(
                        painter = com.aritxonly.deadliner.ui.iconResource(
                            if (isLoading) R.drawable.msr_close_wght400 else R.drawable.msr_send_wght400),
                        contentDescription = if (isLoading) "停止" else stringResource(R.string.send),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.dp
            val buttonWidth = 48.dp
            val horizontalPadding = 32.dp
            val textFieldWidth = screenWidthDp - buttonWidth - horizontalPadding * 2

            BasicTextField(
                value = textState,
                onValueChange = { textState = it },
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                modifier = Modifier
                    .width(textFieldWidth)
                    .padding(start = 8.dp)
                    .heightIn(max = 80.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitQuery() }),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            text = hintText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
