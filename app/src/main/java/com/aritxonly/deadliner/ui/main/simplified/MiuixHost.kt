package com.aritxonly.deadliner.ui.main.simplified

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberFloatingToolbarState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.EditDDLFragment
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.data.HabitViewModelFactory
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.data.MainViewModelFactory
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.showHabitReminderDialog
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.model.UserProfile
import com.aritxonly.deadliner.ui.agent.AIOverlayHost
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.main.TextPageIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.basic.TopAppBar
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiuixHost(
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    // ---------------------------------------------------------
    // 1. 业务逻辑状态 (100% 复制自 SimplifiedHost)
    // ---------------------------------------------------------
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(context))
    val habitVm: HabitViewModel = viewModel(factory = HabitViewModelFactory(context))
    val clipboardManager = remember {
        context.getSystemService(android.content.ClipboardManager::class.java)
    }
    val view = LocalView.current

    val snackbarHostState = remember { SnackbarHostState() }

    var pendingUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var lastHandledUrl by rememberSaveable { mutableStateOf<String?>(null) }

    // URL 监听与消费逻辑
    LaunchedEffect(Unit) {
        consumeDeadlinerUrl(activity)?.let { url ->
            try {
                val item = DeadlinerURLScheme.decodeWithPassphrase(url, "deadliner-2025".toCharArray())
                val intent = Intent(context, AddDDLActivity::class.java).apply {
                    putExtra("EXTRA_FULL_DDL", item)
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.share_link_parse_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(pendingUrl) {
        val url = pendingUrl ?: return@LaunchedEffect
        if (url == lastHandledUrl) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.detect_share_link),
            actionLabel = context.getString(R.string.add),
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )

        if (result == SnackbarResult.ActionPerformed) {
            val item = DeadlinerURLScheme.decodeWithPassphrase(url, "deadliner-2025".toCharArray())
            val intent = Intent(context, AddDDLActivity::class.java).apply {
                putExtra("EXTRA_FULL_DDL", item)
            }
            activity.startActivity(intent)
        }
    }

    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) return@OnWindowFocusChangeListener
            val clipText = clipboardManager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
            if (clipText.isNotEmpty() && (clipText.startsWith(DeadlinerURLScheme.DEADLINER_URL_SCHEME_PREFIX) || clipText.startsWith(DeadlinerURLScheme.DEADLINER_URL_SCHEME_PREFIX_LEGACY))) {
                pendingUrl = clipText
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadData(vm.currentType)
                habitVm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            vm.loadData(vm.currentType)
            habitVm.refresh()
        }
    }

    LaunchedEffect(Unit) {
        GlobalUtils.decideHideFromRecent(context, activity)
        vm.loadData(vm.currentType)
        habitVm.refresh()
    }

    val ddlList by vm.ddlListFlow.collectAsStateWithLifecycle()
    val dueSoonCounts by vm.dueSoonCounts.observeAsState(emptyMap())
    val refreshState by vm.refreshState.collectAsStateWithLifecycle()

    // 工具栏滚动收起逻辑
    var toolbarExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        val closeThresholdPx = with(density) { 32.dp.toPx() }
        val openThresholdPx  = with(density) { 16.dp.toPx() }
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        var accumDown = 0f
        var accumUp   = 0f

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            val dyPx = (offset - lastOffset) + (index - lastIndex) * 10_000f
            lastIndex = index; lastOffset = offset
            if (index == 0 && offset == 0) {
                if (!toolbarExpanded) toolbarExpanded = true
                accumDown = 0f; accumUp = 0f
                return@collect
            }
            if (dyPx > 0f) {
                accumDown += dyPx; accumUp = 0f
                if (toolbarExpanded && accumDown >= closeThresholdPx) {
                    toolbarExpanded = false
                    accumDown = 0f
                }
            } else if (dyPx < 0f) {
                accumUp += -dyPx; accumDown = 0f
                if (!toolbarExpanded && accumUp >= openThresholdPx) {
                    toolbarExpanded = true
                    accumUp = 0f
                }
            }
        }
    }

    var selectedPage by remember { mutableStateOf(vm.currentType) }
    LaunchedEffect(selectedPage) {
        vm.loadData(selectedPage)
        habitVm.refresh()
    }

    // 对话框与浮层状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }
    var childRequestsBlur by remember { mutableStateOf(false) }
    var habitRemTargetId by remember { mutableStateOf<Long?>(null) }
    val shouldBlur = showOverlay || childRequestsBlur || moreExpanded || showDeleteDialog || (habitRemTargetId != null)

    LaunchedEffect(habitRemTargetId) {
        val targetId = habitRemTargetId ?: return@LaunchedEffect
        showHabitReminderDialog(context, targetId) { habitRemTargetId = null }
    }

    // 模糊与缩放效果 (保持与 M3 版一致)
    val scale by animateFloatAsState(targetValue = if (showOverlay) 0.98f else 1f, label = "content-scale")
    val maxBlur = 24f
    val blurProgress by animateFloatAsState(
        targetValue = if (shouldBlur) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "blur-progress"
    )
    val blurRadius = (maxBlur * blurProgress).coerceIn(0f, maxBlur)
    val saturation = lerp(1f, 0.5f, blurProgress)
    val EPS = 0.5f

    // 庆典与撒花
    val jumpAnim = remember { Animatable(40f) }
    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var fireKey by remember { mutableIntStateOf(0) }
    fun celebrate() {
        parties = PartyPresets.festive()
        fireKey++
    }
    LaunchedEffect(parties) {
        if (parties.isNotEmpty()) {
            delay(3500)
            parties = emptyList()
        }
    }

    // 搜索逻辑
    val textFieldState = rememberTextFieldState()
    var suggestions by rememberSaveable { mutableStateOf(emptyList<DDLItem>()) }
    var base by remember { mutableStateOf<List<DDLItem>>(emptyList()) }
    LaunchedEffect(selectedPage) { base = vm.getBaseList(selectedPage) }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .debounce(250)
            .collect { q ->
                val f = SearchFilter.parse(q)
                suggestions = if (q.isBlank()) emptyList()
                else base.filter { f.matches(it) }.toList()
            }
    }

    // 个人资料与头像
    val profile by UserProfileRepository.profile.collectAsState(initial = UserProfile())
    val avatarPainter: Painter? by remember(profile.avatarFileName) {
        mutableStateOf(
            profile.avatarFileName?.let { name ->
                val file = File(context.filesDir, "avatars/$name")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapPainter(it.asImageBitmap()) } else null
            }
        )
    }
    val useAvatar = avatarPainter != null
    var moreAnchorRect by remember { mutableStateOf<Rect?>(null) }

    // 多选逻辑
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    fun enterSelection(id: Long) { selectionMode = true; if (!selectedIds.contains(id)) selectedIds.add(id) }
    fun toggleSelection(id: Long) { if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id); if (selectedIds.isEmpty()) selectionMode = false }
    fun clearSelection() { selectedIds.clear(); selectionMode = false }

    BackHandler(enabled = selectionMode || searchActive) {
        if (searchActive) onSearchActiveChange(false) else clearSelection()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val effects = mutableListOf<RenderEffect>()
                if (blurRadius >= EPS) effects += RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP
                )
                if (saturation < 1f - 1e-3f) {
                    val cm = ColorMatrix().apply { setSaturation(saturation) }
                    effects += RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
                }
                renderEffect = when (effects.size) {
                    0 -> null
                    1 -> effects[0].asComposeRenderEffect()
                    else -> RenderEffect.createChainEffect(effects[0], effects[1])
                        .asComposeRenderEffect()
                }
                scaleX = scale; scaleY = scale
            },
        bottomBar = {
            top.yukonga.miuix.kmp.basic.NavigationBar {
                NavigationBarItem(
                    selected = selectedPage == DeadlineType.TASK,
                    onClick = {
                        selectedPage = DeadlineType.TASK
                        clearSelection()
                    },
                    icon = ImageVector.vectorResource(R.drawable.ic_task),
                    label = stringResource(R.string.task)
                )

                FloatingActionButton(
                    onClick = {
                        val intent = Intent(context, AddDDLActivity::class.java).apply {
                            putExtra("EXTRA_CURRENT_TYPE", if (selectedPage == DeadlineType.TASK) 0 else 1)
                        }
                        launcher.launch(intent)
                    },
                    modifier = Modifier
                        .detectSwipeUp { showOverlay = true }
                        .padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    minHeight = 48.dp
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                        contentDescription = "Add",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                NavigationBarItem(
                    selected = selectedPage == DeadlineType.HABIT,
                    onClick = {
                        selectedPage = DeadlineType.HABIT
                        clearSelection()
                    },
                    icon = ImageVector.vectorResource(R.drawable.ic_habit),
                    label = stringResource(R.string.habit)
                )
            }
        },
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
        floatingToolbar = {
            if (!searchActive && selectionMode) {
                AnimatedVisibility(
                    visible = toolbarExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    FloatingToolbar(
                        modifier = Modifier.padding(bottom = 64.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 多选模式：完成、归档/提醒、删除、编辑 (逻辑与原来完全一致)
                            IconButton(onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    GlobalUtils.triggerVibration(activity, 100)
                                    if (selectedPage == DeadlineType.HABIT) {
                                        val idsToUpdate = selectedIds.toList()
                                        val habitRepo = HabitRepository()
                                        val selectedDate = habitVm.selectedDate.value
                                        if (selectedDate.isAfter(LocalDate.now())) {
                                            Toast.makeText(context, R.string.cannot_check_future, Toast.LENGTH_SHORT).show()
                                        } else {
                                            idsToUpdate.forEach { id ->
                                                val item = ddlList.find { it.id == id } ?: return@forEach
                                                val habit = habitRepo.getHabitByDdlId(item.id) ?: return@forEach
                                                habitRepo.toggleRecord(habit.id, selectedDate)
                                            }
                                            vm.loadData(selectedPage); habitVm.refresh()
                                            selectedIds.clear(); if (GlobalUtils.fireworksOnFinish) celebrate()
                                            Toast.makeText(context, R.string.toast_finished, Toast.LENGTH_SHORT).show()
                                            selectionMode = false
                                        }
                                    } else {
                                        selectedIds.forEach { id ->
                                            val item = ddlList.find { it.id == id } ?: return@forEach
                                            DDLRepository().updateDDL(item.copy(isCompleted = true, completeTime = LocalDateTime.now().toString()))
                                        }
                                        vm.loadData(selectedPage); habitVm.refresh()
                                        Toast.makeText(context, R.string.toast_finished, Toast.LENGTH_SHORT).show()
                                        selectedIds.clear(); selectionMode = false
                                    }
                                }
                            }) { Icon(iconResource(R.drawable.ic_done), null) }

                            if (selectedPage == DeadlineType.TASK) {
                                IconButton(onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        var count = 0
                                        selectedIds.forEach { id ->
                                            val item = ddlList.firstOrNull { it.id == id } ?: return@forEach
                                            if (item.isCompleted) { DDLRepository().updateDDL(item.copy(isArchived = true)); count++ }
                                        }
                                        vm.loadData(selectedPage); habitVm.refresh()
                                        Toast.makeText(activity, activity.getString(R.string.toast_archived, count), Toast.LENGTH_SHORT).show()
                                        selectedIds.clear(); selectionMode = false
                                    }
                                }) { Icon(iconResource(R.drawable.ic_archiving), null) }
                            } else {
                                IconButton(onClick = {
                                    selectedIds.firstOrNull()?.let { habitRemTargetId = it }
                                }) { Icon(iconResource(R.drawable.ic_notification_add), null) }
                            }

                            IconButton(onClick = {
                                if (selectedIds.isNotEmpty()) { GlobalUtils.triggerVibration(activity, 200); showDeleteDialog = true }
                            }) { Icon(iconResource(R.drawable.ic_delete), null) }

                            IconButton(onClick = {
                                selectedIds.firstOrNull()?.let { firstId ->
                                    ddlList.firstOrNull { it.id == firstId }?.let { clickedItem ->
                                        EditDDLFragment(clickedItem) { updatedDDL ->
                                            DDLRepository().updateDDL(updatedDDL); vm.loadData(selectedPage); habitVm.refresh()
                                            selectedIds.clear(); selectionMode = false
                                        }.show(activity.supportFragmentManager, "EditDDLFragment")
                                    }
                                }
                            }) { Icon(iconResource(R.drawable.ic_edit), null) }
                        }
                    }
                }
            }
        },
        topBar = {
            AnimatedContent(targetState = selectionMode, label = "topbar-switch") { isSelection ->
                if (isSelection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { clearSelection() }) { Icon(ImageVector.vectorResource(R.drawable.ic_close), null) }
                        Text(text = stringResource(R.string.selected_items, selectedIds.size), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    Column {
                        TopAppBar(
                            title = "Deadliner",
                            scrollBehavior = MiuixScrollBehavior(),
                            actions = {
                                // 1. 获取位置的 Modifier 留给外层 Button
                                val buttonModifier = Modifier.onGloballyPositioned { moreAnchorRect = it.boundsInWindow() }

                                if (useAvatar && avatarPainter != null) {
                                    IconButton(
                                        onClick = { moreExpanded = true },
                                        modifier = buttonModifier.padding(end = 16.dp) // 按钮只负责外边距，不锁死大小
                                    ) {
                                        // 2. 把具体的尺寸和圆形裁切，直接加在 Image 上！
                                        Image(
                                            painter = avatarPainter!!,
                                            contentDescription = stringResource(R.string.user),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp) // 稍微大一点，36dp 在 TopBar 里更协调
                                                .clip(CircleShape)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { moreExpanded = true },
                                        modifier = buttonModifier.padding(end = 16.dp)
                                    ) {
                                        M3Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_more),
                                            contentDescription = stringResource(R.string.settings_more)
                                        )
                                    }
                                }
                            }
                        )

                        MainSearchBar(
                            textFieldState = textFieldState,
                            searchResults = suggestions,
                            onQueryChanged = { q ->
                                suggestions = if (q.isBlank()) emptyList() else base.filter {
                                    SearchFilter.parse(q).matches(it)
                                }.toList()
                            },
                            onMoreClick = { moreExpanded = true },
                            onMoreAnchorChange = { moreAnchorRect = it },
                            useAvatar = useAvatar,
                            avatarPainter = avatarPainter,
                            activity = activity,
                            expanded = searchActive,
                            onExpandedChangeExternal = onSearchActiveChange,
                            selectedPage = selectedPage,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                            miuixMode = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // 核心内容区
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)) {
            MainDisplay(
                ddlList = ddlList, habitViewModel = habitVm, refreshState = refreshState, selectedPage = selectedPage,
                activity = activity, vm = vm, listState = listState, onRequestBackdropBlur = { childRequestsBlur = it },
                onShowUndoSnackbar = { updatedHabit ->
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(context.getString(R.string.habit_success), context.getString(R.string.undo), true, SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) {
                            val todayStr = LocalDate.now().toString()
                            val json = JSONObject(updatedHabit.note)
                            val datesArray = json.optJSONArray("completedDates") ?: JSONArray()
                            for (i in datesArray.length() - 1 downTo 0) { if (datesArray.optString(i) == todayStr) datesArray.remove(i) }
                            DDLRepository().updateDDL(updatedHabit.copy(note = json.toString(), habitCount = updatedHabit.habitCount - 1))
                            vm.loadData(selectedPage); habitVm.refresh()
                        }
                    }
                },
                onCelebrate = { celebrate() }, moreExpanded = moreExpanded, moreAnchorRect = moreAnchorRect,
                useAvatar = useAvatar, nickname = profile.nickname, avatarPainter = avatarPainter,
                onCloseMorePanel = { moreExpanded = false }, selectionMode = selectionMode,
                isSelected = { selectedIds.contains(it) }, onItemLongPress = { enterSelection(it) }, onItemClickInSelection = { toggleSelection(it) }
            )

            // 撒花层
            key(fireKey) { KonfettiView(modifier = Modifier.fillMaxSize(), parties = parties) }

            // Snackbar 宿主
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = data, shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary, modifier = Modifier
                            .padding(16.dp)
                            .padding(bottom = 80.dp)
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------
    // 3. MIUIX 原生对话框与浮层
    // ---------------------------------------------------------
    if (showOverlay) {
        AIOverlayHost(initialText = "", onAddDDL = { launcher.launch(it) }, onRemoveFromWindow = { showOverlay = false }, respondIme = true)
    }

    if (showDeleteDialog) {
        // 1. 状态桥接：把你的 Boolean 包装成 MIUIX 需要的 MutableState
        val miuixShowState = remember { mutableStateOf(showDeleteDialog) }
        LaunchedEffect(showDeleteDialog) { miuixShowState.value = showDeleteDialog }
        LaunchedEffect(miuixShowState.value) {
            if (!miuixShowState.value && showDeleteDialog) showDeleteDialog = false
        }

        top.yukonga.miuix.kmp.extra.WindowDialog(
            show = miuixShowState, // 传入包装好的 State
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.alert_delete_title),
            summary = stringResource(R.string.alert_delete_message),
        ) {
            // 3. 最后一个大括号直接就是按钮区 (Content)
            // 我们用 Row 把两个按钮并排，并设置间距
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp), // 距离上方的 summary 留出呼吸感
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 取消按钮：使用次要按钮样式 (默认的 ButtonColors 就是次要的浅色)
                top.yukonga.miuix.kmp.basic.Button(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    top.yukonga.miuix.kmp.basic.Text(stringResource(R.string.cancel))
                }

                // 确认按钮：使用主要按钮样式 (Primary)
                top.yukonga.miuix.kmp.basic.Button(
                    onClick = {
                        selectedIds.toList().forEach { id ->
                            DDLRepository().deleteDDL(id)
                            HabitRepository().deleteHabitByDdlId(id)
                            DeadlineAlarmScheduler.cancelAlarm(activity.applicationContext, id)
                        }
                        vm.loadData(selectedPage)
                        habitVm.refresh()
                        Toast.makeText(activity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                        clearSelection()
                        showDeleteDialog = false
                    },
                    colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f)
                ) {
                    top.yukonga.miuix.kmp.basic.Text(stringResource(R.string.accept), color = Color.White)
                }
            }
        }
    }
}

// 辅助函数 (保持私有复用)
private fun consumeDeadlinerUrl(activity: Activity): String? {
    val data = activity.intent?.dataString ?: return null
    if (data.startsWith(DeadlinerURLScheme.DEADLINER_URL_SCHEME_PREFIX) || data.startsWith(DeadlinerURLScheme.DEADLINER_URL_SCHEME_PREFIX_LEGACY)) {
        activity.intent?.data = null
        return data
    }
    return null
}
