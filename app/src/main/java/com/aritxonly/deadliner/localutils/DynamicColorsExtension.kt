package com.aritxonly.deadliner.localutils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.R
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

object DynamicColorsExtension {

    fun apply(activity: Activity, seed: String? = null, isMiuixMode: Boolean = GlobalUtils.miuixMode) {
        val builder = DynamicColorsOptions.Builder()

        if (!seed.isNullOrBlank()) {
            try {
                builder.setContentBasedSource(seed.toColorInt())
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        DynamicColors.applyToActivityIfAvailable(activity, builder.build())

        if (isMiuixMode) {
            activity.theme.applyStyle(R.style.ThemeOverlay_Deadliner_MiuixBackground, true)
        }
    }

    fun applyApp(app: Application, seed: String? = null, isMiuixMode: Boolean = GlobalUtils.miuixMode) {
        val builder = DynamicColorsOptions.Builder()

        if (!seed.isNullOrBlank()) {
            try {
                builder.setContentBasedSource(seed.toColorInt())
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        DynamicColors.applyToActivitiesIfAvailable(app, builder.build())

        if (isMiuixMode) {
            app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                    super.onActivityPreCreated(activity, savedInstanceState)
                    activity.theme.applyStyle(R.style.ThemeOverlay_Deadliner_MiuixBackground, true)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        }
    }
}