package com.aritxonly.deadliner.ui.main.modern.components

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.resolveTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.navigation.ImmersiveTextTabRow
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FixedTitleRowHeight = 52.dp
private val CenterTabsMaximumWidth = 240.dp
internal val ModernListSearchSlotHeight = 45.dp

@Composable
fun ModernListTopAppBar(
    activity: MainActivity,
    selectedPage: DeadlineType,
    onSelectedPageChange: (DeadlineType) -> Unit,
    avatarPainter: Painter?,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    showPageTabs: Boolean = true,
    transparentBackground: Boolean = false,
    searchExpandedFraction: Float = 1f,
    searchContent: (@Composable () -> Unit)? = null,
) {
    val containerColor = if (transparentBackground) Color.Transparent else MiuixTheme.colorScheme.surface
    val materialTarget = resolveTopBarButtonMaterialProgress(
        collapsedFraction = scrollBehavior.state.collapsedFraction,
        scrollProgress = LocalTopBarScrollProgress.current,
        overrideProgress = null,
    )
    val materialProgress by animateFloatAsState(
        targetValue = materialTarget,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "modern-list-top-bar-material",
    )

    CompositionLocalProvider(LocalTopBarButtonMaterialProgress provides materialProgress) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = "",
            largeTitle = "Deadliner",
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            scrollBehavior = scrollBehavior,
            navigationIcon = {
                TopBarIconButton(onClick = {}) {
                    Icon(
                        painter = iconResource(R.drawable.msr_sort_wght400),
                        contentDescription = stringResource(R.string.filter_dialog_title),
                    )
                }
            },
            actions = {
                TopBarIconButton(
                    onClick = {
                        activity.startActivity(Intent(activity, SettingsActivity::class.java))
                    },
                ) {
                    if (avatarPainter != null) {
                        Image(
                            painter = avatarPainter,
                            contentDescription = stringResource(R.string.user),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            painter = iconResource(R.drawable.msr_settings_wght400),
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                }
            },
            bottomContent = {
                searchContent?.let { content ->
                    val expandedFraction = searchExpandedFraction.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ModernListSearchSlotHeight * expandedFraction)
                            .clipToBounds(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ModernListSearchSlotHeight)
                                .graphicsLayer {
                                    alpha = expandedFraction
                                    translationY = -ModernListSearchSlotHeight.toPx() *
                                        (1f - expandedFraction)
                                },
                        ) {
                            content()
                        }
                    }
                }
            },
        )

        if (showPageTabs) {
            val tabsWidth = minOf(maxWidth * 0.5f, CenterTabsMaximumWidth)
            ImmersiveTextTabRow(
                tabs = listOf(
                    stringResource(R.string.task),
                    stringResource(R.string.habit),
                ),
                selectedTabIndex = if (selectedPage == DeadlineType.TASK) 0 else 1,
                onTabSelected = { index ->
                    onSelectedPageChange(if (index == 0) DeadlineType.TASK else DeadlineType.HABIT)
                },
                materialProgress = materialProgress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .height(FixedTitleRowHeight)
                    .padding(vertical = 5.dp)
                    .width(tabsWidth),
            )
        }
        }
    }
}
