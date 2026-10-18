package com.aritxonly.deadliner.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Preview(
    name = "MIUIX Floating Tabs - Light",
    widthDp = 380,
    heightDp = 330,
    showBackground = true,
)
@Composable
private fun MiuixFloatingTabBarLightPreview() {
    FloatingTabBarPreviewScene(darkTheme = false)
}

@Preview(
    name = "MIUIX Floating Tabs - Dark",
    widthDp = 380,
    heightDp = 330,
    showBackground = true,
    backgroundColor = 0xFF111111,
)
@Composable
private fun MiuixFloatingTabBarDarkPreview() {
    FloatingTabBarPreviewScene(darkTheme = true)
}

@Composable
private fun FloatingTabBarPreviewScene(darkTheme: Boolean) {
    val background = if (darkTheme) Color(0xFF111214) else Color(0xFFF1F1F3)
    val accent = if (darkTheme) Color(0xFF8C8C92) else Color(0xFFD1D1D6)
    val secondary = if (darkTheme) Color(0xFF27282C) else Color.White
    val backdrop = rememberLayerBackdrop()
    var horizontalSelectedKey by remember { mutableStateOf("list") }
    var stackedSelectedKey by remember { mutableStateOf("overview") }

    val items = listOf(
        MiuixFloatingTabItem(
            key = "list",
            label = "清单",
            selectedIcon = com.aritxonly.deadliner.ui.mainListHomePainter(),
            unselectedIcon = com.aritxonly.deadliner.ui.mainListHomePainter(),
        ),
        MiuixFloatingTabItem(
            key = "overview",
            label = "概览",
            selectedIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_pie_chart_wght400),
            unselectedIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_pie_chart_wght400),
        ),
        MiuixFloatingTabItem(
            key = "capture",
            label = "速记",
            selectedIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_capture),
            unselectedIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_capture),
        ),
        MiuixFloatingTabItem(
            key = "lifi",
            label = "AI",
            selectedIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_lifi),
            unselectedIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_lifi),
        ),
    )

    MiuixTheme(colors = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        MaterialTheme {
            CompositionLocalProvider(
                LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(
                    enabled = true,
                    blurRadius = 48f,
                    noiseCoefficient = 0.07f,
                    navigationTintAlpha = if (darkTheme) 0.58f else 0.68f,
                ),
                LocalAdvancedMaterialBackdrop provides backdrop,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background),
                ) {
                    PreviewBackdrop(
                        accent = accent,
                        secondary = secondary,
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        MiuixFloatingTabBar(
                            items = items,
                            selectedKey = horizontalSelectedKey,
                            onItemSelected = { horizontalSelectedKey = it.key },
                            layout = MiuixFloatingTabLayout.Horizontal,
                        )
                        MiuixFloatingTabBar(
                            items = items,
                            selectedKey = stackedSelectedKey,
                            onItemSelected = { stackedSelectedKey = it.key },
                            layout = MiuixFloatingTabLayout.Stacked,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBackdrop(
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.62f).height(12.dp),
                    color = accent,
                    shape = RoundedCornerShape(8.dp),
                    content = {},
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(0.84f).height(10.dp),
                    color = secondary,
                    shape = RoundedCornerShape(8.dp),
                    content = {},
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(82.dp),
            color = secondary,
            shape = RoundedCornerShape(20.dp),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.72f), Color.Transparent),
                            ),
                        ),
                )
            },
        )
    }
}
