package com.aritxonly.deadliner.ui.picker

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

private fun Context.pickerActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.pickerActivity()
    else -> null
}

/** Mounted once inside DeadlinerTheme's existing scaffold and advanced-material dialog host. */
@Composable
fun DateTimePickerHost() {
    val activity = LocalContext.current.pickerActivity() ?: return
    val manager = activity.supportFragmentManager
    val controller = remember(manager) { DateTimePickerController() }
    DisposableEffect(manager, controller) {
        DateTimePickers.attach(manager, controller)
        onDispose { DateTimePickers.detach(manager, controller) }
    }
    controller.request?.let { request ->
        key(request.id) {
            BackHandler { controller.dismiss() }
            MiuixDateTimePicker(request = request, onDismiss = controller::dismiss, onConfirm = controller::confirm)
        }
    }
}
