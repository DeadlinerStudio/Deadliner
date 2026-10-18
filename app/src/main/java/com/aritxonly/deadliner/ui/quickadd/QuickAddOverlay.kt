package com.aritxonly.deadliner.ui.quickadd

import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.boundsInWindow
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import kotlinx.coroutines.delay

/** A transient lens sharing the page backdrop. It never records itself into that backdrop. */
@Composable
fun QuickAddOverlay(state: QuickAddState, onSaved: () -> Unit, onExpand: (Intent) -> Unit,
                    onDismissFinished: (() -> Unit)? = null) {
    val morph = quickAddMorphTransition(state.visible)
    var hasShown by remember { mutableStateOf(false) }
    val finished by rememberUpdatedState(onDismissFinished)
    LaunchedEffect(state.visible, morph.active) {
        if (state.visible) hasShown = true
        else if (hasShown && !morph.active && !state.expandingToEditor) finished?.invoke()
    }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.visible) { if (!state.visible && morph.active) keyboard?.hide() }
    if (!morph.active || state.expandingToEditor) return
    val context = LocalContext.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
        BackHandler { state.dismiss() }
        val density = LocalDensity.current
        val placement = with(density) {
            quickAddPlacement(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                origin, state.anchor, WindowInsets.ime.getBottom(this).toFloat(),
                QuickAddPanelGap.toPx(), 58.dp.toPx(), QuickAddMinimumControlsHeight.toPx())
        }
        // Only the page area intercepts dismissal; the real dock and + action remain interactive.
        Box(Modifier.fillMaxWidth().height(with(density) { placement.dismissAreaHeight.toDp() })
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = state::dismiss))
        val source = if (state.anchor.width > 0f) state.anchor.translate(-origin) else {
            val diameter = with(density) { 60.dp.toPx() }
            val right = constraints.maxWidth - placement.rightInset
            val bottom = constraints.maxHeight - placement.actionBottomInset
            Rect(right - diameter, bottom - diameter, right, bottom)
        }
        QuickAddMorphHost(state, morph.progress, source, placement, onSaved) {
            val request = QuickAddEditorEntry.intent(context, state)
            state.expandToEditor()
            onExpand(request)
        }

    }
}

@Composable
internal fun QuickAddContainer(state: QuickAddState, onSaved: () -> Unit, modifier: Modifier = Modifier,
                               onClose: () -> Unit = state::dismiss, onExpand: (() -> Unit)? = null, showClose: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val recipe = DeadlinerGlassRecipes.quickAdd(LocalAdvancedMaterialSpec.current.fineTuning)
    val shape = RoundedCornerShape(DeadlinerGlassRecipes.QuickAddCornerRadius)
    SoftGlassSurface(modifier.onGloballyPositioned {
        state.panelBounds = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
    }.softGlassShadow(shape, colors.onSurface.luminance() > 0.5f)
        .pointerInput(Unit) { detectTapGestures { } }.semantics { paneTitle = "快速添加" }, shape = shape, recipe = recipe) {
        QuickAddContent(state, onSaved, onClose, onExpand, showClose)
    }
}

@Composable
internal fun QuickAddContent(state: QuickAddState, onSaved: () -> Unit, onClose: () -> Unit = state::dismiss,
                             onExpand: (() -> Unit)? = null, showClose: Boolean = true,
                             modifier: Modifier = Modifier, autofocus: Boolean = state.visible) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(autofocus) {
        if (autofocus) { delay(80); if (state.visible) { focus.requestFocus(); keyboard?.show() } }
    }
    val scroll = rememberScrollState()
    val confirm = { state.submit(onSaved) }
    Column(modifier.fillMaxWidth().verticalScroll(scroll).animateContentSize()
        .padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("快速添加", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showClose) Icon(materialSymbolPainter("close"), "关闭快速添加", Modifier.size(44.dp).miuixPressFeedback(enabled = state.canClose, overlay = false).clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(enabled = state.canClose, onClick = onClose).padding(10.dp), tint = colors.onSurfaceVariant)
                if (onExpand != null) Icon(com.aritxonly.deadliner.ui.quickAddExpandPainter(), "展开完整编辑器", Modifier.size(44.dp).miuixPressFeedback(enabled = state.canClose, overlay = false).clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(enabled = state.canClose, onClick = onExpand).padding(14.8.dp), tint = colors.onSurfaceVariant)
            }
        }
        BasicTextField(state.text, state::edit, Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 72.dp).focusRequester(focus),
            enabled = !state.busy || state.recognizingAI, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary), maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { confirm() }),
            decorationBox = { inner -> Box {
                if (state.text.isEmpty()) Text("明天下午3点交报告 / 每天读书", color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge)
                inner()
            } })
        QuickAddModelDownloadStatus(state)
        val draft = state.effectiveProposal
        AnimatedVisibility(draft.title.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(colors.surface.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val recognitionSource = when (draft.source) {
                    "MANUAL" -> "手动"
                    "MODEL" -> "端侧模型"
                    "AI" -> "AI"
                    "CALENDAR" -> "日历"
                    else -> "规则"
                }
                Text(recognitionSource + " · " + if (state.needsAI) "待确认草案" else
                    (if (draft.kind == "HABIT") "习惯草案" else "任务草案"),
                    style = MaterialTheme.typography.labelMedium, color = colors.primary)
                Text(draft.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            }
        }
        Box(Modifier.padding(horizontal = 16.dp)) { QuickAddProperties(state, onSaved) }
        val status = when {
            state.busy -> if (state.recognizingAI) "正在生成草案…" else "正在添加…"
            else -> state.message
        }
        if (status.isNotBlank()) Text(status,
            Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)

    }
}

@Composable
internal fun QuickAddPropertyButton(symbol: String, label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Row(modifier.heightIn(min = 48.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
        Icon(materialSymbolPainter(symbol), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
