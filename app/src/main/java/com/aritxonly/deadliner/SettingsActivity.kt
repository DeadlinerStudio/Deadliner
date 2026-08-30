package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.drawToBitmap
import com.aritxonly.deadliner.ui.settings.AboutSettingsScreen
import com.aritxonly.deadliner.ui.settings.AdvancedMaterialSettingsScreen
import com.aritxonly.deadliner.ui.settings.AppIconSettingsScreen
import com.aritxonly.deadliner.ui.settings.ArchiveSettingsScreen
import com.aritxonly.deadliner.ui.settings.BackupSettingsScreen
import com.aritxonly.deadliner.ui.settings.BadgeSettingsScreen
import com.aritxonly.deadliner.ui.settings.AISettingsScreen
import com.aritxonly.deadliner.ui.settings.DesignColorSettingsScreen
import com.aritxonly.deadliner.ui.settings.CustomDisplayScaleSettingsScreen
import com.aritxonly.deadliner.ui.settings.DisplayScaleSettingsScreen
import com.aritxonly.deadliner.ui.settings.LabSettingsScreen
import com.aritxonly.deadliner.ui.settings.DonateScreen
import com.aritxonly.deadliner.ui.settings.FeedbackScreen
import com.aritxonly.deadliner.ui.settings.BehaviorSettingsScreen
import com.aritxonly.deadliner.ui.settings.AppearanceSettingsScreen
import com.aritxonly.deadliner.ui.settings.LicenseScreen
import com.aritxonly.deadliner.ui.settings.MainSettingsScreen
import com.aritxonly.deadliner.ui.settings.ModelSettingsScreen
import com.aritxonly.deadliner.ui.settings.NotificationSettingsScreen
import com.aritxonly.deadliner.ui.settings.PolicyScreen
import com.aritxonly.deadliner.ui.settings.PromptSettingsScreen
import com.aritxonly.deadliner.ui.settings.ProfileSettingsScreen
import com.aritxonly.deadliner.ui.settings.UpdateScreen
import com.aritxonly.deadliner.ui.settings.VibrationSettingsScreen
import com.aritxonly.deadliner.ui.settings.WebSettingsScreen
import com.aritxonly.deadliner.ui.settings.WidgetSettingsScreen
import com.aritxonly.deadliner.ui.settings.WikiScreen
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.DisplayScaleManager
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.ui.settings.UiSettingsScreen
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Checkbox
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.base.advancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

sealed class SettingsRoute(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val supportRes: Int?,
    @DrawableRes val iconRes: Int?
) {
    object Main : SettingsRoute("main", R.string.settings_title, null, null)

    // region: 二级界面
    object Behavior : SettingsRoute("behavior", R.string.settings_behavior, R.string.settings_support_behavior, R.drawable.ic_tune)
    object Appearance : SettingsRoute("appearance", R.string.settings_interface_display, R.string.settings_support_interface_display, R.drawable.ic_palette)
    object Notification : SettingsRoute("notification", R.string.settings_notification, R.string.settings_support_notification, R.drawable.ic_notification_settings)
    object Backup : SettingsRoute("backup", R.string.settings_backup, R.string.settings_support_backup, R.drawable.ic_backup_settings)
    object AI : SettingsRoute("ai", R.string.settings_deadliner_ai, R.string.settings_support_deadliner_ai,
        GlobalUtils.getDeadlinerAIConfig().getCurrentLogo()
    )
    object WebDAV : SettingsRoute("webdav", R.string.settings_webdav, R.string.settings_support_webdav, R.drawable.ic_cloud)
    object Lab : SettingsRoute("lab", R.string.settings_lab, R.string.settings_support_lab, R.drawable.ic_lab)
    object Widget : SettingsRoute("widget", R.string.settings_widget, R.string.settings_support_widget, R.drawable.ic_widgets_settings)
    object Wiki : SettingsRoute("wiki", R.string.settings_wiki, R.string.settings_support_wiki, R.drawable.ic_manual)
    object Feedback : SettingsRoute("feedback", R.string.settings_feedback, R.string.settings_support_feedback, R.drawable.ic_support)
    object About : SettingsRoute("about", R.string.settings_about, R.string.settings_support_about, R.drawable.ic_package)
    // endregion

    // region:三级界面
    object Vibration : SettingsRoute("vibration", R.string.settings_vibration_title, R.string.settings_support_vibration, null)
    object Archive : SettingsRoute("archive", R.string.settings_auto_archive_title, R.string.settings_support_auto_archive, null)
    object Model : SettingsRoute("model", R.string.settings_model_endpoint, R.string.settings_support_model_endpoint, null)
    object Prompt : SettingsRoute("prompt", R.string.settings_ai_custom_prompt, R.string.settings_support_ai_custom_prompt, null)

    object Badge : SettingsRoute("badge", R.string.settings_tasks_badge_title, R.string.settings_support_tasks_badge, null)
    object AppearanceDesign : SettingsRoute("appearance_design", R.string.settings_design_color_title, R.string.settings_design_color_summary, null)
    object AppearanceMaterial : SettingsRoute("appearance_material", R.string.settings_advanced_material, R.string.settings_advanced_material_summary, null)
    object UI : SettingsRoute("ui", R.string.settings_ui_mode_title, R.string.settings_support_ui_mode, null)
    object DisplayScale : SettingsRoute("display_scale", R.string.settings_display_size_title, R.string.settings_support_display_size_menu, null)
    object DisplayScaleCustom : SettingsRoute("display_scale_custom", R.string.settings_display_size_custom_title, R.string.settings_support_display_size_lab, null)
    object AppIcon : SettingsRoute("app_icon", R.string.settings_app_icon_title, R.string.settings_app_icon_summary, null)
    object Profile : SettingsRoute("profile", R.string.edit_profile, R.string.settings_support_profile, null)

    object Update : SettingsRoute("update", R.string.settings_check_for_updates, R.string.settings_check_for_updates, R.drawable.ic_update)
    object License : SettingsRoute("license", R.string.settings_license, R.string.settings_license_summary, R.drawable.ic_license)
    object Policy : SettingsRoute("policy", R.string.settings_privacy_policy, R.string.settings_privacy_summary, R.drawable.ic_privacy)
    object Donate : SettingsRoute("donate", R.string.settings_donate, R.string.settings_support_donate, null)
    // endregion

    companion object {
        val allSubRoutes = listOf(
            listOf(
                Appearance,
                Behavior,
                Notification,
                Backup,
            ),
            listOf(Widget),
            listOf(
                AI,
                WebDAV,
            ),
            listOf(Lab),
            listOf(
                Wiki,
                Feedback,
                About
            )
        )

        val behaviorThirdRoutes = listOf(
            Vibration, Archive
        )

        val appearanceThirdRoutes = listOf(
            AppearanceDesign, AppearanceMaterial, AppIcon, UI, DisplayScale
        )

        val aboutThirdRoutes = listOf(
            Update, License, Policy, Donate
        )
    }
}

private data class SettingsDesignModeTransition(
    val targetMode: AppearanceDesignMode,
    val snapshotBitmap: Bitmap?,
    val overlayColorArgb: Int,
    val indicatorColorArgb: Int,
)

private data class CustomCalendarFilterDialogState(
    val allItems: List<String>,
    val selectedItems: Set<String>,
)

class SettingsActivity : DeadlinerAppCompatActivity() {
    companion object {
        private const val EXPORT_REQUEST_CODE = 1001
        private const val IMPORT_REQUEST_CODE = 1002

        const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
        private const val STATE_CURRENT_ROUTE = "state_current_route"
    }

    private var latestRoute: String = SettingsRoute.Main.route
    private var designModeTransitionOverlay: View? = null
    private var designModeTransitionInFlight: Boolean = false
    private var customCalendarFilterDialogState by mutableStateOf<CustomCalendarFilterDialogState?>(null)
    private var showCustomCalendarNewItemDialog by mutableStateOf(false)
    private var customCalendarNewItemDraft by mutableStateOf("")
    private var importConfirmUri by mutableStateOf<Uri?>(null)
    private var showImportSuccessDialog by mutableStateOf(false)
    private var showRestartAppDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()

        super.onCreate(savedInstanceState)

        val initialRoute = savedInstanceState?.getString(STATE_CURRENT_ROUTE)
            ?: intent.getStringExtra(EXTRA_INITIAL_ROUTE)
            ?: SettingsRoute.Main.route
        latestRoute = initialRoute
        intent.putExtra(EXTRA_INITIAL_ROUTE, initialRoute)

        setContent {
            val navController = rememberNavController()
            var restoredRoute by rememberSaveable {
                mutableStateOf(initialRoute)
            }
            val currentBackStackEntry by navController.currentBackStackEntryAsState()

            val defaultEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(tween(300))
            }
            val defaultExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                fadeOut(tween(300))
            }
            val defaultPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                fadeIn(tween(300))
            }
            val defaultPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
            }

            LaunchedEffect(restoredRoute) {
                val route = restoredRoute
                if (route != SettingsRoute.Main.route && navController.currentDestination?.route != route) {
                    navController.navigate(route) {
                        popUpTo(SettingsRoute.Main.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            LaunchedEffect(currentBackStackEntry?.destination?.route) {
                currentBackStackEntry?.destination?.route?.let {
                    restoredRoute = it
                    latestRoute = it
                    intent.putExtra(EXTRA_INITIAL_ROUTE, it)
                }
            }

            DeadlinerTheme {
                val colorScheme = MaterialTheme.colorScheme
                ProvideAdvancedMaterialDialogBlurHost {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .advancedMaterialDialogBlurHost()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = colorScheme.surface
                        ) {
                            NavHost(
                                navController,
                                startDestination = SettingsRoute.Main.route,
                                enterTransition = defaultEnter,
                                exitTransition = defaultExit,
                                popEnterTransition = defaultPopEnter,
                                popExitTransition = defaultPopExit
                            ) {
                        composable(SettingsRoute.Main.route) {
                            MainSettingsScreen(navController, onClose = { finishAfterTransition() })
                        }

                        composable(SettingsRoute.Appearance.route) {
                            AppearanceSettingsScreen(
                                navController
                            ) { navController.navigateUp() }
                        }
                        composable(SettingsRoute.Behavior.route) {
                            BehaviorSettingsScreen(
                                navController, handleRestart = { showRestartAppDialog = true }
                            ) { navController.navigateUp() }
                        }
                        composable(SettingsRoute.Profile.route) {
                            ProfileSettingsScreen { navController.navigateUp() }
                        }
                        composable(SettingsRoute.Notification.route) { NotificationSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Backup.route) {
                            BackupSettingsScreen(
                                handleExport = { createBackup() },
                                handleImport = {
                                    DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
                                    GlobalUtils.NotificationStatusManager.clearAllNotified()
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        getString(R.string.destroy_alarms),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    openBackup()
                                },
                                handleWebSettings = { navController.navigate(SettingsRoute.WebDAV.route) }
                            ) { navController.navigateUp() }
                        }

                        composable(SettingsRoute.Widget.route) { WidgetSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.AI.route) { AISettingsScreen(navController) { navController.navigateUp() } }
                        composable(SettingsRoute.WebDAV.route) { WebSettingsScreen { navController.navigateUp() } }

                        composable(SettingsRoute.Lab.route) {
                            LabSettingsScreen(
                                onClickCustomDisplayScale = {
                                    navController.navigate(SettingsRoute.DisplayScaleCustom.route)
                                },
                                onClickCustomFilter = {
                                    openCustomCalendarFilterDialog()
                                },
                                onClickCancelAll = {
                                    DeadlineAlarmScheduler.cancelAllAlarms(applicationContext)
                                    GlobalUtils.NotificationStatusManager.clearAllNotified()
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        getString(R.string.destroy_alarms),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onClickShowIntro = {
                                    GlobalUtils.showIntroPage = true
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        getString(R.string.show_intro_next_time),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            ) { navController.navigateUp() }
                        }

                        composable(SettingsRoute.Wiki.route) { WikiScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Feedback.route) { FeedbackScreen { navController.navigateUp() } }
                        composable(SettingsRoute.About.route) { AboutSettingsScreen(navController) { navController.navigateUp() } }

                        composable(SettingsRoute.Vibration.route) { VibrationSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Archive.route) { ArchiveSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Badge.route) { BadgeSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.AppearanceDesign.route) {
                            DesignColorSettingsScreen(
                                navigateUp = { navController.navigateUp() },
                                onRequestDesignModeChange = { mode ->
                                    requestDesignModeTransition(
                                        transition = SettingsDesignModeTransition(
                                            targetMode = mode,
                                            snapshotBitmap = window.decorView.rootView.drawToBitmap(Bitmap.Config.ARGB_8888),
                                            overlayColorArgb = colorScheme.surface.toArgb(),
                                            indicatorColorArgb = colorScheme.primary.toArgb(),
                                        )
                                    )
                                }
                            )
                        }
                        composable(SettingsRoute.AppearanceMaterial.route) { AdvancedMaterialSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.DisplayScale.route) { DisplayScaleSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.DisplayScaleCustom.route) { CustomDisplayScaleSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.AppIcon.route) { AppIconSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.UI.route) { UiSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Model.route) { ModelSettingsScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Prompt.route) { PromptSettingsScreen { navController.navigateUp() } }

                        composable(SettingsRoute.Update.route) { UpdateScreen { navController.navigateUp() } }
                        composable(SettingsRoute.License.route) { LicenseScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Policy.route) { PolicyScreen { navController.navigateUp() } }
                        composable(SettingsRoute.Donate.route) { DonateScreen { navController.navigateUp() } }
                            }
                        }

                        if (customCalendarFilterDialogState != null && !showCustomCalendarNewItemDialog) {
                            CustomCalendarFilterDialog(
                                state = customCalendarFilterDialogState!!,
                                onDismiss = { customCalendarFilterDialogState = null },
                                onAddFilter = {
                                    customCalendarNewItemDraft = ""
                                    showCustomCalendarNewItemDialog = true
                                },
                                onToggleItem = ::toggleCustomCalendarFilterItem,
                                onConfirm = ::saveCustomCalendarFilterSelection
                            )
                        }

                        if (showCustomCalendarNewItemDialog) {
                            NewCustomCalendarFilterDialog(
                                draft = customCalendarNewItemDraft,
                                onDraftChange = { customCalendarNewItemDraft = it },
                                onDismiss = { showCustomCalendarNewItemDialog = false },
                                onConfirm = ::addCustomCalendarFilterItem
                            )
                        }

                        importConfirmUri?.let { pendingUri ->
                            AlertDialog(
                                show = true,
                                onDismissRequest = { importConfirmUri = null },
                                title = { Text(stringResource(R.string.confirm_import)) },
                                text = { Text(stringResource(R.string.confirm_import_message)) },
                                miuixTitle = getString(R.string.confirm_import),
                                miuixSummary = getString(R.string.confirm_import_message),
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            importConfirmUri = null
                                            performImport(pendingUri)
                                        }
                                    ) {
                                        Text(stringResource(R.string.accept))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { importConfirmUri = null }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }

                        if (showImportSuccessDialog) {
                            AlertDialog(
                                show = true,
                                onDismissRequest = { showImportSuccessDialog = false },
                                text = { Text(stringResource(R.string.import_success)) },
                                miuixSummary = getString(R.string.import_success),
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showImportSuccessDialog = false
                                            restartApp()
                                        }
                                    ) {
                                        Text(stringResource(R.string.restart_now))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showImportSuccessDialog = false }) {
                                        Text(stringResource(R.string.later))
                                    }
                                }
                            )
                        }

                        if (showRestartAppDialog) {
                            AlertDialog(
                                show = true,
                                onDismissRequest = { showRestartAppDialog = false },
                                text = { Text(stringResource(R.string.embedded_activities_success)) },
                                miuixSummary = getString(R.string.embedded_activities_success),
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showRestartAppDialog = false
                                            restartApp()
                                        }
                                    ) {
                                        Text(stringResource(R.string.restart_now))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRestartAppDialog = false }) {
                                        Text(stringResource(R.string.later))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_ROUTE, latestRoute)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EXPORT_REQUEST_CODE -> handleExportResult(resultCode, data)
            IMPORT_REQUEST_CODE -> handleImportResult(resultCode, data)
        }
    }

    // 导出数据库
    @SuppressLint("SimpleDateFormat")
    private fun createBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/x-sqlite3"
            putExtra(Intent.EXTRA_TITLE, "deadliner_backup_${SimpleDateFormat("yyyyMMdd_HHmmss").format(
                Date()
            )}.db")
        }

        startActivityForResult(intent, EXPORT_REQUEST_CODE)
    }

    // 导入数据库
    private fun openBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/x-sqlite3",
                "application/vnd.sqlite3",
                "application/octet-stream",
                "application/sqlite3",
                "application/x-sql"
            ))
            // 可选：限制文件扩展名
            putExtra(Intent.EXTRA_TITLE, "*.db")
        }

        // 兼容旧版本
        val chooser = Intent.createChooser(intent, getString(R.string.choose_backup))
        startActivityForResult(chooser, IMPORT_REQUEST_CODE)
    }

    private fun handleExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME)
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    showToast(getString(R.string.export_success))
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast(getString(R.string.export_failed, e.localizedMessage))
                }
            }
        }
    }

    private fun handleImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                importConfirmUri = uri
            }
        }
    }

    private fun performImport(uri: Uri) {
        try {
            // 关闭当前数据库连接
            DatabaseHelper.closeInstance()

            // 替换数据库文件
            val dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 重新初始化数据库
            DatabaseHelper.getInstance(this)

            showImportSuccessDialog = true
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.import_failed, e.localizedMessage))
        }
    }

    private fun requestDesignModeTransition(transition: SettingsDesignModeTransition) {
        if (designModeTransitionInFlight) return

        val contentRoot = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            ?: run {
                GlobalUtils.designMode = transition.targetMode
                return
            }

        designModeTransitionInFlight = true
        designModeTransitionOverlay?.let { existing ->
            contentRoot.removeView(existing)
        }

        val overlay = buildDesignModeTransitionOverlay(transition)
        designModeTransitionOverlay = overlay
        contentRoot.addView(overlay)

        overlay.animate()
            .alpha(1f)
            .setDuration(220L)
            .withEndAction {
                GlobalUtils.designMode = transition.targetMode
                dismissDesignModeTransitionOverlayAfterNextDraw(
                    contentRoot = contentRoot,
                    overlay = overlay,
                    snapshotBitmap = transition.snapshotBitmap,
                )
            }
            .start()
    }

    private fun buildDesignModeTransitionOverlay(
        transition: SettingsDesignModeTransition,
    ): View {
        val overlay = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            alpha = 0f
            isClickable = true
            isFocusable = true
        }

        transition.snapshotBitmap?.let { snapshot ->
            overlay.addView(
                ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = ImageView.ScaleType.FIT_XY
                    setImageBitmap(snapshot)
                }
            )
        }

        overlay.addView(
            View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(applyAlpha(transition.overlayColorArgb, 0.92f))
            }
        )

        overlay.addView(
            CircularProgressIndicator(this).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(64), dpToPx(64)).apply {
                    gravity = Gravity.CENTER
                }
                isIndeterminate = true
                setIndicatorColor(transition.indicatorColorArgb)
                trackThickness = dpToPx(4)
                trackColor = applyAlpha(transition.indicatorColorArgb, 0.16f)
            }
        )

        return overlay
    }

    private fun dismissDesignModeTransitionOverlayAfterNextDraw(
        contentRoot: ViewGroup,
        overlay: View,
        snapshotBitmap: Bitmap?,
    ) {
        contentRoot.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (contentRoot.viewTreeObserver.isAlive) {
                    contentRoot.viewTreeObserver.removeOnPreDrawListener(this)
                }

                contentRoot.post {
                    contentRoot.post {
                        overlay.animate()
                            .alpha(0f)
                            .setDuration(320L)
                            .withEndAction {
                                if (overlay.parent === contentRoot) {
                                    contentRoot.removeView(overlay)
                                }
                                if (designModeTransitionOverlay === overlay) {
                                    designModeTransitionOverlay = null
                                }
                                snapshotBitmap?.recycle()
                                designModeTransitionInFlight = false
                            }
                            .start()
                    }
                }

                return true
            }
        })
        contentRoot.invalidate()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun applyAlpha(color: Int, alphaFraction: Float): Int {
        val alpha = (255 * alphaFraction.coerceIn(0f, 1f)).toInt()
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun openCustomCalendarFilterDialog() {
        val allItems = GlobalUtils.customCalendarFilterList?.filterNotNull()?.distinct().orEmpty()
        val persistedSelection = GlobalUtils.customCalendarFilterListSelected
            ?.filterNotNull()
            ?.toSet()
        val selectedItems = if (persistedSelection.isNullOrEmpty()) {
            allItems.toSet()
        } else {
            persistedSelection
        }
        customCalendarFilterDialogState = CustomCalendarFilterDialogState(
            allItems = allItems,
            selectedItems = selectedItems
        )
    }

    private fun toggleCustomCalendarFilterItem(item: String, checked: Boolean) {
        val state = customCalendarFilterDialogState ?: return
        customCalendarFilterDialogState = state.copy(
            selectedItems = if (checked) {
                state.selectedItems + item
            } else {
                state.selectedItems - item
            }
        )
    }

    private fun addCustomCalendarFilterItem() {
        val state = customCalendarFilterDialogState ?: return
        val newItem = customCalendarNewItemDraft.trim()
        if (newItem.isEmpty() || state.allItems.contains(newItem)) {
            showCustomCalendarNewItemDialog = false
            return
        }

        val updatedItems = state.allItems + newItem
        val updatedSelection = state.selectedItems + newItem
        GlobalUtils.customCalendarFilterList = updatedItems.toSet()
        GlobalUtils.customCalendarFilterListSelected = updatedSelection
        customCalendarFilterDialogState = state.copy(
            allItems = updatedItems,
            selectedItems = updatedSelection
        )
        customCalendarNewItemDraft = ""
        showCustomCalendarNewItemDialog = false
    }

    private fun saveCustomCalendarFilterSelection() {
        val state = customCalendarFilterDialogState ?: return
        GlobalUtils.customCalendarFilterList = state.allItems.toSet()
        GlobalUtils.customCalendarFilterListSelected = state.selectedItems
        customCalendarFilterDialogState = null
    }

    private fun restartApp() {
        DisplayScaleManager.restartApp(this)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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

@Composable
private fun CustomCalendarFilterDialog(
    state: CustomCalendarFilterDialogState,
    onDismiss: () -> Unit,
    onAddFilter: () -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        show = true,
        onDismissRequest = onDismiss,
        renderTextContentInMiuix = true,
        title = { Text(stringResource(R.string.select_filter_calendar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onAddFilter) {
                    Text(stringResource(R.string.filter_add_one))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.allItems.forEach { item ->
                        val checked = state.selectedItems.contains(item)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(item, !checked) }
                                .padding(vertical = 6.dp),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleItem(item, it) }
                            )
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        miuixTitle = stringResource(R.string.select_filter_calendar),
        miuixSummary = stringResource(R.string.filter_add_one),
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun NewCustomCalendarFilterDialog(
    draft: String,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        show = true,
        onDismissRequest = onDismiss,
        renderTextContentInMiuix = true,
        title = { Text(stringResource(R.string.new_filter_title)) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.new_filter_name)) },
                miuixLabel = stringResource(R.string.new_filter_name),
                singleLine = true
            )
        },
        miuixTitle = stringResource(R.string.new_filter_title),
        miuixSummary = stringResource(R.string.new_filter_name),
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
