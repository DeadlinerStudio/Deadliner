package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import com.aritxonly.deadliner.ui.material.glass.LocalImmersiveExperienceEnabled
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.material.glass.ProgressiveTopBarMaterial
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val AdaptiveMaterialTopBarShape = RoundedCornerShape(0.dp)

@Composable
fun AdaptiveMaterialScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    legacyTopBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    advancedMaterialTopBarTintColor: Color? = null,
    wrapTopBarInMaterialContainer: Boolean = true,
    topBarMaterialAlpha: Float = 1f,
    topBarMaterialScrollKey: Any? = Unit,
    useCurrentTopBarHeightForContentPadding: Boolean = false,
    applyScaffoldPaddingOutsideBackdrop: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val immersiveExperienceEnabled = LocalImmersiveExperienceEnabled.current
    val topBarScrollObserver = rememberTopBarMaterialScrollObserver(
        scrollKey = topBarMaterialScrollKey,
    )
    val topBarScrollProgress = if (immersiveExperienceEnabled) {
        topBarScrollObserver.progress
    } else {
        null
    }
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val backdropCaptureColor = resolveBackdropCaptureColor(
        containerColor = containerColor,
        miuixSurfaceColor = MiuixTheme.colorScheme.surface,
    )
    val backdrop = if (advancedMaterial.enabled) {
        rememberLayerBackdrop {
            drawRect(backdropCaptureColor)
            drawContent()
        }
    } else {
        null
    }
    var currentTopBarHeightPx by remember { mutableIntStateOf(0) }
    var maxTopBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeight = with(density) {
        if (useCurrentTopBarHeightForContentPadding) {
            currentTopBarHeightPx.toDp()
        } else {
            maxTopBarHeightPx.toDp()
        }
    }
    val tintBaseColor = advancedMaterialTopBarTintColor ?: MiuixTheme.colorScheme.surface
    val surfaceTint = tintBaseColor.copy(alpha = advancedMaterial.topBarTintAlpha)
    val blurColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(surfaceTint)))
    val hostedTopBar: @Composable () -> Unit = {
        ProgressiveTopBarMaterial(
            enabled = immersiveExperienceEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            topBar()
        }
    }
    val hostedFloatingActionButton: @Composable () -> Unit = {
        if (backdrop != null) {
            CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                floatingActionButton()
            }
        } else {
            floatingActionButton()
        }
    }

    CompositionLocalProvider(
        LocalTopBarScrollProgress provides topBarScrollProgress,
        LocalTopBarMaterialScrollObserver provides topBarScrollObserver,
    ) {
        ProvideAdvancedMaterialDialogBlurHost {
            RegisterAdvancedMaterialModalBackdrop(backdrop)
            Scaffold(
        modifier = modifier
            .let { scaffoldModifier ->
                if (immersiveExperienceEnabled) {
                    scaffoldModifier.nestedScroll(topBarScrollObserver.nestedScrollConnection)
                } else {
                    scaffoldModifier
                }
            },
        topBar = {
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    legacyTopBar()
                }
            } else {
                legacyTopBar()
            }
        },
        bottomBar = {
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    bottomBar()
                }
            } else {
                bottomBar()
            }
        },
        snackbarHost = snackbarHost,
        floatingActionButton = hostedFloatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = Color.Transparent,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        val mergedPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding() + topBarHeight,
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding(),
        )
        val contentPadding = if (applyScaffoldPaddingOutsideBackdrop) {
            PaddingValues(top = topBarHeight)
        } else {
            mergedPadding
        }
        val hostModifier = if (applyScaffoldPaddingOutsideBackdrop) {
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = hostModifier,
        ) {
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    // The recorder must contain page content only. Backdrop consumers such as
                    // top/bottom bars are siblings below; recording an ancestor that contains
                    // those consumers creates a cyclic RenderNode tree on Android HWUI.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .background(containerColor),
                    ) {
                        content(contentPadding)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(containerColor),
                ) {
                    content(contentPadding)
                }
            }

            val topBarModifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    currentTopBarHeightPx = size.height
                    if (size.height > maxTopBarHeightPx) {
                        maxTopBarHeightPx = size.height
                    }
                }

            if (
                backdrop != null &&
                wrapTopBarInMaterialContainer &&
                !immersiveExperienceEnabled
            ) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    Box(
                        modifier = topBarModifier.then(
                            Modifier
                                .advancedTextureBlur(
                                    advancedMaterial = advancedMaterial,
                                    backdrop = backdrop,
                                    shape = AdaptiveMaterialTopBarShape,
                                    colors = blurColors,
                                )
                                .let { blurModifier ->
                                    val resolvedAlpha = topBarMaterialAlpha.coerceIn(0f, 1f)
                                    if (resolvedAlpha < 0.999f) {
                                        blurModifier.alpha(resolvedAlpha)
                                    } else {
                                        blurModifier
                                    }
                                },
                        )
                    ) {
                        hostedTopBar()
                    }
                }
            } else if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    Box(modifier = topBarModifier) {
                        hostedTopBar()
                    }
                }
            } else {
                Box(modifier = topBarModifier) {
                    hostedTopBar()
                }
            }
        }
        }
    }
}
}

internal fun resolveBackdropCaptureColor(
    containerColor: Color,
    miuixSurfaceColor: Color,
): Color = if (containerColor.alpha >= 0.999f) {
    containerColor
} else {
    miuixSurfaceColor
}
