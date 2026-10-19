package com.aritxonly.deadliner.ui.base

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aritxonly.deadliner.model.MiuixModalKind
import com.aritxonly.deadliner.model.miuixModalMaterial
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.deadlinerGlassMaterial
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.RemovePlatformDialogDefaultEffects

@Composable
internal fun SoftGlassDropdownPopup(
    show: Boolean,
    title: String,
    anchor: Rect,
    items: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSelect: (Int) -> Unit,
    progress: Animatable<Float, androidx.compose.animation.core.AnimationVector1D> = remember { Animatable(0f) },
    itemLeading: (@Composable (Int) -> Unit)? = null,
    itemTextColor: ((Int) -> Color?)? = null,
    itemRole: Role = Role.RadioButton,
) {
    var mounted by remember { mutableStateOf(false) }
    var positioned by remember { mutableStateOf(false) }
    val currentFinished by rememberUpdatedState(onDismissFinished)
    val opening = show && items.isNotEmpty()
    LaunchedEffect(opening, positioned) {
        if (!opening && !mounted) return@LaunchedEffect
        if (opening) {
            mounted = true
            if (!positioned) return@LaunchedEffect
        }
        progress.animateTo(if (opening) 1f else 0f, tween(
            if (opening) GlassDropdownMotion.EnterDuration else GlassDropdownMotion.ExitDuration,
            easing = GlassDropdownMotion.Easing,
        ))
        if (!opening) {
            mounted = false
            positioned = false
            currentFinished()
        }
    }
    if (!mounted && !opening) return
    val advanced = LocalAdvancedMaterialSpec.current
    val backdrop = LocalWindowGlassBackdrop.current
    DisposableEffect(backdrop, advanced.textureBlurEnabled) {
        val active = backdrop?.takeIf { advanced.textureBlurEnabled }
        active?.let { it.consumers++ }
        onDispose { active?.let { it.consumers-- } }
    }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // OEM Dialog dismissal can otherwise animate the already-collapsed window again.
        RemovePlatformDialogDefaultEffects()
        var origin by remember { mutableStateOf<Offset?>(null) }
        var menuSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        val insets = WindowInsets.safeDrawing
        val settled by remember(progress) { derivedStateOf { progress.value == 1f } }
        // Keep the optical clip full-size; the outer surface alone owns the morph.
        val materialShape = remember { RoundedCornerShape(GlassDropdownMotion.CornerRadius) }
        val container = MiuixTheme.colorScheme.surfaceContainer
        val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f
        val modal = advanced.fineTuning.miuixModalMaterial(MiuixModalKind.Dialog)
        val material = if (advanced.textureBlurEnabled && backdrop != null) {
            Modifier.deadlinerGlassMaterial(
                advancedMaterial = advanced, backdrop = backdrop, shape = materialShape,
                tint = container, isDark = dark,
                recipe = DeadlinerGlassRecipes.dropdown(advanced.fineTuning),
                blurRadiusOverride = modal.blurRadiusDp,
                tintAlphaOverride = maxOf(modal.backgroundAlpha, if (dark) 0.84f else 0.78f),
            )
        } else Modifier.background(container, materialShape)
        BoxWithConstraints(Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionOnScreen() }
            .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }) {
            val margin = with(density) { 8.dp.toPx() }
            val safe = Rect(
                insets.getLeft(density, direction) + margin,
                insets.getTop(density) + margin,
                (constraints.maxWidth - insets.getRight(density, direction) - margin)
                    .coerceAtLeast(insets.getLeft(density, direction) + margin),
                (constraints.maxHeight - insets.getBottom(density) - margin)
                    .coerceAtLeast(insets.getTop(density) + margin),
            )
            val widthLimit = minOf(with(density) { 288.dp.roundToPx() }, safe.width.toInt().coerceAtLeast(0))
            val minimumWidth = minOf(widthLimit, maxOf(with(density) { 192.dp.roundToPx() }, anchor.width.toInt()))
            // Resolve placement and the source offset together in composition. Reading a new
            // onGloballyPositioned origin only in a graphicsLayer exposed the old (0,0) offset.
            val layoutReady = origin != null && menuSize.width > 0 && menuSize.height > 0
            val position = origin?.let {
                GlassDropdownGeometry.position(anchor.translate(-it), safe, menuSize,
                    direction == LayoutDirection.Rtl, margin)
            } ?: IntOffset.Zero
            val source = origin?.let {
                anchor.translate(-(it + Offset(position.x.toFloat(), position.y.toFloat())))
            } ?: Rect.Zero
            SideEffect { if (opening && layoutReady) positioned = true }
            Box(Modifier.absoluteOffset { position }
                .widthIn(min = with(density) { minimumWidth.toDp() }, max = with(density) { widthLimit.toDp() })
                .heightIn(max = with(density) { safe.height.toDp() }), propagateMinConstraints = true) {
                Box(Modifier
                    .onSizeChanged { menuSize = it }
                    .graphicsLayer {
                        alpha = if (layoutReady) GlassDropdownMotion.surfaceAlpha(progress.value) else 0f
                    }
                    .dropdownMorphDrawing(source, progress, dark).then(material).semantics { paneTitle = title }, propagateMinConstraints = true) {
                    Box(Modifier.graphicsLayer {
                        val bounds = GlassDropdownMotion.bounds(source, size, progress.value)
                        val scale = if (size.width > 0f) bounds.width / size.width else 1f
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationX = bounds.left
                        translationY = bounds.top
                        alpha = GlassDropdownMotion.contentAlpha(progress.value)
                    }, propagateMinConstraints = true) {
                        SoftGlassDropdownOptions(items, selectedIndex, onSelect,
                            enabled = show && settled,
                            itemLeading = itemLeading, itemTextColor = itemTextColor, itemRole = itemRole)
                    }
                }
            }
        }
    }
}
