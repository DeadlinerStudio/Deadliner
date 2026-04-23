package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

enum class TopAppBarStyle {
    CENTER, LARGE, SMALL
}

/**
 * 应用级 TopAppBar：统一 Material3 / MIUIX 两套实现。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    forceMiuix: Boolean = false
) {
    if (forceMiuix) {
        when (mode) {
            TopAppBarStyle.LARGE ->
                MiuixTopAppBar(
                    title = title,
                    color = MaterialTheme.colorScheme.surface,
                    navigationIcon = navigationIcon ?: {},
                    actions = actions,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    largeTitleColor = MaterialTheme.colorScheme.onSurface,
                )

            else ->
                SmallTopAppBar(
                    title = title,
                    color = MaterialTheme.colorScheme.surface,
                    navigationIcon = navigationIcon ?: {},
                    actions = actions,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                )
        }
    } else when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            when (mode) {
                TopAppBarStyle.CENTER ->
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )

                TopAppBarStyle.LARGE ->
                    LargeTopAppBar(
                        title = {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )

                else ->
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        navigationIcon = { navigationIcon?.invoke() },
                        actions = actions,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
            }
        }

        AppDesignSystem.MIUIX -> {
            when (mode) {
                TopAppBarStyle.LARGE ->
                    MiuixTopAppBar(
                        title = title,
                        color = MaterialTheme.colorScheme.surface,
                        navigationIcon = navigationIcon ?: {},
                        actions = actions,
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        largeTitleColor = MaterialTheme.colorScheme.onSurface,
                    )

                else ->
                    SmallTopAppBar(
                        title = title,
                        color = MaterialTheme.colorScheme.surface,
                        navigationIcon = navigationIcon ?: {},
                        actions = actions,
                        titleColor = MaterialTheme.colorScheme.onSurface,
                    )
            }
        }
    }
}
