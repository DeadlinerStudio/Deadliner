package com.aritxonly.deadliner

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.aritxonly.deadliner.logging.AppLog
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import com.aritxonly.deadliner.ui.base.LocalNavigationOverlay
import com.aritxonly.deadliner.ui.base.LocalNavigationOverlayContentPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.main.modern.ModernHost
import com.aritxonly.deadliner.ui.main.simplified.SimplifiedHost
import com.aritxonly.deadliner.ui.focus.FocusGlobalBar
import com.aritxonly.deadliner.ui.focus.FocusTimerEntryHost
import com.aritxonly.deadliner.ui.focus.FocusTimerDockLayout
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.platform.PlatformLogSink
import com.deadliner.platform.PlatformLogger
import com.deadliner.platform.KmpBuildMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : DeadlinerAppCompatActivity() {
    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.initialize(applicationContext)
        PlatformLogSink.configure(filesDir.absolutePath, android.os.Process.myPid().toLong())
        PlatformLogger().i("kmp.build", "compiledAtUtc=" + KmpBuildMetadata.compiledAtUtc)

        enableEdgeToEdgeForAllDevices()

        DynamicColorsExtension.apply(this, GlobalUtils.seedColor)

        _showSearch.value = intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false)

        setContent {
            val searchActive by showSearch.collectAsState()
            val style by GlobalUtils.styleFlow.collectAsState()
            val focusState by KmpV6Runtime.focusTimerViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                KmpV6Runtime.focusTimerViewModel.initialize()
                while (isActive) {
                    delay(1_000)
                    KmpV6Runtime.focusTimerViewModel.tick()
                }
            }

            DeadlinerTheme {
                val focusOverlay: @androidx.compose.runtime.Composable (Modifier) -> Unit = { overlayModifier ->
                    AnimatedVisibility(
                        visible = focusState.timer != null,
                        modifier = overlayModifier,
                        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 2 },
                        exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight / 2 },
                    ) {
                        FocusGlobalBar(
                            state = focusState,
                            onOpen = { startActivity(FocusTimerActivity.newIntent(this@MainActivity, "", "")) },
                            onPause = KmpV6Runtime.focusTimerViewModel::pause,
                            onResume = KmpV6Runtime.focusTimerViewModel::resume,
                            onFinish = KmpV6Runtime.focusTimerViewModel::finish,
                        )
                    }
                }
                FocusTimerEntryHost(onOpenTimer = {
                    startActivity(FocusTimerActivity.newIntent(this@MainActivity, "", ""))
                }) {
                    CompositionLocalProvider(
                        LocalNavigationOverlay provides focusOverlay,
                        LocalNavigationOverlayContentPadding provides if (focusState.timer != null) FocusTimerDockLayout.ContentClearance else 0.dp,
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            when (style) {
                                UiStyle.Simplified  -> {
                                    val backdrop = rememberLayerBackdrop()
                                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                                        SimplifiedHost(
                                            searchActive = searchActive,
                                            onSearchActiveChange = { _showSearch.value = it },
                                            activity = this@MainActivity,
                                        )
                                    }
                                    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                                        focusOverlay(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                                            .padding(bottom = 92.dp))
                                    }
                                }
                                UiStyle.Miuix -> {
                                    ModernHost(
                                        searchActive = searchActive,
                                        onSearchActiveChange = { _showSearch.value = it },
                                        activity = this@MainActivity
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        _showSearch.value = intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false)

        AppLog.d("Search", _showSearch.value.toString())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

}
