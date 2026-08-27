package com.aritxonly.deadliner.ui.main.modern.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val SearchContainerSharedKey = "modern-list-search-container"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ModernListSearchTransition(
    searchActive: Boolean,
    useSharedContainerTransform: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    listContent: @Composable () -> Unit,
    searchContent: @Composable (searchContainerModifier: Modifier) -> Unit,
) {
    AnimatedContent(
        targetState = searchActive,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(tween(180, easing = FastOutSlowInEasing))
                .togetherWith(fadeOut(tween(120)))
                .using(SizeTransform(clip = false))
        },
        label = "modern-list-search-transition",
    ) { expanded ->
        if (expanded) {
            searchContent(
                modernListSearchSharedBoundsModifier(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent,
                    enabled = useSharedContainerTransform,
                ),
            )
        } else {
            listContent()
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ModernListSharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable (SharedTransitionScope) -> Unit,
) {
    SharedTransitionLayout(modifier = modifier) {
        content(this)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun modernListSearchSharedBoundsModifier(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    enabled: Boolean,
): Modifier = if (enabled) {
    with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(SearchContainerSharedKey),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = { _, _ ->
                tween(durationMillis = 300, easing = FastOutSlowInEasing)
            },
            // SearchBar has internal minimum-size constraints. Remeasuring it into the compact
            // source capsule during a system-back transition can produce negative child sizes.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
            ),
        )
    }
} else {
    Modifier
}

@Composable
fun ImmersiveListSearchEntry(
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        inputField = {
            Box(modifier = Modifier.fillMaxWidth()) {
                InputField(
                    query = "",
                    onQueryChange = {},
                    onSearch = { onOpenSearch() },
                    expanded = false,
                    onExpandedChange = { expanded ->
                        if (expanded) onOpenSearch()
                    },
                    label = "",
                )
                Text(
                    text = stringResource(R.string.search_hint),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 48.dp, end = 16.dp),
                    color = MiuixTheme.colorScheme.onSurfaceContainerHigh.copy(alpha = 0.56f),
                    style = MiuixTheme.textStyles.main.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }
        },
        expanded = false,
        onExpandedChange = { expanded ->
            if (expanded) onOpenSearch()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {}
}
