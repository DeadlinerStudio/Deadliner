package com.aritxonly.deadliner.ui.agent

import androidx.compose.ui.draw.clip
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

@Composable
internal fun ToolRequestCard(
    item: ChatDisplayItem.AIToolRequestCard,
    enabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val toolKey = item.request.tool.trim().replace("-", "").replace("_", "").lowercase()
    val statusText = when (item.status) {
        ToolRequestStatus.Pending -> "等待你的确认"
        ToolRequestStatus.Approved -> "已允许一次"
        ToolRequestStatus.Rejected -> "已拒绝"
    }
    val args = item.request.args
    val days = args.timeRangeDays ?: 7
    val st = args.status ?: "OPEN"
    val kws = args.keywords.orEmpty().joinToString("、")
    val rangeText = "范围：未来 $days 天 · 状态：$st" + if (kws.isBlank()) "" else " · 关键词：$kws"
    val title = when (toolKey) {
        "readtasks" -> "需要读取任务列表"
        "readhabits" -> "需要读取习惯列表"
        "createtask" -> "需要创建任务"
        "createhabit" -> "需要创建习惯"
        else -> "需要执行工具调用"
    }
    val fallbackReason = when (toolKey) {
        "readtasks" -> "为了更准确地回答你，我需要读取你的任务列表。"
        "readhabits" -> "为了准确整理你的习惯信息，我需要读取习惯列表。"
        "createtask" -> "我将根据你的要求创建一个新任务。"
        "createhabit" -> "我将根据你的需求创建一个新习惯。"
        else -> "为了继续完成你的请求，我需要执行一次工具调用。"
    }
    val requestItems = parseCreateRequestItems(item.request.rawArgsJson, toolKey)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(item.request.reason ?: fallbackReason, style = MaterialTheme.typography.bodyMedium)
        if (toolKey == "readtasks") {
            Text(rangeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (requestItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                requestItems.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(statusText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

        if (item.status == ToolRequestStatus.Pending) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = enabled,
                    modifier = (Modifier.weight(1f)).miuixPressFeedback(enabled = enabled, shape = androidx.compose.material3.ButtonDefaults.outlinedShape)
                ) {
                    Text("拒绝")
                }
                Button(
                    onClick = onApprove,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("允许一次")
                }
            }
        }

    }
}

internal fun parseCreateRequestItems(rawArgsJson: String?, toolKey: String): List<String> {
    if (rawArgsJson.isNullOrBlank()) return emptyList()
    val obj = runCatching { JsonParser.parseString(rawArgsJson).asJsonObject }.getOrNull() ?: return emptyList()
    val args = if (obj.has("args") && obj.get("args").isJsonObject) obj.getAsJsonObject("args") else obj
    return when (toolKey) {
        "createtask" -> {
            val fromBatch = if (args.has("tasks") && args.get("tasks").isJsonArray) args.getAsJsonArray("tasks").toList() else emptyList()
            val source = if (fromBatch.isNotEmpty()) fromBatch else listOf(args)
            source.mapNotNull { node ->
                if (!node.isJsonObject) return@mapNotNull null
                val task = node.asJsonObject
                val name = task.getString("name")
                val due = task.getString("dueTime")
                val note = task.getString("note")
                "• ${name.ifBlank { "(未命名)" }} ｜ 截止：${due.ifBlank { "-" }} ｜ 备注：${if (note.isBlank()) "-" else note}"
            }
        }
        "createhabit" -> {
            val fromBatch = if (args.has("habits") && args.get("habits").isJsonArray) args.getAsJsonArray("habits").toList() else emptyList()
            val source = if (fromBatch.isNotEmpty()) fromBatch else listOf(args)
            source.mapNotNull { node ->
                if (!node.isJsonObject) return@mapNotNull null
                val habit = node.asJsonObject
                val name = habit.getString("name")
                val schedule = habit.getObject("schedule")
                val goal = habit.getObject("goal")
                val tracking = habit.getObject("tracking")
                val scheduleKind = schedule?.getString("kind").orEmpty().ifBlank { habit.getString("period") }
                val weekdays = schedule?.getString("weekdaysMask").orEmpty()
                val scheduleText = if (weekdays.isBlank()) scheduleKind else "$scheduleKind($weekdays)"
                val target = goal?.getString("targetCount").orEmpty().ifBlank { habit.getString("timesPerPeriod") }
                val window = goal?.getString("window").orEmpty().ifBlank { habit.getString("goalType") }
                val trackingMode = tracking?.getString("mode").orEmpty()
                val unit = tracking?.getString("unit").orEmpty()
                val trackingText = listOf(trackingMode, unit).filter(String::isNotBlank).joinToString("/")
                val trackingSuffix = trackingText.takeIf(String::isNotBlank)?.let { " ｜ 记录：$it" }.orEmpty()
                "• ${name.ifBlank { "(未命名)" }} ｜ 计划：${scheduleText.ifBlank { "-" }} ｜ 目标：${target.ifBlank { "-" }} / ${window.ifBlank { "-" }}$trackingSuffix"
            }
        }
        else -> emptyList()
    }
}

internal fun JsonObject.getString(field: String): String {
    return runCatching {
        if (has(field) && !get(field).isJsonNull) get(field).asString else ""
    }.getOrDefault("")
}

internal fun JsonObject.getObject(field: String): JsonObject? = runCatching {
    if (has(field) && get(field).isJsonObject) getAsJsonObject(field) else null
}.getOrNull()

@Composable
internal fun ToolResultBubble(summary: TaskSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = iconResource(R.drawable.msr_check_circle_wght400),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "已读取任务：${summary.count} 条（逾期 ${summary.overdue}，24h 内 ${summary.dueSoon24h}）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = 22.dp,
                        bottomEnd = 10.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
internal fun AiBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = 10.dp,
                        bottomEnd = 22.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThinkingBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoadingIndicator(
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ErrorBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = iconResource(R.drawable.msr_error_wght400),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
internal fun InitialGuideView(
    onApplyPrompt: (String) -> Unit,
    prompts: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "你好，我是 Lifi",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "你可以像聊天一样告诉我：任务、日程或目标拆解",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            prompts.forEach { prompt ->
                QuickPromptRow(
                    prompt = prompt,
                    onClick = { onApplyPrompt(prompt) }
                )
            }
        }
    }
}

@Composable
internal fun QuickPromptRow(
    prompt: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = iconResource(R.drawable.msr_rocket_launch_wght400),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = iconResource(R.drawable.msr_chevron_right_wght400),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun rememberScreenScaledWobbleDp(
    fractionOfMinSide: Float = 0.012f,
    minDp: Dp = 16.dp,
    maxDp: Dp = 48.dp
): Dp {
    val cfg = LocalConfiguration.current
    val base = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val raw = (base * fractionOfMinSide).dp
    return raw.coerceIn(minDp, maxDp)
}

@Composable
fun GlowScrim(
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    blur: Dp = 60.dp,
    opacity: Float = 1f,
    jitterEnabled: Boolean = true,
    jitterRadius: Dp = 6.dp,
    freqBlue: Float = 1.00f,
    freqPink: Float = 0.95f,
    freqAmber: Float = 1.10f,
    freqBreathe: Float = 0.35f
) {
    val a = opacity.coerceIn(0f, 1f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val jPx = with(density) { jitterRadius.toPx() }

    val timeSec by rememberTimeSeconds()

    fun s(freqHz: Float, phase: Float = 0f): Float {
        val angle = (2f * Math.PI.toFloat()) * (timeSec * freqHz) + phase
        return sin(angle)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .blur(blur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val refW = with(density) { 840.dp.toPx() }
                val widthNorm = (w / refW).coerceIn(0f, 1f)
                val separationBoost = lerp(0.0f, 0.8f, 1f - widthNorm)
                val radiusScale = lerp(0.82f, 1.00f, widthNorm)
                val alphaScale = lerp(0.85f, 1.00f, widthNorm)
                val jitterScale = lerp(0.70f, 1.00f, widthNorm)
                val j = jPx * jitterScale

                val blueCenter = Offset(
                    w * (0.25f - separationBoost) + if (jitterEnabled) j * 0.9f * s(freqBlue, 0.13f) else 0f,
                    h * 0.80f + if (jitterEnabled) j * 0.5f * s(freqBlue * 1.3f, 0.37f) else 0f
                )
                val pinkCenter = Offset(
                    w * (0.78f + separationBoost) + if (jitterEnabled) j * 0.7f * s(freqPink, 0.51f) else 0f,
                    h * 0.72f + if (jitterEnabled) j * 0.6f * s(freqPink * 1.4f, 0.11f) else 0f
                )
                val amberCenter = Offset(
                    w * 0.55f + if (jitterEnabled) j * 0.8f * s(freqAmber, 0.29f) else 0f,
                    h * 0.95f + if (jitterEnabled) j * 0.4f * s(freqAmber * 0.8f, 0.73f) else 0f
                )

                val blueRadius = h * 1.10f * radiusScale * (1f + if (jitterEnabled) 0.015f * s(freqBlue * 1.1f, 0.2f) else 0f)
                val pinkRadius = h * 1.00f * radiusScale * (1f + if (jitterEnabled) 0.018f * s(freqPink * 0.95f, 0.4f) else 0f)
                val amberRadius = h * 1.30f * radiusScale * (1f + if (jitterEnabled) 0.012f * s(freqAmber * 1.05f, 0.6f) else 0f)
                val breathe = 0.90f + 0.10f * (if (jitterEnabled) (s(freqBreathe, 0.18f) * 0.5f + 0.5f) else 1f)

                val blue = Brush.radialGradient(
                    colors = listOf(Color(0xFF6AA9FF).copy(alpha = 0.85f * alphaScale * a * breathe), Color.Transparent),
                    center = blueCenter,
                    radius = blueRadius
                )
                val pink = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF6AE6).copy(alpha = 0.80f * alphaScale * a * breathe), Color.Transparent),
                    center = pinkCenter,
                    radius = pinkRadius
                )
                val amber = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFC36A).copy(alpha = 0.80f * alphaScale * a * breathe), Color.Transparent),
                    center = amberCenter,
                    radius = amberRadius
                )

                val whiteFog = Brush.radialGradient(
                    colors = listOf(surfaceColor.copy(alpha = 0.55f * a), Color.Transparent),
                    center = Offset(w / 2f, h * 1.12f),
                    radius = h * 1.25f
                )
                val vertical = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.60f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.35f * a)
                )

                onDrawBehind {
                    drawRect(amber, blendMode = BlendMode.Plus)
                    drawRect(blue, blendMode = BlendMode.Plus)
                    drawRect(pink, blendMode = BlendMode.Plus)
                    drawRect(whiteFog)
                    drawRect(vertical)
                }
            }
    )
}

fun Modifier.glowingWobbleBorder(
    shape: Shape,
    colors: List<Color>,
    stroke: Dp,
    wobblePx: Dp = 4.dp,
    freqHz: Float = 0.20f,
    breatheAmp: Float = 0.12f,
    breatheHz: Float = 0.10f
): Modifier = composed {
    val density = LocalDensity.current
    val timeSec by rememberTimeSeconds()

    val wobble = with(density) { wobblePx.toPx() } * sin(2f * Math.PI.toFloat() * (timeSec * freqHz))
    val breathe = 1f + breatheAmp * sin(2f * Math.PI.toFloat() * (timeSec * breatheHz + 0.17f))

    this.then(
        Modifier.drawWithCache {
            val strokePx = stroke.toPx()

            val brush = Brush.linearGradient(
                colors = colors.map { it.copy(alpha = (it.alpha * breathe).coerceIn(0f, 1f)) },
                start = Offset(-wobble, 0f),
                end = Offset(size.width + wobble, 0f)
            )

            onDrawWithContent {
                drawContent()

                inset(strokePx / 2f) {
                    val outline = shape.createOutline(
                        size = size,
                        layoutDirection = this.layoutDirection,
                        density = this
                    )
                    when (outline) {
                        is Outline.Rounded -> {
                            val rr = outline.roundRect
                            val path = Path().apply { addRoundRect(rr) }
                            drawPath(path = path, brush = brush, style = Stroke(width = strokePx))
                        }
                        is Outline.Rectangle -> {
                            drawRect(brush = brush, style = Stroke(width = strokePx))
                        }
                        is Outline.Generic -> {
                            drawPath(path = outline.path, brush = brush, style = Stroke(width = strokePx))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun rememberTimeSeconds(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    time.floatValue += dt
                }
                last = now
            }
        }
    }
    return time
}
