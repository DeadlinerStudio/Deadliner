package com.aritxonly.deadliner.ui.main.modern

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
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
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.capture.ui.CaptureContent
import com.aritxonly.deadliner.capture.ui.CaptureTopBar
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.agent.AIOverlayHost
import com.aritxonly.deadliner.ui.base.AdaptiveNavItem
import com.aritxonly.deadliner.ui.base.AdaptiveNavigationSuiteScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.FloatingActionButton
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.advancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.main.shared.MainSection
import com.aritxonly.deadliner.ui.main.modern.components.ModernMainHeader
import com.aritxonly.deadliner.ui.main.shared.DeadlinerUrlIntake
import com.aritxonly.deadliner.ui.main.shared.MainDisplay
import com.aritxonly.deadliner.ui.main.shared.MainHostLifecycleCoordinator
import com.aritxonly.deadliner.ui.main.shared.MainSearchResultsContent
import com.aritxonly.deadliner.ui.main.shared.MainSearchScope
import com.aritxonly.deadliner.ui.main.shared.rememberMainHostUiState
import com.aritxonly.deadliner.ui.main.shared.rememberMainSelectionActionController
import com.aritxonly.deadliner.ui.main.shared.rememberMainHostState
import com.aritxonly.deadliner.ui.main.simplified.MainSearchBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import java.io.File

private fun LazyListState.hasScrolledPastHeader(threshold: Int = 12): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > threshold
}

private enum class ModernTopBarState {
    HIDDEN,
    LIST_DEFAULT,
    LIST_SELECTION,
    OVERVIEW,
    CAPTURE,
}

private const val WideLayoutMinWidthDp = 840
private const val FlattenedOverviewMinWidthDp = 1100

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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
    val scope = rememberCoroutineScope()

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

    val taskListState = rememberLazyListState()
    val habitListState = rememberLazyListState()
    val textFieldState = rememberTextFieldState()
    val captureState by captureVm.uiState.collectAsStateWithLifecycle()
    val searchQuery = textFieldState.text.toString()
    var overlayTopBarHeightPx by remember { mutableIntStateOf(0) }

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

    BackHandler(enabled = hostState.selectionMode || searchActive || hostState.showOverlay) {
        when {
            hostState.showOverlay -> hostState.showOverlay = false
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
            ImageVector.vectorResource(R.drawable.ic_check_circle_fill),
            ImageVector.vectorResource(R.drawable.ic_check_circle),
        ),
        AdaptiveNavItem(
            "overview",
            context.getString(R.string.overview),
            ImageVector.vectorResource(R.drawable.ic_overview_fill),
            ImageVector.vectorResource(R.drawable.ic_overview),
        ),
        AdaptiveNavItem(
            "capture",
            context.getString(R.string.capture_title),
            ImageVector.vectorResource(R.drawable.ic_draw_fill),
            ImageVector.vectorResource(R.drawable.ic_draw),
        ),
        AdaptiveNavItem(
            "search",
            context.getString(R.string.search),
            ImageVector.vectorResource(R.drawable.ic_search),
            ImageVector.vectorResource(R.drawable.ic_search),
        ),
    )

    val selectedKey = if (searchActive) {
        "search"
    } else when (hostState.selectedSection) {
        MainSection.LIST -> "list"
        MainSection.OVERVIEW -> "overview"
        MainSection.CAPTURE -> "capture"
    }
    val topBarState = when {
        searchActive -> ModernTopBarState.HIDDEN
        hostState.selectedSection == MainSection.LIST && hostState.selectionMode ->
            ModernTopBarState.LIST_SELECTION
        hostState.selectedSection == MainSection.LIST -> ModernTopBarState.LIST_DEFAULT
        hostState.selectedSection == MainSection.OVERVIEW -> ModernTopBarState.OVERVIEW
        else -> ModernTopBarState.CAPTURE
    }
    val useOverlayTopBar = when (hostState.selectedSection) {
        MainSection.LIST -> !searchActive
        MainSection.OVERVIEW -> !useFlattenedOverview
        MainSection.CAPTURE -> true
    }
    val overlayTopBarPadding = if (useOverlayTopBar) {
        with(LocalDensity.current) { overlayTopBarHeightPx.toDp() }
    } else {
        0.dp
    }
    val shouldCollapseHeaderAccessories by remember(
        isWideLayout,
        hostState.selectedSection,
        hostState.selectedPage,
        hostState.selectionMode,
        searchActive,
        taskListState,
        habitListState,
    ) {
        androidx.compose.runtime.derivedStateOf {
            if (
                hostState.selectedSection != MainSection.LIST ||
                hostState.selectionMode ||
                searchActive
            ) {
                false
            } else if (isWideLayout) {
                taskListState.hasScrolledPastHeader() || habitListState.hasScrolledPastHeader()
            } else {
                when (hostState.selectedPage) {
                    DeadlineType.TASK -> taskListState.hasScrolledPastHeader()
                    DeadlineType.HABIT -> habitListState.hasScrolledPastHeader()
                }
            }
        }
    }
    val forceMaterialTopBarsInMiuix = appearance.usesMiuixTheme && appearance.useMaterialTopAppBarInMiuix

    val taskState by taskVm.uiState.collectAsStateWithLifecycle()
    val displayedTasks = taskVm.getFilteredTasks().filter {
        it.state != com.deadliner.domain.task.TaskState.ARCHIVED &&
            it.state != com.deadliner.domain.task.TaskState.ABANDONED_ARCHIVED
    }
    val sharedHabitState by sharedHabitVm.uiState.collectAsStateWithLifecycle()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val isShortBottomNavigation = navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium
    val isFloatingMiuixBottomNavigation = appearance.usesMiuixTheme &&
        appearance.useMiuixFloatingTabBar &&
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact
    val selectionActions = rememberMainSelectionActionController(
        activity = activity,
        taskViewModel = taskVm,
        habitViewModel = sharedHabitVm,
        hostState = hostState,
    )

    val shouldBlur = hostState.showOverlay ||
        uiState.childRequestsBlur ||
        selectionActions.showDeleteDialog ||
        (hostState.selectedSection == MainSection.OVERVIEW && showOverviewSettings) ||
        (hostState.selectedSection == MainSection.CAPTURE && showCaptureMergeSheet)
    val advancedMaterialBlur = uiState.childRequestsBlur ||
        selectionActions.showDeleteDialog ||
        (hostState.selectedSection == MainSection.OVERVIEW && showOverviewSettings) ||
        (hostState.selectedSection == MainSection.CAPTURE && showCaptureMergeSheet)
    ProvideAdvancedMaterialDialogBlurHost {
        AdaptiveNavigationSuiteScaffold(
            modifier = modifier
                .fillMaxSize()
                .advancedMaterialDialogBlurHost(
                    active = advancedMaterialBlur,
                    forceActive = hostState.showOverlay,
                    activeScale = if (hostState.showOverlay) 0.98f else 1f,
                ),
        items = navItems,
        selectedKey = selectedKey,
        useMiuixFloatingTabBar = appearance.useMiuixFloatingTabBar,
        onItemSelected = {
            when (it.key) {
                "search" -> {
                    openSearch()
                }
                "overview" -> {
                    closeSearch()
                    hostState.selectedSection = MainSection.OVERVIEW
                }
                "capture" -> {
                    closeSearch()
                    hostState.selectedSection = MainSection.CAPTURE
                }
                else -> {
                    closeSearch()
                    hostState.selectedSection = MainSection.LIST
                }
            }
            hostState.clearSelection()
            activeSelectionPane = null
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
                            ModernMainHeader(
                                activity = activity,
                                selectedPage = hostState.selectedPage,
                                onSelectedPageChange = { hostState.selectedPage = it },
                                avatarPainter = avatarPainter,
                                onShowAiOverlay = { hostState.showOverlay = true },
                                showPageTabs = !isWideLayout,
                                showAccessoryRow = !isWideLayout && !shouldCollapseHeaderAccessories,
                                showAiActionInTopBar = isWideLayout,
                                forceMaterialTopAppBar = forceMaterialTopBarsInMiuix,
                            )
                        }
                        ModernTopBarState.LIST_SELECTION -> {
                            TopAppBar(
                                title = context.getString(R.string.selected_items, hostState.selectedIds.size),
                                mode = TopAppBarStyle.SMALL,
                                titleTextStyle = MaterialTheme.typography.titleLarge,
                                forceMaterial3 = forceMaterialTopBarsInMiuix,
                                navigationIcon = {
                                    IconButton(onClick = {
                                        hostState.clearSelection()
                                        activeSelectionPane = null
                                    }) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.ic_close),
                                            contentDescription = stringResource(R.string.close),
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        selectionActions.onDoneClick(
                                            onCelebrate = {
                                                if (com.aritxonly.deadliner.localutils.GlobalUtils.fireworksOnFinish) {
                                                    celebrate()
                                                }
                                            },
                                        )
                                    }) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.ic_done),
                                            contentDescription = stringResource(R.string.accept)
                                        )
                                    }
                                    if (hostState.selectedPage == DeadlineType.TASK) {
                                        IconButton(onClick = { selectionActions.onArchiveClick() }) {
                                            Icon(
                                                ImageVector.vectorResource(R.drawable.ic_archiving),
                                                contentDescription = stringResource(R.string.archive)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { selectionActions.onDeleteClick() }) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.ic_delete),
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
                                    mode = TopAppBarStyle.SMALL,
                                    forceMaterial3 = forceMaterialTopBarsInMiuix,
                                )
                            } else {
                                OverviewTopBarWithTabs(
                                    selectedTab = overviewSelectedTab,
                                    onTabSelected = { overviewSelectedTab = it },
                                    showNavigationIcon = false,
                                    onShowSettings = { showOverviewSettings = true },
                                    mode = TopAppBarStyle.SMALL,
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
                                forceMaterial3 = forceMaterialTopBarsInMiuix,
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val shouldShowFab =
                hostState.selectedSection == MainSection.LIST && !hostState.selectionMode && !searchActive

                if (shouldShowFab) {
                    val onFabClick = {
                        val intent = Intent(context, AddDDLActivity::class.java).apply {
                            putExtra("EXTRA_CURRENT_TYPE", if (hostState.selectedPage == DeadlineType.TASK) 0 else 1)
                        }
                        launcher.launch(intent)
                    }
                    FloatingActionButton(
                        onClick = onFabClick,
                        forceMaterial3 = true,
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add_task)
                        )
                    }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {

            when (hostState.selectedSection) {
                MainSection.LIST -> {
                    if (!searchActive) {
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
                            )
                        }
                    }
                }
                MainSection.OVERVIEW -> {
                    OverviewContent(
                        overviewState = overviewState,
                        activity = activity,
                        flattenedLayout = useFlattenedOverview,
                        selectedTab = overviewSelectedTab,
                        onSelectedTabChange = { overviewSelectedTab = it },
                        showTabsInContent = useFlattenedOverview,
                        topContentPadding = if (!useFlattenedOverview) overlayTopBarPadding else 0.dp,
                    )
                }
                MainSection.CAPTURE -> {
                    CaptureContent(
                        vm = captureVm,
                        twoColumnLayout = isWideLayout,
                        showMergeSheet = showCaptureMergeSheet,
                        onShowMergeSheetChange = { showCaptureMergeSheet = it },
                        topOverlayPadding = overlayTopBarPadding,
                        bottomLiftPadding = when {
                            isFloatingMiuixBottomNavigation -> 96.dp
                            isShortBottomNavigation -> 96.dp
                            else -> 28.dp
                        },
                    )
                }
            }
            }

        AnimatedVisibility(
            visible = searchActive,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                expandVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                shrinkVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            label = "modern-searchbar-visibility",
        ) {
            MainSearchBar(
                textFieldState = textFieldState,
                onQueryChanged = { q ->
                    textFieldState.edit { replace(0, length, q) }
                },
                activity = activity,
                expanded = true,
                onExpandedChangeExternal = { expanded ->
                    if (!expanded) {
                        closeSearch()
                    }
                },
                useSurfaceColors = true,
                miuixMode = false,
            ) {
                MainSearchResultsContent(
                    query = searchQuery,
                    scope = searchScope,
                    onScopeChange = { searchScope = it },
                    activity = activity,
                    inspirations = captureState.items,
                )
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
    }

    if (selectionActions.showDeleteDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { selectionActions.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
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

    if (hostState.showOverlay) {
        AIOverlayHost(
            initialText = "",
            onAddDDL = { launcher.launch(it) },
            onRemoveFromWindow = { hostState.showOverlay = false },
            respondIme = true,
        )
    }
}
