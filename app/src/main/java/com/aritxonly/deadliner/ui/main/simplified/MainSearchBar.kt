package com.aritxonly.deadliner.ui.main.simplified

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.DeadlineDetailActivity
// 别名导入，防止 M3 和 MIUIX 冲突
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.SearchBar as M3SearchBar
import top.yukonga.miuix.kmp.basic.SearchBar as MiuixSearchBar
import top.yukonga.miuix.kmp.basic.InputField as MiuixInputField
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    onQueryChanged: (String) -> Unit,
    searchResults: List<DDLItem>,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onMoreAnchorChange: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    useAvatar: Boolean = false,
    avatarPainter: Painter? = null,
    activity: MainActivity? = null,
    expanded: Boolean,
    onExpandedChangeExternal: (Boolean) -> Unit = {},
    selectedPage: DeadlineType,
    miuixMode: Boolean = false
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val isEnabled = GlobalUtils.motivationalQuotes
    val excitementArray = stringArrayResource(id = R.array.excitement_array).toList()
    var idx by rememberSaveable {
        mutableIntStateOf(if (excitementArray.isNotEmpty()) (0 until excitementArray.size).random() else 0)
    }

    LaunchedEffect(expanded) { onExpandedChangeExternal(expanded) }
    LaunchedEffect(isEnabled, excitementArray) {
        if (!isEnabled || excitementArray.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(30_000)
            idx = (idx + 1) % excitementArray.size
        }
    }

    val handleClose = {
        textFieldState.edit { replace(0, length, "") }
        onExpandedChangeExternal(false)
        focusManager.clearFocus()
    }

    if (!miuixMode) {
        // ==========================================
        // M3 模式：维持原样，头像和更多按钮在搜索框内部
        // ==========================================
        val searchBarPadding by animateDpAsState(
            targetValue = if (expanded) 0.dp else 16.dp,
            label = "Search bar padding"
        )

        Box(modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
            M3SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = searchBarPadding)
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        query = textFieldState.text.toString(),
                        onQueryChange = {
                            onQueryChanged(it)
                            textFieldState.edit { replace(0, length, it) }
                        },
                        onSearch = { handleClose() },
                        expanded = expanded,
                        onExpandedChange = onExpandedChangeExternal,
                        placeholder = {
                            AnimatedHintPlaceholder(
                                expanded = expanded, isEnabled = isEnabled, excitement = excitementArray, idx = idx
                            )
                        },
                        leadingIcon = {
                            if (expanded) {
                                M3IconButton(onClick = handleClose) {
                                    M3Icon(ImageVector.vectorResource(R.drawable.ic_back), stringResource(R.string.back))
                                }
                            } else {
                                M3Icon(ImageVector.vectorResource(R.drawable.ic_search), stringResource(R.string.search_events))
                            }
                        },
                        trailingIcon = {
                            if (!expanded) {
                                val iconModifier = Modifier.clip(CircleShape).onGloballyPositioned { onMoreAnchorChange(it.boundsInWindow()) }
                                if (useAvatar && avatarPainter != null) {
                                    M3IconButton(onClick = onMoreClick, modifier = iconModifier.size(32.dp)) {
                                        Image(painter = avatarPainter, contentDescription = stringResource(R.string.user), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                } else {
                                    M3IconButton(onClick = onMoreClick, modifier = iconModifier) {
                                        M3Icon(ImageVector.vectorResource(R.drawable.ic_more), stringResource(R.string.settings_more))
                                    }
                                }
                            } else {
                                if (textFieldState.text.isNotEmpty()) {
                                    M3IconButton(onClick = { textFieldState.edit { replace(0, length, "") } }) {
                                        M3Icon(ImageVector.vectorResource(R.drawable.ic_close), stringResource(R.string.close))
                                    }
                                }
                            }
                        }
                    )
                },
                expanded = expanded,
                onExpandedChange = { exp ->
                    if (!exp) textFieldState.edit { replace(0, length, "") }
                    onExpandedChangeExternal(exp)
                }
            ) {
                SearchResultContent(searchResults, selectedPage, context, activity)
            }
        }
    } else {
        val defaultHint = stringResource(R.string.search_hint)

        // 目标 hint
        val targetHint = when {
            expanded -> defaultHint
            isEnabled && excitementArray.isNotEmpty() -> excitementArray[idx % excitementArray.size]
            else -> defaultHint
        }

        MiuixSearchBar(
            modifier = modifier.fillMaxWidth().padding(8.dp, 8.dp, 8.dp, 0.dp),
            inputField = {
                MiuixInputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        onQueryChanged(it)
                        textFieldState.edit { replace(0, length, it) }
                    },
                    onSearch = { handleClose() },
                    expanded = expanded,
                    onExpandedChange = onExpandedChangeExternal,
                    label = targetHint
                )
            },
            expanded = expanded,
            onExpandedChange = { exp ->
                if (!exp) textFieldState.edit { replace(0, length, "") }
                onExpandedChangeExternal(exp)
            }
        ) {
            SearchResultContent(searchResults, selectedPage, context, activity)
        }
    }
}

@Composable
private fun SearchResultContent(
    searchResults: List<DDLItem>,
    selectedPage: DeadlineType,
    context: Context,
    activity: MainActivity?
) {
    if (searchResults.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.search_no_result_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.search_no_result_suggestion_prefix))
                    append("\n")
                    appendExample("y2025", R.string.search_example_y)
                    append("\n")
                    appendExample("m10", R.string.search_example_m)
                    append("\n")
                    appendExample("d15", R.string.search_example_d)
                    append("\n")
                    appendExample("h20", R.string.search_example_h)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(0.6f)
            )
        }
    }

    when (selectedPage) {
        DeadlineType.TASK -> {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 96.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .fadingTopEdge(height = 16.dp),
            ) {
                itemsIndexed(
                    items = searchResults,
                    key = { _, it -> it.id }
                ) { index, item ->
                    AnimatedItem(
                        item = item,
                        index = index
                    ) {
                        val startTime = GlobalUtils.parseDateTime(item.startTime)
                        val endTime = GlobalUtils.parseDateTime(item.endTime)
                        val now = LocalDateTime.now()

                        val remainingTimeText =
                            if (!item.isCompleted)
                                GlobalUtils.buildRemainingTime(
                                    context,
                                    startTime,
                                    endTime,
                                    true,
                                    now
                                )
                            else stringResource(R.string.completed)

                        val progress = computeProgress(startTime, endTime, now)
                        val status =
                            DDLStatus.calculateStatus(
                                startTime,
                                endTime,
                                now,
                                item.isCompleted
                            )

                        DDLItemCardSimplified(
                            title = item.name,
                            remainingTimeAlt = remainingTimeText,
                            note = item.note,
                            progress = progress,
                            isStarred = item.isStared,
                            status = status,
                            onClick = {
                                val intent =
                                    DeadlineDetailActivity.newIntent(context, item)
                                activity?.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        DeadlineType.HABIT -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2), // 👉 手机上固定两列
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = searchResults,
                    key = { _, it -> it.id }
                ) { index, item ->
                    AnimatedItem(
                        item = item,
                        index = index
                    ) {
                        HabitItem(
                            item = item,
                            onRefresh = {  },
                            updateDDL = {  },
                            onCheckInFailed = {  },
                            onCheckInSuccess = { _, _ -> },
                        )
                    }
                }
            }
        }
    }
}