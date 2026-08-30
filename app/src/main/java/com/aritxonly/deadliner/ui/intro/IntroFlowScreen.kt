package com.aritxonly.deadliner.ui.intro

import android.animation.ValueAnimator
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.intro.IntroGuideSceneConfig
import com.aritxonly.deadliner.intro.IntroGuideScenes
import com.aritxonly.deadliner.intro.IntroPageType
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.model.UiStyle
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import nl.dionsegijn.konfetti.xml.KonfettiView

@Composable
fun IntroFlowScreen(
    onFinish: () -> Unit,
) {
    val style by GlobalUtils.styleFlow.collectAsState()
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val pages = remember(style) { IntroPageType.createPages(style) }
    var savedIntroPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedIntroPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
        pageCount = { pages.size }
    )
    val scope = rememberCoroutineScope()
    var celebrationTick by remember { mutableIntStateOf(0) }
    var pendingDesignMode by remember { mutableStateOf<AppearanceDesignMode?>(null) }
    var designModeTransitionVisible by remember { mutableStateOf(false) }

    val currentPage = pagerState.currentPage.coerceIn(0, pages.lastIndex)
    val currentPageType = pages[currentPage]
    val progressAlpha by animateFloatAsState(
        targetValue = if (currentPageType == IntroPageType.Final) 0f else 1f,
        label = "intro-progress-alpha"
    )
    val nextButtonVisible = currentPageType != IntroPageType.Final
    val nextButtonEnabled = !(currentPageType == IntroPageType.UiStyleChoice && style == UiStyle.Classic)

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage > pages.lastIndex) {
            pagerState.scrollToPage(pages.lastIndex)
            savedIntroPage = pages.lastIndex
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                savedIntroPage = page.coerceIn(0, pages.lastIndex)
            }
    }

    LaunchedEffect(celebrationTick) {
        if (celebrationTick > 0) {
            delay(1000)
            onFinish()
        }
    }

    LaunchedEffect(pendingDesignMode) {
        val targetMode = pendingDesignMode ?: return@LaunchedEffect
        designModeTransitionVisible = true
        delay(120)
        GlobalUtils.designMode = targetMode
        delay(260)
        designModeTransitionVisible = false
        delay(220)
        pendingDesignMode = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentPageType != IntroPageType.Final && celebrationTick == 0,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (val pageType = pages[page]) {
                IntroPageType.Cover -> IntroCoverPage()
                IntroPageType.Permissions -> PermissionsScreen()
                IntroPageType.Design -> DesignIntroScreen(
                    onRequestDesignModeChange = { mode ->
                        if (mode != appearance.designMode && pendingDesignMode == null) {
                            pendingDesignMode = mode
                        }
                    }
                )
                IntroPageType.Color -> ColorIntroScreen()
                IntroPageType.AdvancedMaterial -> AdvancedMaterialIntroScreen()
                IntroPageType.UiStyleChoice -> UiModeScreen()
                is IntroPageType.GuideScene -> {
                    IntroGuideScenePage(
                        scene = requireNotNull(IntroGuideScenes.findById(pageType.sceneId))
                    )
                }
                IntroPageType.Final -> {
                    IntroFinalPage(
                        finishing = celebrationTick > 0,
                        onFinish = {
                            if (celebrationTick == 0) {
                                celebrationTick += 1
                            }
                        }
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { progressForPage(currentPage, pages.lastIndex) },
            modifier = Modifier
                .graphicsLayer(alpha = progressAlpha)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 64.dp, end = 64.dp)
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        )

        AnimatedVisibility(
            visible = nextButtonVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val nextPage = (pagerState.currentPage + 1).coerceAtMost(pages.lastIndex)
                        pagerState.animateScrollToPage(nextPage)
                    }
                },
                enabled = nextButtonEnabled && celebrationTick == 0,
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Text(text = stringResource(R.string.go_next))
            }
        }

        IntroKonfettiOverlay(
            trigger = celebrationTick,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = designModeTransitionVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                )
            }
        }
    }
}

private fun progressForPage(currentPage: Int, lastIndex: Int): Float {
    if (lastIndex <= 0) return 1f
    return (currentPage.toFloat() / lastIndex.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun IntroCoverPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IntroDynamicLottie(
                    rawRes = R.raw.intro_welcome,
                    dynamicTint = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = stringResource(R.string.intro_cover_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp)
        )

        Text(
            text = stringResource(R.string.intro_cover_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun IntroGuideScenePage(
    scene: IntroGuideSceneConfig,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                IntroDynamicLottie(
                    rawRes = IntroGuideScenes.resolveRawRes(scene.fileName),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = scene.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )

        Text(
            text = scene.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Text(
            text = scene.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun IntroFinalPage(
    finishing: Boolean,
    onFinish: () -> Unit,
) {
    val welcome = stringResource(R.string.welcome)
    var displayedText by remember(welcome) { mutableStateOf("") }
    var revealSupport by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(welcome) {
        displayedText = ""
        revealSupport = false
        showButton = false
        delay(120)
        revealSupport = true
        welcome.forEachIndexed { index, _ ->
            displayedText = welcome.take(index + 1)
            delay(42)
        }
        showButton = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = displayedText,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        AnimatedVisibility(
            visible = revealSupport,
            enter = fadeIn() + slideInVertically { it / 3 }
        ) {
            Text(
                text = stringResource(R.string.welcome_version),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            Button(
                onClick = onFinish,
                enabled = !finishing,
                modifier = Modifier
                    .padding(top = 36.dp)
                    .size(width = 180.dp, height = 56.dp)
            ) {
                Text(text = stringResource(R.string.intro_get_started))
            }
        }
    }
}

@Composable
private fun IntroDynamicLottie(
    @RawRes rawRes: Int,
    modifier: Modifier = Modifier,
    dynamicTint: Boolean = false,
) {
    AndroidView(
        factory = { context ->
            LottieAnimationView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                repeatCount = ValueAnimator.INFINITE
                setAnimation(rawRes)
                if (dynamicTint) {
                    applyDynamicIntroColors()
                }
                playAnimation()
                tag = rawRes
            }
        },
        update = { view ->
            if (view.tag != rawRes) {
                view.setAnimation(rawRes)
                if (dynamicTint) {
                    view.applyDynamicIntroColors()
                }
                view.tag = rawRes
            }
            if (!view.isAnimating) {
                view.playAnimation()
            }
        },
        modifier = modifier
    )
}

private fun LottieAnimationView.applyDynamicIntroColors() {
    val primary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer)
    val secondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryVariant)
    val outline = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer)

    addValueCallback(
        KeyPath("Ð¼Ð°ÑÐºÐ° 1", "**", "Stroke 1"),
        LottieProperty.STROKE_COLOR,
        LottieValueCallback(primary)
    )
    addValueCallback(
        KeyPath("Ð¼Ð°ÑÐºÐ° 2", "**", "Stroke 1"),
        LottieProperty.STROKE_COLOR,
        LottieValueCallback(primary)
    )
    addValueCallback(
        KeyPath("Ð¼Ð°ÑÐºÐ° 3", "**", "Stroke 1"),
        LottieProperty.STROKE_COLOR,
        LottieValueCallback(primary)
    )
    addValueCallback(
        KeyPath("Ð¿ÑÑÐ¶Ð¸Ð½ÐºÐ°", "**", "Stroke 1"),
        LottieProperty.STROKE_COLOR,
        LottieValueCallback(outline)
    )
    addValueCallback(
        KeyPath("ÑÐ°ÑÐ¸Ðº 2", "**", "Fill 1"),
        LottieProperty.COLOR,
        LottieValueCallback(secondary)
    )
    addValueCallback(
        KeyPath("ÑÐ°ÑÐ¸Ðº 2", "**"),
        LottieProperty.OPACITY,
        LottieValueCallback(85)
    )
    invalidate()
}

@Composable
private fun IntroKonfettiOverlay(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            KonfettiView(context).apply {
                tag = 0
            }
        },
        update = { view ->
            val playedFor = view.tag as? Int ?: 0
            if (trigger > 0 && trigger != playedFor) {
                view.start(PartyPresets.explode())
                view.tag = trigger
            }
        },
        modifier = modifier
    )
}
