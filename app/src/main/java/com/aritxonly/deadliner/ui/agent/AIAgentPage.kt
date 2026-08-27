package com.aritxonly.deadliner.ui.agent

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AIAgentPresentation {
    Overlay,
    Page,
}

@Immutable
internal data class AIAgentChromePolicy(
    val showDismissButton: Boolean,
    val handleBackPress: Boolean,
)

internal fun aiAgentChromePolicy(
    presentation: AIAgentPresentation,
): AIAgentChromePolicy = when (presentation) {
    AIAgentPresentation.Overlay -> AIAgentChromePolicy(
        showDismissButton = true,
        handleBackPress = true,
    )

    AIAgentPresentation.Page -> AIAgentChromePolicy(
        showDismissButton = false,
        handleBackPress = false,
    )
}

object AIAgentPageDefaults {
    val OverlayNavigationInputPadding: Dp = 112.dp
    val InsetNavigationInputPadding: Dp = 40.dp

    fun inputBottomPadding(navigationOverlaysContent: Boolean): Dp =
        if (navigationOverlaysContent) OverlayNavigationInputPadding else InsetNavigationInputPadding
}

@Composable
fun AIAgentPage(
    onAddDDL: (Intent) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    initialText: String = "",
    inputBottomPadding: Dp = AIAgentPageDefaults.OverlayNavigationInputPadding,
    respondIme: Boolean = true,
) {
    AIOverlay(
        initialText = initialText,
        onDismiss = {},
        onAddDDL = onAddDDL,
        modifier = modifier,
        respondIme = respondIme,
        presentation = AIAgentPresentation.Page,
        inputBottomPadding = inputBottomPadding,
    )
}
