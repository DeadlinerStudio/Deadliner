package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.geometry.Rect
import com.aritxonly.deadliner.ui.base.sheet.LocalSheetExpansionProgress
import com.aritxonly.deadliner.ui.base.sheet.SheetExpansionMotion
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.MiuixModalKind
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.material.glass.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.base.sheet.ScaffoldSheetOverlay
import com.aritxonly.deadliner.ui.base.sheet.LocalSheetHeaderDragModifier
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.LocalContentColor as MaterialContentColor
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixContentColor

/**
 * Page-scoped glass sheet with a fixed-height Scaffold and fading content beneath its header.
 * Pass [content]'s padding to LazyColumn.contentPadding, or inside a verticalScroll modifier,
 * so scrolling content can travel beneath the header. The overlay owns IME/drag handling;
 * the inner Scaffold owns navigation-bar padding and reserves no status-bar space.
 */
@Composable
fun MiuixBottomSheet(
    show: Boolean,
    title: String? = null,
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    allowDismiss: Boolean = true,
    sheetHeight: SheetHeight = SheetHeight.MEDIUM,
    enterFrom: Rect? = null,
    bottomBar: @Composable () -> Unit = {},
    headerContent: @Composable () -> Unit = {},
    sampleContentForBottomBar: Boolean = false,
    contentFadeHeight: Dp = 24.dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val contentColor = MiuixTheme.colorScheme.onSurface
    val pageBackdrop = LocalAdvancedMaterialBackdrop.current
        ?: LocalAdvancedMaterialDialogBlurHostState.current?.modalBackdrop
    val systemTop = maxOf(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding(),
        WindowInsets.captionBar.asPaddingValues().calculateTopPadding())
    val bodyHeight = sheetHeight.bodyHeight(LocalWindowInfo.current.containerDpSize.height, systemTop)
    val shape = RoundedCornerShape(36.dp)
    val surface = rememberMiuixModalSurface(
        kind = MiuixModalKind.BottomSheet,
        shape = shape,
        recipe = DeadlinerGlassRecipes.bottomSheet(advancedMaterial.fineTuning),
    )

    ScaffoldSheetOverlay(
        show = show,
        modifier = modifier.then(surface.modifier),
        backgroundColor = surface.backgroundColor,
        topInset = sheetHeight.topInset(systemTop),
        enterFrom = enterFrom,
        allowDismiss = allowDismiss,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) {
        val bottomBackdrop = if (sampleContentForBottomBar && advancedMaterial.enabled) {
            rememberLayerBackdrop { drawContent() }
        } else null
        CompositionLocalProvider(
            LocalTopBarScrollProgress provides null,
            LocalTopBarMaterialScrollObserver provides null,
            LocalTopBarButtonMaterialProgress provides 0f,
            LocalAdvancedMaterialBackdrop provides null,
        ) {
            val expansion = LocalSheetExpansionProgress.current
            Scaffold(
                modifier = Modifier.fillMaxWidth().height(bodyHeight).graphicsLayer {
                    alpha = SheetExpansionMotion.contentAlpha(expansion.value)
                    translationY = 12.dp.toPx() * (1f - expansion.value.coerceIn(0f, 1f))
                },
                containerColor = Color.Transparent,
                contentColor = contentColor,
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides (bottomBackdrop ?: pageBackdrop)) {
                        bottomBar()
                    }
                },
                topBar = {
                    // Sheet actions use ordinary buttons, independent of page glass hosting.
                    CompositionLocalProvider(
                        LocalProgressiveTopBarMaterialHosted provides false,
                        // There is no outer glass/press host here; each action owns its feedback.
                        LocalTopBarButtonMaterialHosted provides false,
                    ) {
                        // Supply both locals inside the popup/Scaffold Surface, which can replace
                        // inherited content colors when its container is transparent.
                        CompositionLocalProvider(
                            MaterialContentColor provides contentColor,
                            MiuixContentColor provides contentColor,
                        ) {
                            Column(Modifier.fillMaxWidth().then(LocalSheetHeaderDragModifier.current)) {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .height(56.dp)
                                        .pointerInput(Unit) {
                                            // Masked rows beneath the title must not receive header taps.
                                            detectTapGestures { }
                                        }
                                        .padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = title.orEmpty(),
                                        modifier = Modifier.align(Alignment.Center).padding(
                                            horizontal = if (startAction != null || endAction != null) 56.dp else 12.dp,
                                        ),
                                        style = MiuixTheme.textStyles.title4,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Box(Modifier.align(Alignment.CenterStart)) { startAction?.invoke() }
                                    Box(Modifier.align(Alignment.CenterEnd)) { endAction?.invoke() }
                                }
                                headerContent()
                            }
                        }
                    }
                },
            ) { padding ->
                val layoutDirection = LocalLayoutDirection.current
                Box(
                    modifier = Modifier.fillMaxSize()
                        .then(if (bottomBackdrop != null) Modifier.layerBackdrop(bottomBackdrop) else Modifier)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            // Mask the content itself, including card backgrounds, rather than
                            // drawing another sampled/tinted surface over the sheet.
                            val start = padding.calculateTopPadding().toPx()
                            val mask = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.White),
                                startY = start,
                                endY = start + contentFadeHeight.toPx(),
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(mask, blendMode = BlendMode.DstIn)
                            }
                        },
                ) {
                    CompositionLocalProvider(
                        MaterialContentColor provides contentColor,
                        MiuixContentColor provides contentColor,
                        LocalSheetFormContainers provides true,
                        LocalAdvancedMaterialBackdrop provides null,
                    ) {
                        // Keep the first item outside the fade at rest; only scrolling content
                        // enters it. Preserve the Scaffold's directional/system-bar padding.
                        content(PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding() + contentFadeHeight,
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = padding.calculateBottomPadding(),
                        ))
                    }
                }
            }
        }
    }
}
