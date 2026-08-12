package com.aritxonly.deadliner

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.view.Gravity
import android.view.Window
import androidx.activity.compose.setContent
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.ui.archive.ArchiveScreen
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

class ArchiveActivity : DeadlinerAppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, ArchiveActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enableEdgeToEdgeForAllDevices()
        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        DynamicColorsExtension.applyApp(application, GlobalUtils.seedColor)
        DynamicColorsExtension.apply(this, GlobalUtils.seedColor)
        GlobalUtils.decideHideFromRecent(this, this)

        setupWindowTransitions()

        setContent {
            DeadlinerTheme {
                ArchiveScreen(
                    onClose = { finishAfterTransition() },
                    onDataChanged = { setResult(RESULT_OK) },
                )
            }
        }
    }

    private fun setupWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.enterTransition = Slide(Gravity.BOTTOM).apply {
                duration = 300
            }
            window.exitTransition = Fade().apply {
                duration = 250
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}
