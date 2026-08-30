package com.aritxonly.deadliner.ui.main.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType

@Composable
fun MainHostLifecycleCoordinator(
    activity: MainActivity,
    selectedPage: DeadlineType,
    onPageChanged: (DeadlineType) -> Unit,
    onResumed: (DeadlineType) -> Unit,
    onInitialExtra: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        GlobalUtils.decideHideFromRecent(context, activity)
        onInitialExtra()
    }

    LaunchedEffect(selectedPage) {
        onPageChanged(selectedPage)
    }

    DisposableEffect(lifecycleOwner, selectedPage) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResumed(selectedPage)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
