package com.aritxonly.deadliner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.aritxonly.deadliner.ui.material.glass.SoftGlassShadowTokens
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.ui.material.glass.softGlassShadowTokens

internal fun floatingNavigationShadowTokens(isDark: Boolean): SoftGlassShadowTokens =
    softGlassShadowTokens(isDark)

@Composable
internal fun Modifier.floatingNavigationShadow(
    shape: Shape,
    isDark: Boolean,
): Modifier {
    return softGlassShadow(
        shape = shape,
        isDark = isDark,
    )
}
