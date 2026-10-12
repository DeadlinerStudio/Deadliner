package com.aritxonly.deadliner.ui.main.modern

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import com.aritxonly.deadliner.ui.quickadd.quickAddAnchor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.ArchiveActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.OverviewContent
import com.aritxonly.deadliner.OverviewSettingsDialog
import com.aritxonly.deadliner.OverviewTopBar
import com.aritxonly.deadliner.OverviewTopBarWithTabs
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.capture.ui.CaptureContent
import com.aritxonly.deadliner.capture.ui.CaptureTopBar
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.agent.AIAgentPage
import com.aritxonly.deadliner.ui.agent.AIAgentPageDefaults
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import com.aritxonly.deadliner.ui.base.AdaptiveNavItem
import com.aritxonly.deadliner.ui.base.AdaptiveNavigationSuiteScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.FloatingActionButton
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.resolveTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.main.shared.MainSection
import com.aritxonly.deadliner.ui.main.modern.components.ModernListTopAppBar
import com.aritxonly.deadliner.ui.main.modern.components.ModernListSearchSlotHeight
import com.aritxonly.deadliner.ui.main.modern.components.ImmersiveListSearchEntry
import com.aritxonly.deadliner.ui.main.modern.components.ModernListSharedTransitionLayout
import com.aritxonly.deadliner.ui.main.modern.components.ModernListSearchTransition
import com.aritxonly.deadliner.ui.main.modern.components.modernListSearchSharedBoundsModifier
import com.aritxonly.deadliner.ui.main.shared.DeadlinerUrlIntake
import com.aritxonly.deadliner.ui.main.shared.MainDisplay
import com.aritxonly.deadliner.ui.main.shared.MainHostLifecycleCoordinator
import com.aritxonly.deadliner.ui.main.shared.MainSearchResultsContent
import com.aritxonly.deadliner.ui.main.shared.MainSearchScope
import com.aritxonly.deadliner.ui.main.shared.rememberMainHostUiState
import com.aritxonly.deadliner.ui.main.shared.rememberMainSelectionActionController
import com.aritxonly.deadliner.ui.main.shared.rememberMainHostState
import com.aritxonly.deadliner.ui.main.simplified.MainSearchBar
import com.aritxonly.deadliner.ui.category.CategoryFilter
import com.aritxonly.deadliner.ui.category.CategoryFilterResolver
import com.aritxonly.deadliner.ui.category.CategoryFilterSheet
import com.aritxonly.deadliner.ui.category.rememberActiveCategories
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButtonDefaults
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationAction
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalHomeFabColors
import kotlinx.coroutines.FlowPreview
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import java.io.File
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

private enum class ModernTopBarState {
    HIDDEN,
    LIST_DEFAULT,
    LIST_SELECTION,
    OVERVIEW,
    CAPTURE,
}

private const val WideLayoutMinWidthDp = 840
private const val FlattenedOverviewMinWidthDp = 1100

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    FlowPreview::class,
)
@Composable
fun ModernHost(
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val configuration = LocalConfiguration.current
    val isWideLayout = configuration.screenWidthDp >= WideLayoutMinWidthDp
    val useFlattenedOverview = configuration.screenWidthDp >= FlattenedOverviewMinWidthDp
    val taskVm = KmpV6Runtime.taskListViewModel
    val sharedHabitVm = KmpV6Runtime.habitListViewModel
    val captureVm: CaptureViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    val hostState = rememberMainHostState(DeadlineType.TASK)
    val uiState = rememberMainHostUiState()
    var overviewState by remember { mutableStateOf(KmpV6Runtime.overviewViewModel.loadNow()) }
    var showOverviewSettings by rememberSaveable { mutableStateOf(false) }
    var overviewSelectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCaptureMergeSheet by rememberSaveable { mutableStateOf(false) }
    var searchScope by rememberSaveable { mutableStateOf(MainSearchScope.ALL) }
    var activeSelectionPane by rememberSaveable { mutableStateOf<DeadlineType?>(null) }
    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var fireKey by remember { mutableIntStateOf(0) }
    var showCategoryFilter by rememberSaveable { mutableStateOf(false) }
    var categoryFilter by remember { mutableStateOf<CategoryFilter>(CategoryFilter.All) }

    val taskListState = rememberLazyListState()
    val habitListState = rememberLazyListState()
    val textFieldState = rememberTextFieldState()
    val captureState by captureVm.uiState.collectAsStateWithLifecycle()
    val searchQuery = textFieldState.text.toString()
    var overlayTopBarHeightPx by remember { mutableIntStateOf(0) }

    val canCollapseListTopBar = canCollapseModernListTopBar(
        selectedSection = hostState.selectedSection,
        selectedPage = hostState.selectedPage,
        selectionMode = hostState.selectionMode,
        searchActive = searchActive,
    )
    val currentCanCollapseListTopBar by rememberUpdatedState(canCollapseListTopBar)
    val listTopBarScrollBehavior = MiuixScrollBehavior(
        canScroll = { currentCanCollapseListTopBar },
    )
    val currentSelectedSection by rememberUpdatedState(hostState.selectedSection)
    val overviewTopBarScrollBehavior = MiuixScrollBehavior(
        canScroll = { currentSelectedSection == MainSection.OVERVIEW },
    )
    val captureTopBarScrollBehavior = MiuixScrollBehavior(
        canScroll = { currentSelectedSection == MainSection.CAPTURE },
    )

    LaunchedEffect(canCollapseListTopBar, searchActive) {
        // Keep the outgoing search entry at its current bounds while the shared container expands.
        if (!canCollapseListTopBar && !searchActive) {
            listTopBarScrollBehavior.state.heightOffset = 0f
            listTopBarScrollBehavior.state.contentOffset = 0f
        }
    }

    LaunchedEffect(isWideLayout) {
        if (isWideLayout && hostState.selectedPage != DeadlineType.TASK) {
            hostState.selectedPage = DeadlineType.TASK
        }
    }

    LaunchedEffect(hostState.selectionMode) {
        if (!hostState.selectionMode) {
            activeSelectionPane = null
            if (isWideLayout) {
                hostState.selectedPage = DeadlineType.TASK
            }
        }
    }
    fun celebrate() {
        parties = PartyPresets.festive()
        fireKey++
    }
    LaunchedEffect(parties) {
        if (parties.isNotEmpty()) {
            kotlinx.coroutines.delay(3500)
            parties = emptyList()
        }
    }

    val settingsState by KmpV6Runtime.settingsViewModel.uiState.collectAsStateWithLifecycle()
    val profile = settingsState.profile
    val avatarPainter: Painter? by remember(profile?.avatarFileName) {
        mutableStateOf(
            profile?.avatarFileName?.let { name ->
                val file = File(context.filesDir, "avatars/$name")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapPainter(it.asImageBitmap()) } else null
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            taskVm.loadNow()
            sharedHabitVm.loadNow()
        }
    }

    MainHostLifecycleCoordinator(
        activity = activity,
        selectedPage = hostState.selectedPage,
        onPageChanged = {
            taskVm.loadNow()
            sharedHabitVm.loadNow()
        },
        onResumed = {
            taskVm.loadNow()
            sharedHabitVm.loadNow()
            overviewState = KmpV6Runtime.overviewViewModel.loadNow()
        },
        onInitialExtra = {
            overviewState = KmpV6Runtime.overviewViewModel.loadNow()
        },
    )

    LaunchedEffect(hostState.selectedSection) {
        if (hostState.selectedSection == MainSection.OVERVIEW) {
            overviewState = KmpV6Runtime.overviewViewModel.loadNow()
        }
    }

    DeadlinerUrlIntake(
        activity = activity,
        snackbarHostState = snackbarHostState,
        pendingUrl = hostState.pendingUrl,
        onPendingUrlChange = { hostState.pendingUrl = it },
        onDecodedItemReady = { item ->
            val intent = Intent(context, AddDDLActivity::class.java).apply {
                putExtra("EXTRA_GENERATE_DDL", item)
            }
            activity.startActivity(intent)
        },
    )

    fun closeSearch() {
        textFieldState.edit { replace(0, length, "") }
        searchScope = MainSearchScope.ALL
        onSearchActiveChange(false)
    }

    fun openSearch() {
        hostState.selectedSection = MainSection.LIST
        onSearchActiveChange(true)
    }

    BackHandler(enabled = hostState.selectionMode || searchActive) {
        when {
            searchActive -> {
                closeSearch()
            }
            hostState.selectionMode -> {
                hostState.clearSelection()
                activeSelectionPane = null
            }
        }
    }

    val navItems = listOf(
        AdaptiveNavItem(
            "list",
            context.getString(R.string.main_section_list),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_home_fill_wght400),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_home_wght400),
            immersiveIcon = rememberVectorPainter(MiuixIcons.Home),
        ),
        AdaptiveNavItem(
            "overview",
            context.getString(R.string.overview),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_pie_chart_fill_wght400),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_pie_chart_wght400),
            immersiveIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_chart_pie_fill),
            immersiveIconScale = 0.86f,
        ),
        AdaptiveNavItem(
            "capture",
            context.getString(R.string.capture_title),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_capture_400),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_capture_400),
            immersiveIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_capture),
            immersiveIconScale = 1.02f,
        ),
        AdaptiveNavItem(
            "lifi",
            context.getString(R.string.lifi),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_lifi),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_lifi),
            immersiveIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.ic_lifi),
            immersiveIconScale = 0.945f,
        ),
        AdaptiveNavItem(
            "search",
            context.getString(R.string.search),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_search_wght400),
            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_search_wght400),
            immersiveIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_search_wght400),
            immersiveIconResource = R.drawable.msr_search_wght400,
            immersiveIconScale = 1.18f,
        ),
    )

    val isShortBottomNavigation = !currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(androidx.window.core.layout.WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isImmersiveCompactNavigation = appearance.usesMiuixTheme &&
        isShortBottomNavigation
    val navigationOverlaysContent = isShortBottomNavigation &&
        (isImmersiveCompactNavigation || LocalAdvancedMaterialSpec.current.enabled)
    val showImmersiveListSearchEntry = shouldShowImmersiveListSearchEntry(
        usesMiuixTheme = appearance.usesMiuixTheme,
        isCompactFloatingNavigation = isShortBottomNavigation,
        selectedSection = hostState.selectedSection,
        selectedPage = hostState.selectedPage,
        selectionMode = hostState.selectionMode,
    )
    val searchSlotHeightPx = with(LocalDensity.current) {
        ModernListSearchSlotHeight.toPx()
    }
    val listSearchCollapseState = rememberModernListSearchCollapseState(searchSlotHeightPx)
    val listChromeScrollConnection = rememberModernListChromeScrollConnection(
        searchState = listSearchCollapseState,
        topBarScrollBehavior = listTopBarScrollBehavior,
        chromeEnabled = canCollapseListTopBar,
        searchEnabled = showImmersiveListSearchEntry && !searchActive,
    )
    LaunchedEffect(showImmersiveListSearchEntry) {
        if (!showImmersiveListSearchEntry) {
            listSearchCollapseState.snapToExpanded()
        }
    }
    val quickAdd = com.aritxonly.deadliner.ui.quickadd.rememberQuickAddState()
    val quickAddRoute = listOf(hostState.selectedSection, hostState.selectedPage, searchActive, hostState.selectionMode)
    var previousQuickAddRoute by remember { mutableStateOf(quickAddRoute) }
    LaunchedEffect(quickAddRoute) {
        if (previousQuickAddRoute != quickAddRoute) quickAdd.dismiss()
        previousQuickAddRoute = quickAddRoute
    }
    val quickAddAnchor = Modifier.quickAddAnchor(quickAdd)
    val homeFabColors = LocalHomeFabColors.current
    val onAddDeadline = {
        if (quickAdd.visible) {
            quickAdd.dismiss()
        } else if (GlobalUtils.quickAddEnabled) {
            quickAdd.open(if (hostState.selectedPage == DeadlineType.TASK) "TASK" else "HABIT")
        } else {
            val intent = Intent(context, AddDDLActivity::class.java).apply {
                putExtra("EXTRA_CURRENT_TYPE", if (hostState.selectedPage == DeadlineType.TASK) 0 else 1)
            }
            launcher.launch(intent)
        }
    }
    val immersiveDetachedAction = if (isImmersiveCompactNavigation) {
        MiuixFloatingNavigationAction(
            iconResource = R.drawable.msr_add_2_wght400,
            label = if (quickAdd.visible) "关闭快速添加" else context.getString(R.string.add_task),
            containerColor = homeFabColors.container,
            contentColor = if (quickAdd.visible) MaterialTheme.colorScheme.onSurface else homeFabColors.content,
            neutralTint = quickAdd.visible,
            iconRotation = if (quickAdd.visible) 45f else 0f,
            iconScale = 1.05f,
            onClick = onAddDeadline,
            modifier = quickAddAnchor,
            symbolName = "add_2",
            enabled = !quickAdd.visible || quickAdd.canClose,
        )
    } else {
        null
    }
    val selectedKey = modernNavigationSelectedKey(
        section = hostState.selectedSection,
        searchActive = searchActive,
    )
    val topBarState = when {
        searchActive -> ModernTopBarState.HIDDEN
        hostState.selectedSection == MainSection.LIST && hostState.selectionMode ->
            ModernTopBarState.LIST_SELECTION
        hostState.selectedSection == MainSection.LIST -> ModernTopBarState.LIST_DEFAULT
        hostState.selectedSection == MainSection.OVERVIEW -> ModernTopBarState.OVERVIEW
        hostState.selectedSection == MainSection.LIFI -> ModernTopBarState.HIDDEN
        else -> ModernTopBarState.CAPTURE
    }
    val topBarMaterialScrollKey = when (hostState.selectedSection) {
        MainSection.LIST -> hostState.selectedPage
        MainSection.OVERVIEW -> overviewSelectedTab
        MainSection.CAPTURE,
        MainSection.LIFI -> Unit
    }
    val useOverlayTopBar = when (hostState.selectedSection) {
        MainSection.LIST -> !searchActive
        MainSection.OVERVIEW -> !useFlattenedOverview
        MainSection.CAPTURE -> true
        MainSection.LIFI -> false
    }
    // AnimatedContent keeps the list composed during its exit. Preserve its top inset so that
    // hiding the overlay top bar does not re-layout the list behind the expanding search surface.
    val preserveListTopBarGeometry = hostState.selectedSection == MainSection.LIST && searchActive
    val overlayTopBarPadding = if (useOverlayTopBar || preserveListTopBarGeometry) {
        with(LocalDensity.current) { overlayTopBarHeightPx.toDp() }
    } else {
        0.dp
    }
    val forceMaterialTopBarsInMiuix = appearance.usesMiuixTheme && appearance.useMaterialTopAppBarInMiuix

    val taskState by taskVm.uiState.collectAsStateWithLifecycle()
    val categories = rememberActiveCategories(taskState)
    val availableCategoryUids = remember(categories) { categories.mapTo(mutableSetOf()) { it.uid } }
    LaunchedEffect(availableCategoryUids) {
        categoryFilter = CategoryFilterResolver.reconcile(categoryFilter, availableCategoryUids)
    }
    val displayedTasks = taskVm.getFilteredTasks().filter {
        it.state != com.deadliner.domain.task.TaskState.ARCHIVED &&
            it.state != com.deadliner.domain.task.TaskState.ABANDONED_ARCHIVED &&
            CategoryFilterResolver.matches(categoryFilter, it.categoryUid, availableCategoryUids)
    }
    val sharedHabitState by sharedHabitVm.uiState.collectAsStateWithLifecycle()
    val listSemanticMaterialProgress = if (hostState.selectedSection == MainSection.LIST) {
        resolveTopBarButtonMaterialProgress(
            collapsedFraction = listTopBarScrollBehavior.state.collapsedFraction,
            scrollProgress = null,
            overrideProgress = null,
        )
    } else {
        0f
    }
    val selectionActions = rememberMainSelectionActionController(
        activity = activity,
        taskViewModel = taskVm,
        habitViewModel = sharedHabitVm,
        hostState = hostState,
    )

    ProvideAdvancedMaterialDialogBlurHost {
        ModernListSharedTransitionLayout(modifier = Modifier.fillMaxSize()) { sharedTransitionScope ->
        AdaptiveNavigationSuiteScaffold(
            modifier = modifier.fillMaxSize(),
        items = navItems,
        selectedKey = selectedKey,
        topBarMaterialScrollKey = topBarMaterialScrollKey,
        immersiveDetachedItemKey = "search",
        immersiveDetachedAction = immersiveDetachedAction,
        overlayContent = { com.aritxonly.deadliner.ui.quickadd.QuickAddOverlay(quickAdd, onSaved = ::celebrate,
            onExpand = { launcher.launch(it) }) },
        onItemSelected = {
            when (it.key) {
                "search" -> {
                    openSearch()
                }
                else -> {
                    mainSectionForNavigationKey(it.key)?.let { section ->
                        closeSearch()
                        hostState.selectedSection = section
                    }
                }
            }
            hostState.clearSelection()
            activeSelectionPane = null
        },
        background = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                val semanticMaterialProgress = semanticAtmosphereMaterialProgress(
                    section = hostState.selectedSection,
                    listProgress = listSemanticMaterialProgress,
                    observedScrollProgress = LocalTopBarScrollProgress.current,
                )
                if (
                    appearance.semanticGlowEnabled &&
                    shouldShowSemanticAtmosphere(hostState.selectedSection)
                ) {
                    ModernSemanticAtmosphere(
                        tasks = displayedTasks,
                        visibleHabits = sharedHabitState.visibleHabits,
                        categoryFilter = categoryFilter,
                        availableCategoryUids = availableCategoryUids,
                        selectedSection = hostState.selectedSection,
                        selectedPage = hostState.selectedPage,
                        materialProgress = semanticMaterialProgress,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        },
        topBar = {
            Box(
                modifier = if (useOverlayTopBar) {
                    Modifier.onSizeChanged { overlayTopBarHeightPx = it.height }
                } else {
                    Modifier
                },
            ) {
                AnimatedContent(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                    targetState = topBarState,
                    transitionSpec = {
                        val enter =
                            fadeIn(animationSpec = tween(180, delayMillis = 20)) +
                                slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight / 6 },
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                )
                        val exit =
                            fadeOut(animationSpec = tween(120)) +
                                slideOutVertically(
                                    targetOffsetY = { fullHeight -> -fullHeight / 10 },
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                )

                        enter.togetherWith(exit).using(SizeTransform(clip = false))
                    },
                    label = "modern-topbar-transition",
                ) { currentTopBarState ->
                    when (currentTopBarState) {
                        ModernTopBarState.HIDDEN -> {
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                        ModernTopBarState.LIST_DEFAULT -> {
                            val searchContainerModifier = modernListSearchSharedBoundsModifier(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = this@AnimatedContent,
                                enabled = isImmersiveCompactNavigation &&
                                    showImmersiveListSearchEntry,
                            )
                            ModernListTopAppBar(
                                activity = activity,
                                selectedPage = hostState.selectedPage,
                                onSelectedPageChange = { hostState.selectedPage = it },
                                avatarPainter = avatarPainter,
                                showPageTabs = !isWideLayout,
                                scrollBehavior = listTopBarScrollBehavior,
                                transparentBackground = true,
                                searchExpandedFraction = listSearchCollapseState.expandedFraction,
                                searchContent = if (showImmersiveListSearchEntry) {
                                    {
                                        ImmersiveListSearchEntry(
                                            onOpenSearch = { openSearch() },
                                            modifier = searchContainerModifier,
                                        )
                                    }
                                } else {
                                    null
                                },
                                onCategoryFilterClick = { showCategoryFilter = true },
                            )
                        }
                        ModernTopBarState.LIST_SELECTION -> {
                            TopAppBar(
                                title = context.getString(R.string.selected_items, hostState.selectedIds.size),
                                mode = TopAppBarStyle.SMALL,
                                titleTextStyle = MaterialTheme.typography.titleLarge,
                                forceMaterial3 = forceMaterialTopBarsInMiuix,
                                navigationIcon = {
                                    TopBarIconButton(onClick = {
                                        hostState.clearSelection()
                                        activeSelectionPane = null
                                    }) {
                                        Icon(
                                            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_close_wght400),
                                            contentDescription = stringResource(R.string.close),
                                        )
                                    }
                                },
                                actions = {
                                    TopBarIconButton(onClick = {
                                        selectionActions.onDoneClick(
                                            onCelebrate = {
                                                if (com.aritxonly.deadliner.localutils.GlobalUtils.fireworksOnFinish) {
                                                    celebrate()
                                                }
                                            },
                                        )
                                    }) {
                                        Icon(
                                            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_task_alt_wght400),
                                            contentDescription = stringResource(R.string.accept)
                                        )
                                    }
                                    if (hostState.selectedPage == DeadlineType.TASK) {
                                        TopBarIconButton(onClick = { selectionActions.onArchiveClick() }) {
                                            Icon(
                                                com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_move_to_inbox_wght400),
                                                contentDescription = stringResource(R.string.archive)
                                            )
                                        }
                                    }
                                    TopBarIconButton(onClick = { selectionActions.onDeleteClick() }) {
                                        Icon(
                                            com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_delete_wght400),
                                            contentDescription = stringResource(R.string.delete)
                                        )
                                    }
                                },
                            )
                        }
                        ModernTopBarState.OVERVIEW -> {
                            if (useFlattenedOverview) {
                                OverviewTopBar(
                                    showNavigationIcon = false,
                                    onShowSettings = { showOverviewSettings = true },
                                    mode = TopAppBarStyle.LARGE,
                                    scrollBehavior = overviewTopBarScrollBehavior,
                                    forceMaterial3 = forceMaterialTopBarsInMiuix,
                                )
                            } else {
                                OverviewTopBarWithTabs(
                                    selectedTab = overviewSelectedTab,
                                    onTabSelected = { overviewSelectedTab = it },
                                    showNavigationIcon = false,
                                    onShowSettings = { showOverviewSettings = true },
                                    mode = TopAppBarStyle.LARGE,
                                    scrollBehavior = overviewTopBarScrollBehavior,
                                    forceMaterial3 = forceMaterialTopBarsInMiuix,
                                )
                            }
                        }
                        ModernTopBarState.CAPTURE -> {
                            CaptureTopBar(
                                vm = captureVm,
                                onClose = { hostState.selectedSection = MainSection.LIST },
                                showNavigationIcon = false,
                                onRequestMerge = { showCaptureMergeSheet = true },
                                mode = TopAppBarStyle.LARGE,
                                scrollBehavior = captureTopBarScrollBehavior,
                                forceMaterial3 = forceMaterialTopBarsInMiuix,
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val shouldShowFab =
                hostState.selectedSection == MainSection.LIST &&
                    !hostState.selectionMode &&
                    !searchActive &&
                    !isImmersiveCompactNavigation

            if (shouldShowFab) {
                com.aritxonly.deadliner.ui.quickadd.QuickAddToggleButton(
                    expanded = quickAdd.visible, onClick = onAddDeadline, modifier = quickAddAnchor,
                    enabled = !quickAdd.visible || quickAdd.canClose,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {

            when (hostState.selectedSection) {
                MainSection.LIST -> {
                    ModernListSearchTransition(
                        searchActive = searchActive,
                        useSharedContainerTransform = isImmersiveCompactNavigation,
                        sharedTransitionScope = sharedTransitionScope,
                        listContent = {
                        if (isWideLayout) {
                            val taskPaneBlocked = hostState.selectionMode && activeSelectionPane == DeadlineType.HABIT
                            val habitPaneBlocked = hostState.selectionMode && activeSelectionPane == DeadlineType.TASK
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MainDisplay(
                                        tasks = displayedTasks,
                                        habitViewModel = sharedHabitVm,
                                        isRefreshing = taskState.isLoading || sharedHabitState.isLoading,
                                        selectedPage = DeadlineType.TASK,
                                        activity = activity,
                                        taskViewModel = taskVm,
                                        listState = taskListState,
                                        modifier = Modifier.fillMaxSize(),
                                        moreExpanded = false,
                                        moreAnchorRect = null,
                                        useAvatar = false,
                                        nickname = profile?.nickname.orEmpty(),
                                        avatarPainter = avatarPainter,
                                        onCloseMorePanel = {},
                                        selectionMode = hostState.selectionMode,
                                        isSelected = { id -> hostState.selectedIds.contains(id) },
                                        onItemLongPress = { id ->
                                            if (!taskPaneBlocked && (activeSelectionPane == null || activeSelectionPane == DeadlineType.TASK)) {
                                                activeSelectionPane = DeadlineType.TASK
                                                hostState.selectedPage = DeadlineType.TASK
                                                hostState.enterSelection(id)
                                            }
                                        },
                                        onItemClickInSelection = { id ->
                                            if (!taskPaneBlocked && activeSelectionPane == DeadlineType.TASK) {
                                                hostState.toggleSelection(id)
                                            }
                                        },
                                        onRequestBackdropBlur = { enable ->
                                            uiState.childRequestsBlur = enable
                                        },
                                        onCelebrate = { celebrate() },
                                        topOverlayPadding = overlayTopBarPadding,
                                        listNestedScrollConnection = listChromeScrollConnection,
                                        categories = categories,
                                        categoryFilter = categoryFilter,
                                    )
                                    if (taskPaneBlocked) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    MainDisplay(
                                        tasks = displayedTasks,
                                        habitViewModel = sharedHabitVm,
                                        isRefreshing = taskState.isLoading || sharedHabitState.isLoading,
                                        selectedPage = DeadlineType.HABIT,
                                        activity = activity,
                                        taskViewModel = taskVm,
                                        listState = habitListState,
                                        modifier = Modifier.fillMaxSize(),
                                        moreExpanded = false,
                                        moreAnchorRect = null,
                                        useAvatar = false,
                                        nickname = profile?.nickname.orEmpty(),
                                        avatarPainter = avatarPainter,
                                        onCloseMorePanel = {},
                                        selectionMode = hostState.selectionMode,
                                        isSelected = { id -> hostState.selectedIds.contains(id) },
                                        onItemLongPress = { id ->
                                            if (!habitPaneBlocked && (activeSelectionPane == null || activeSelectionPane == DeadlineType.HABIT)) {
                                                activeSelectionPane = DeadlineType.HABIT
                                                hostState.selectedPage = DeadlineType.HABIT
                                                hostState.enterSelection(id)
                                            }
                                        },
                                        onItemClickInSelection = { id ->
                                            if (!habitPaneBlocked && activeSelectionPane == DeadlineType.HABIT) {
                                                hostState.toggleSelection(id)
                                            }
                                        },
                                        onRequestBackdropBlur = { enable ->
                                            uiState.childRequestsBlur = enable
                                        },
                                        onCelebrate = { celebrate() },
                                        topOverlayPadding = overlayTopBarPadding,
                                        categories = categories,
                                        categoryFilter = categoryFilter,
                                    )
                                    if (habitPaneBlocked) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        )
                                    }
                                }
                            }
                        } else {
                            MainDisplay(
                                tasks = displayedTasks,
                                habitViewModel = sharedHabitVm,
                                isRefreshing = taskState.isLoading || sharedHabitState.isLoading,
                                selectedPage = hostState.selectedPage,
                                activity = activity,
                                taskViewModel = taskVm,
                                listState = if (hostState.selectedPage == DeadlineType.TASK) {
                                    taskListState
                                } else {
                                    habitListState
                                },
                                moreExpanded = false,
                                moreAnchorRect = null,
                                useAvatar = avatarPainter != null,
                                nickname = profile?.nickname.orEmpty(),
                                avatarPainter = avatarPainter,
                                onCloseMorePanel = {},
                                selectionMode = hostState.selectionMode,
                                isSelected = { id -> hostState.selectedIds.contains(id) },
                                onItemLongPress = { id -> hostState.enterSelection(id) },
                                onItemClickInSelection = { id -> hostState.toggleSelection(id) },
                                onRequestBackdropBlur = { enable ->
                                    uiState.childRequestsBlur = enable
                                },
                                onCelebrate = { celebrate() },
                                topOverlayPadding = overlayTopBarPadding,
                                listNestedScrollConnection = listChromeScrollConnection,
                                categories = categories,
                                categoryFilter = categoryFilter,
                            )
                        }
                        },
                        searchContent = { searchContainerModifier ->
                            MainSearchBar(
                                textFieldState = textFieldState,
                                onQueryChanged = { query ->
                                    textFieldState.edit { replace(0, length, query) }
                                },
                                activity = activity,
                                expanded = true,
                                onExpandedChangeExternal = { expanded ->
                                    if (!expanded) closeSearch()
                                },
                                onArchiveClick = {
                                    activity.startActivity(Intent(activity, ArchiveActivity::class.java))
                                },
                                useSurfaceColors = true,
                                miuixMode = false,
                                modifier = searchContainerModifier,
                            ) {
                                MainSearchResultsContent(
                                    query = searchQuery,
                                    scope = searchScope,
                                    onScopeChange = { searchScope = it },
                                    activity = activity,
                                    inspirations = captureState.items,
                                )
                            }
                        },
                    )
                }
                MainSection.OVERVIEW -> {
                    Box(modifier = Modifier.nestedScroll(overviewTopBarScrollBehavior.nestedScrollConnection)) {
                        OverviewContent(
                            overviewState = overviewState,
                            activity = activity,
                            flattenedLayout = useFlattenedOverview,
                            selectedTab = overviewSelectedTab,
                            onSelectedTabChange = { overviewSelectedTab = it },
                            showTabsInContent = useFlattenedOverview,
                            topContentPadding = if (!useFlattenedOverview) overlayTopBarPadding else 0.dp,
                            transparentBackground = true,
                        )
                    }
                }
                MainSection.CAPTURE -> {
                    Box(modifier = Modifier.nestedScroll(captureTopBarScrollBehavior.nestedScrollConnection)) {
                        CaptureContent(
                            vm = captureVm,
                            twoColumnLayout = isWideLayout,
                            showMergeSheet = showCaptureMergeSheet,
                            onShowMergeSheetChange = { showCaptureMergeSheet = it },
                            topOverlayPadding = overlayTopBarPadding,
                            bottomLiftPadding = when {
                                isImmersiveCompactNavigation -> 96.dp
                                isShortBottomNavigation -> 96.dp
                                else -> 28.dp
                            },
                        )
                    }
                }
                MainSection.LIFI -> {
                    AIAgentPage(
                        onAddDDL = { launcher.launch(it) },
                        inputBottomPadding = AIAgentPageDefaults.inputBottomPadding(
                            navigationOverlaysContent = navigationOverlaysContent,
                        ),
                    )
                }
            }
            }

        key(fireKey) {
            Box(Modifier.fillMaxSize()) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = parties
                )
            }
        }
        }
    }

    OverviewSettingsDialog(
        visible = hostState.selectedSection == MainSection.OVERVIEW && showOverviewSettings,
        onDismiss = { showOverviewSettings = false },
    )
    if (showCategoryFilter) {
        CategoryFilterSheet(
            categories = categories,
            filter = categoryFilter,
            onFilterChange = { categoryFilter = it },
            onManageCategories = {
                activity.startActivity(com.aritxonly.deadliner.CategoryManagementActivity.newIntent(activity))
            },
            onDismiss = { showCategoryFilter = false },
        )
    }
    }
    }

    if (selectionActions.showDeleteDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { selectionActions.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            miuixTitle = stringResource(R.string.alert_delete_title),
            miuixSummary = stringResource(R.string.alert_delete_message),
            dismissButton = {
                TextButton(onClick = { selectionActions.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { selectionActions.confirmDeleteSelected() }) {
                    Text(stringResource(R.string.accept))
                }
            },
        )
    }

}
