package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.Scaffold
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(
    navigateUp: () -> Unit
) {

    // WebView owns native rendering; do not record it into a Compose glass backdrop.
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_wiki),
                mode = TopAppBarStyle.SMALL,
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp,
                        modifier = (navIconPaddingModifier).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)
                    ) {
                        Icon(
                            com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = expressiveTypeModifier
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { contentPadding ->
        SettingsWebContent(
            url = "https://github.com/AritxOnly/Deadliner/wiki",
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        )
    }
}
