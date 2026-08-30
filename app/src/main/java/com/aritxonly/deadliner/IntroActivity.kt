package com.aritxonly.deadliner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.ui.intro.IntroFlowScreen
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

class IntroActivity : DeadlinerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeForAllDevices()
        DynamicColorsExtension.apply(this)

        setContent {
            DeadlinerTheme {
                IntroFlowScreen(
                    onFinish = ::goToMainActivity
                )
            }
        }
    }

    private fun goToMainActivity() {
        GlobalUtils.firstRun = false
        GlobalUtils.showIntroPage = false

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
