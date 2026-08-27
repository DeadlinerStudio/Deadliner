package com.aritxonly.deadliner.ui.main.simplified

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
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
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onMoreAnchorChange: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    useAvatar: Boolean = false,
    avatarPainter: Painter? = null,
    activity: MainActivity? = null,
    expanded: Boolean,
    onExpandedChangeExternal: (Boolean) -> Unit = {},
    onArchiveClick: (() -> Unit)? = null,
    useSurfaceColors: Boolean = false,
    miuixMode: Boolean = LocalAppDesignSystem.current == AppDesignSystem.MIUIX,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
            withFrameNanos { }
            keyboardController?.show()
        }
    }

    val isEnabled = GlobalUtils.motivationalQuotes
    val excitementArray = stringArrayResource(id = R.array.excitement_array).toList()
    var idx by rememberSaveable {
        mutableIntStateOf(if (excitementArray.isNotEmpty()) (0 until excitementArray.size).random() else 0)
    }

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
        val searchBarColors = if (useSurfaceColors) {
            SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                dividerColor = MaterialTheme.colorScheme.surface,
            )
        } else {
            SearchBarDefaults.colors()
        }
        val inputFieldColors = if (useSurfaceColors) {
            SearchBarDefaults.inputFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            )
        } else {
            SearchBarDefaults.inputFieldColors()
        }

        Box(modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
            M3SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = searchBarPadding)
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        modifier = Modifier.focusRequester(focusRequester),
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
                                    M3Icon(com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_arrow_back_wght400), stringResource(R.string.back))
                                }
                            } else {
                                M3Icon(com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_search_wght400), stringResource(R.string.search_events))
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
                                        M3Icon(com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_more_vert_wght400), stringResource(R.string.settings_more))
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (textFieldState.text.isNotEmpty()) {
                                        M3IconButton(onClick = { textFieldState.edit { replace(0, length, "") } }) {
                                            M3Icon(com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_close_wght400), stringResource(R.string.close))
                                        }
                                    }
                                    if (onArchiveClick != null) {
                                        M3IconButton(onClick = onArchiveClick) {
                                            M3Icon(
                                                com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_inbox_wght400),
                                                stringResource(R.string.archive),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = inputFieldColors,
                    )
                },
                expanded = expanded,
                onExpandedChange = { exp ->
                    if (!exp) textFieldState.edit { replace(0, length, "") }
                    onExpandedChangeExternal(exp)
                },
                colors = searchBarColors,
            ) {
                content()
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
                    modifier = Modifier.focusRequester(focusRequester),
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
            },
            outsideEndAction = onArchiveClick?.let { openArchive ->
                {
                    MiuixIconButton(onClick = openArchive) {
                        MiuixIcon(
                            painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_inbox_wght400),
                            contentDescription = stringResource(R.string.archive),
                        )
                    }
                }
            },
        ) {
            content()
        }
    }
}
