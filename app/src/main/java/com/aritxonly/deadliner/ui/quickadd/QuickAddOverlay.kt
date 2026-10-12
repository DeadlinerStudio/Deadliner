package com.aritxonly.deadliner.ui.quickadd

import androidx.activity.compose.BackHandler
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
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import kotlinx.coroutines.delay

/** A transient lens sharing the page backdrop. It never records itself into that backdrop. */
@Composable
fun QuickAddOverlay(state: QuickAddState, onSaved: () -> Unit, onExpand: (Intent) -> Unit) {
    val morph = quickAddMorphTransition(state.visible)
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.visible) { if (!state.visible && morph.active) keyboard?.hide() }
    if (!morph.active) return
    val context = LocalContext.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
        BackHandler { state.dismiss() }
        val density = LocalDensity.current
        val placement = with(density) {
            quickAddPlacement(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                origin, state.anchor, WindowInsets.ime.getBottom(this).toFloat(),
                12.dp.toPx(), 58.dp.toPx(), 64.dp.toPx())
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
            onExpand(QuickAddEditorEntry.intent(context, state)); state.dismiss()
        }

    }
}

@Composable
internal fun QuickAddContainer(state: QuickAddState, onSaved: () -> Unit, modifier: Modifier = Modifier,
                               onClose: () -> Unit = state::dismiss, onExpand: (() -> Unit)? = null, showClose: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val recipe = DeadlinerGlassRecipes.quickAdd(LocalAdvancedMaterialSpec.current.fineTuning)
    val shape = RoundedCornerShape(DeadlinerGlassRecipes.QuickAddCornerRadius)
    SoftGlassSurface(modifier.softGlassShadow(shape, colors.onSurface.luminance() > 0.5f)
        .pointerInput(Unit) { detectTapGestures { } }.semantics { paneTitle = "快速添加" }, shape = shape, recipe = recipe) {
        QuickAddContent(state, onSaved, onClose, onExpand, showClose)
    }
}

@Composable
internal fun QuickAddContent(state: QuickAddState, onSaved: () -> Unit, onClose: () -> Unit = state::dismiss,
                             onExpand: (() -> Unit)? = null, showClose: Boolean = true, autofocus: Boolean = state.visible) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(autofocus) {
        if (autofocus) { delay(80); if (state.visible) { focus.requestFocus(); keyboard?.show() } }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).animateContentSize()
        .padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("快速添加", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
            if (showClose) Icon(materialSymbolPainter("close"), "关闭快速添加", Modifier.size(44.dp)
                .clickable(enabled = !state.busy || state.recognizingAI, onClick = onClose).padding(10.dp), tint = colors.onSurfaceVariant)
        }
        BasicTextField(state.text, state::edit, Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 72.dp).focusRequester(focus),
            enabled = !state.busy || state.recognizingAI, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary), maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { state.submit(onSaved) }),
            decorationBox = { inner -> Box {
                if (state.text.isEmpty()) Text("明天下午3点交报告 / 每天读书", color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge)
                inner()
            } })
        QuickAddModelDownloadStatus(state)
        AnimatedVisibility(state.proposal.title.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(colors.surface.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val recognitionSource = when (state.proposal.source) {
                    "MODEL" -> "端侧模型"
                    "AI" -> "AI"
                    "CALENDAR" -> "日历"
                    else -> "规则"
                }
                Text(recognitionSource + " · " + if (state.needsAI) "待确认草案" else
                    (if (state.proposal.kind == "HABIT") "习惯草案" else "任务草案"),
                    style = MaterialTheme.typography.labelMedium, color = colors.primary)
                Text(state.proposal.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            }
        }
        Box(Modifier.padding(horizontal = 16.dp)) { QuickAddProperties(state, onSaved) }
        if (onExpand != null && !state.proposal.ready && !state.needsAI && state.text.isNotBlank()) {
            Text("展开完整表单", Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp)
                .clickable(enabled = !state.busy, onClick = onExpand).padding(vertical = 12.dp), color = colors.primary)
        }
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
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
        Icon(materialSymbolPainter(symbol), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
