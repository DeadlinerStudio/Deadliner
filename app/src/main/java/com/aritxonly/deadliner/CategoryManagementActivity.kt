package com.aritxonly.deadliner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.category.CategoryEditorDraft
import com.aritxonly.deadliner.ui.category.CategoryEditorScreen
import com.aritxonly.deadliner.ui.category.CategoryManagementController
import com.aritxonly.deadliner.ui.category.CategoryManagementScreen
import com.aritxonly.deadliner.ui.category.toEditorDraft
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.widgets.glance.TaskWidgetRefreshCoordinator

class CategoryManagementActivity : DeadlinerAppCompatActivity() {
    companion object {
        private const val ROUTE_LIST = "category_list"
        private const val ROUTE_EDITOR = "category_editor"

        fun newIntent(context: Context): Intent = Intent(context, CategoryManagementActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)
        val controller = CategoryManagementController()

        setContent {
            val navController = rememberNavController()
            var categories by remember { mutableStateOf(controller.load()) }
            var editingUid by rememberSaveable { mutableStateOf<String?>(null) }
            val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
            }
            val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                fadeOut(tween(300))
            }
            val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                fadeIn(tween(300))
            }
            val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
            }

            fun refreshCategories() {
                categories = controller.load()
                TaskWidgetRefreshCoordinator.requestRefresh(this@CategoryManagementActivity)
            }

            fun deleteCategory(uid: String, onSuccess: () -> Unit = {}) {
                controller.delete(uid)
                    .onSuccess {
                        refreshCategories()
                        Toast.makeText(
                            this@CategoryManagementActivity,
                            R.string.category_deleted,
                            Toast.LENGTH_SHORT,
                        ).show()
                        onSuccess()
                    }
                    .onFailure {
                        Toast.makeText(
                            this@CategoryManagementActivity,
                            R.string.category_delete_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
            }

            DeadlinerTheme {
                ProvideAdvancedMaterialDialogBlurHost {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_LIST,
                                enterTransition = enter,
                                exitTransition = exit,
                                popEnterTransition = popEnter,
                                popExitTransition = popExit,
                            ) {
                                composable(ROUTE_LIST) {
                                    CategoryManagementScreen(
                                        categories = categories,
                                        onBack = { finishAfterTransition() },
                                        onAdd = {
                                            editingUid = null
                                            navController.navigate(ROUTE_EDITOR)
                                        },
                                        onEdit = { uid ->
                                            editingUid = uid
                                            navController.navigate(ROUTE_EDITOR)
                                        },
                                        onDelete = { deleteCategory(it) },
                                    )
                                }
                                composable(ROUTE_EDITOR) {
                                    val initialDraft = categories
                                        .firstOrNull { it.uid == editingUid }
                                        ?.toEditorDraft()
                                        ?: CategoryEditorDraft()
                                    CategoryEditorScreen(
                                        initialDraft = initialDraft,
                                        isEditing = editingUid != null,
                                        onBack = { navController.navigateUp() },
                                        onSave = { draft ->
                                            controller.save(editingUid, draft).map {
                                                refreshCategories()
                                                Toast.makeText(
                                                    this@CategoryManagementActivity,
                                                    R.string.category_saved,
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                                navController.navigateUp()
                                            }
                                        },
                                        onDelete = editingUid?.let { uid ->
                                            { deleteCategory(uid) { navController.navigateUp() } }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
