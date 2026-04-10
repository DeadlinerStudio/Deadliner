package com.aritxonly.deadliner

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import com.aritxonly.deadliner.ui.base.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.ui.settings.RoundedTextField
import com.aritxonly.deadliner.ui.settings.RoundedTextFieldMetrics
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.random.Random

data class InspirationItem(
    val id: Long,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class CaptureUiState(
    val draftText: String = "",
    val query: String = "",
    val items: List<InspirationItem> = emptyList(),
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val editingItemId: Long? = null,
    val editingText: String = "",
    val loading: Boolean = false
) {
    val filteredItems: List<InspirationItem>
        get() = if (query.isBlank()) items else items.filter { it.text.contains(query, ignoreCase = true) }
}

sealed interface CaptureEffect {
    data class ToastRes(@StringRes val resId: Int) : CaptureEffect
    data class OpenTaskEditor(val generated: GeneratedDDL?, val sourceText: String) : CaptureEffect
    data class OpenHabitEditor(val sourceText: String) : CaptureEffect
}

private class CaptureRepository(context: Context) {
    private val prefs = context.getSharedPreferences("capture_inspiration", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "items_json"

    fun load(): List<InspirationItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<InspirationItem>>() {}.type
        return runCatching { gson.fromJson<List<InspirationItem>>(raw, type) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
    }

    fun save(items: List<InspirationItem>) {
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }
}

class CaptureViewModel(
    app: Application
) : AndroidViewModel(app) {
    private val repo = CaptureRepository(app)
    private val _uiState = MutableStateFlow(CaptureUiState(items = repo.load()))
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CaptureEffect>()
    val effects: SharedFlow<CaptureEffect> = _effects.asSharedFlow()

    private val aiDueFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun updateDraft(text: String) {
        _uiState.value = _uiState.value.copy(draftText = text)
    }

    fun updateQuery(text: String) {
        _uiState.value = _uiState.value.copy(query = text)
    }

    fun saveDraft() {
        val text = _uiState.value.draftText.trim()
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        val newItem = InspirationItem(
            id = now + Random.nextLong(1000L, 99999L),
            text = text,
            createdAt = now,
            updatedAt = now
        )
        val newItems = listOf(newItem) + _uiState.value.items
        repo.save(newItems)
        _uiState.value = _uiState.value.copy(draftText = "", items = newItems)
    }

    fun openDetail(itemId: Long) {
        val item = _uiState.value.items.find { it.id == itemId } ?: return
        _uiState.value = _uiState.value.copy(editingItemId = item.id, editingText = item.text)
    }

    fun closeDetail() {
        _uiState.value = _uiState.value.copy(editingItemId = null, editingText = "")
    }

    fun updateEditingText(text: String) {
        _uiState.value = _uiState.value.copy(editingText = text)
    }

    fun saveEditing() {
        val state = _uiState.value
        val id = state.editingItemId ?: return
        val text = state.editingText.trim()
        if (text.isEmpty()) return
        val updated = state.items.map {
            if (it.id == id) it.copy(text = text, updatedAt = System.currentTimeMillis()) else it
        }.sortedByDescending { it.updatedAt }
        repo.save(updated)
        _uiState.value = state.copy(items = updated)
    }

    fun deleteItem(itemId: Long) {
        val state = _uiState.value
        val updated = state.items.filterNot { it.id == itemId }
        repo.save(updated)
        _uiState.value = state.copy(
            items = updated,
            selectedIds = state.selectedIds - itemId,
            editingItemId = if (state.editingItemId == itemId) null else state.editingItemId,
            editingText = if (state.editingItemId == itemId) "" else state.editingText
        )
    }

    fun toggleMultiSelect() {
        _uiState.value = _uiState.value.copy(isMultiSelectMode = true)
    }

    fun exitMultiSelect() {
        _uiState.value = _uiState.value.copy(isMultiSelectMode = false, selectedIds = emptySet())
    }

    fun toggleSelect(itemId: Long) {
        val state = _uiState.value
        val newSet = state.selectedIds.toMutableSet()
        if (!newSet.add(itemId)) newSet.remove(itemId)
        _uiState.value = state.copy(selectedIds = newSet)
    }

    fun deleteSelected() {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        val updated = state.items.filterNot { state.selectedIds.contains(it.id) }
        repo.save(updated)
        _uiState.value = state.copy(
            items = updated,
            selectedIds = emptySet(),
            isMultiSelectMode = false
        )
    }

    private fun mergeTextOfSelected(): String {
        val state = _uiState.value
        val selected = state.items.filter { state.selectedIds.contains(it.id) }
            .sortedBy { it.createdAt }
        return selected.joinToString("\n\n---\n\n") { it.text.trim() }
    }

    private fun replaceSelectedByMerged(): InspirationItem? {
        val state = _uiState.value
        if (state.selectedIds.size < 2) return null
        val mergedText = mergeTextOfSelected().trim()
        if (mergedText.isEmpty()) return null
        val now = System.currentTimeMillis()
        val merged = InspirationItem(
            id = now + Random.nextLong(111L, 999L),
            text = mergedText,
            createdAt = now,
            updatedAt = now
        )
        val remaining = state.items.filterNot { state.selectedIds.contains(it.id) }
        val updated = (listOf(merged) + remaining).sortedByDescending { it.updatedAt }
        repo.save(updated)
        _uiState.value = state.copy(
            items = updated,
            selectedIds = emptySet(),
            isMultiSelectMode = false
        )
        return merged
    }

    fun mergeSelectedOnly() {
        if (_uiState.value.selectedIds.size < 2) return
        replaceSelectedByMerged()
    }

    fun convertCurrentEditingToTask(useAi: Boolean) {
        val state = _uiState.value
        val text = state.editingText.trim()
        if (text.isEmpty()) return
        convertTextToTask(text, useAi)
    }

    fun convertCurrentEditingToHabit(useAi: Boolean) {
        val state = _uiState.value
        val text = state.editingText.trim()
        if (text.isEmpty()) return
        convertTextToHabit(text, useAi)
    }

    fun convertItemToTask(itemId: Long, useAi: Boolean) {
        val text = _uiState.value.items.find { it.id == itemId }?.text?.trim().orEmpty()
        if (text.isEmpty()) return
        convertTextToTask(text, useAi)
    }

    fun convertItemToHabit(itemId: Long, useAi: Boolean) {
        val text = _uiState.value.items.find { it.id == itemId }?.text?.trim().orEmpty()
        if (text.isEmpty()) return
        convertTextToHabit(text, useAi)
    }

    fun mergeAndConvertToTask(useAi: Boolean) {
        val merged = replaceSelectedByMerged() ?: return
        convertTextToTask(merged.text, useAi)
    }

    fun mergeAndConvertToHabit(useAi: Boolean) {
        val merged = replaceSelectedByMerged() ?: return
        convertTextToHabit(merged.text, useAi)
    }

    private fun convertTextToTask(text: String, useAi: Boolean) {
        if (!useAi) {
            viewModelScope.launch { _effects.emit(CaptureEffect.OpenTaskEditor(generated = null, sourceText = text)) }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val generated = runCatching {
                val json = AIUtils.generateMixed(getApplication(), text)
                val mixed = AIUtils.parseMixedResult(json)
                val firstTask = mixed.tasks.firstOrNull() ?: return@runCatching null
                val due = LocalDateTime.parse(firstTask.dueTime, aiDueFormatter)
                GeneratedDDL(name = firstTask.name, dueTime = due, note = firstTask.note.orEmpty())
            }.getOrNull()
            _effects.emit(CaptureEffect.OpenTaskEditor(generated = generated, sourceText = text))
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    private fun convertTextToHabit(text: String, useAi: Boolean) {
        viewModelScope.launch {
            if (useAi) {
                _effects.emit(CaptureEffect.ToastRes(R.string.capture_ai_habit_toast))
            }
            _effects.emit(CaptureEffect.OpenHabitEditor(sourceText = text))
        }
    }
}

class CaptureActivity : ComponentActivity() {
    private val vm by viewModels<CaptureViewModel> {
        object : ViewModelProvider.AndroidViewModelFactory(application) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        DynamicColorsExtension.apply(this, null)

        setContent {
            DeadlinerTheme {
                CaptureScreen(vm = vm, onClose = { finishAfterTransition() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureScreen(
    vm: CaptureViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val ui by vm.uiState.collectAsState()
    var showMergeSheet by rememberSaveable { mutableStateOf(false) }
    var showDirectConvertMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            when (effect) {
                is CaptureEffect.ToastRes -> {
                    Toast.makeText(context, context.getString(effect.resId), Toast.LENGTH_SHORT).show()
                }
                is CaptureEffect.OpenTaskEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 0)
                        effect.generated?.let { putExtra("EXTRA_GENERATE_DDL", it) }
                    }
                    context.startActivity(intent)
                }
                is CaptureEffect.OpenHabitEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 1)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    if (ui.editingItemId != null) {
        CaptureDetailSheet(
            text = ui.editingText,
            loading = ui.loading,
            onTextChange = vm::updateEditingText,
            onClose = vm::closeDetail,
            onSave = vm::saveEditing,
            onDelete = {
                ui.editingItemId?.let(vm::deleteItem)
            },
            onAiTask = { vm.convertCurrentEditingToTask(useAi = true) },
            onAiHabit = { vm.convertCurrentEditingToHabit(useAi = true) },
            onDirectTask = { vm.convertCurrentEditingToTask(useAi = false) },
            onDirectHabit = { vm.convertCurrentEditingToHabit(useAi = false) }
        )
    }

    if (showMergeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMergeSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.capture_merge_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.capture_merge_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        showMergeSheet = false
                        vm.mergeAndConvertToTask(useAi = true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_ai_task)) }
                FilledTonalButton(
                    onClick = {
                        showMergeSheet = false
                        vm.mergeAndConvertToHabit(useAi = true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_ai_habit)) }

                Box {
                    OutlinedButton(
                        onClick = { showDirectConvertMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.capture_direct_secondary))
                    }
                    DropdownMenu(
                        expanded = showDirectConvertMenu,
                        onDismissRequest = { showDirectConvertMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                showDirectConvertMenu = false
                                showMergeSheet = false
                                vm.mergeAndConvertToTask(useAi = false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                showDirectConvertMenu = false
                                showMergeSheet = false
                                vm.mergeAndConvertToHabit(useAi = false)
                            }
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (!ui.isMultiSelectMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.capture_title)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = vm::toggleMultiSelect) { Text(stringResource(R.string.capture_multi_select)) }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.capture_selected_count, ui.selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = vm::exitMultiSelect) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = vm::deleteSelected,
                            enabled = ui.selectedIds.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.capture_delete))
                        }
                        TextButton(
                            onClick = { showMergeSheet = true },
                            enabled = ui.selectedIds.size >= 2
                        ) {
                            Text(stringResource(R.string.capture_merge_count, ui.selectedIds.size))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CaptureInputCard(
                        draftText = ui.draftText,
                        onDraftChange = vm::updateDraft,
                        onSave = vm::saveDraft
                    )
                    RoundedTextField(
                        value = ui.query,
                        onValueChange = vm::updateQuery,
                        hint = stringResource(R.string.capture_search_label),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (ui.filteredItems.isNotEmpty()) {
                item {
                    Text(
                        text = if (ui.query.isBlank()) {
                            stringResource(R.string.capture_list_title)
                        } else {
                            stringResource(R.string.capture_search_result_group)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                }
            }

            items(ui.filteredItems, key = { it.id }) { item ->
                InspirationLightCard(
                    item = item,
                    selected = ui.selectedIds.contains(item.id),
                    inMultiSelectMode = ui.isMultiSelectMode,
                    onClick = {
                        if (ui.isMultiSelectMode) vm.toggleSelect(item.id) else vm.openDetail(item.id)
                    },
                    onLongPressSelect = { vm.toggleSelect(item.id) },
                    onDelete = { vm.deleteItem(item.id) },
                    onAiTask = { vm.convertItemToTask(item.id, useAi = true) },
                    onAiHabit = { vm.convertItemToHabit(item.id, useAi = true) }
                )
            }

            if (ui.filteredItems.isEmpty()) {
                item {
                    EmptyCaptureHint(
                        text = if (ui.query.isBlank()) {
                            stringResource(R.string.capture_empty_hint_default)
                        } else {
                            stringResource(R.string.capture_empty_hint_search)
                        }
                    )
                }
            }

            item { Box(modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun CaptureInputCard(
    draftText: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val sectionShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = sectionShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_input_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.capture_input_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.capture_input_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RoundedTextField(
                value = draftText,
                onValueChange = onDraftChange,
                hint = stringResource(R.string.capture_input_placeholder),
                metrics = RoundedTextFieldMetrics(
                    singleLine = false,
                    minHeight = 120.dp,
                    cornerSize = 12.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.capture_voice_reserved_toast), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = null
                    )
                    Text(" ${stringResource(R.string.capture_voice_input)}")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.capture_save)) }
            }
        }
    }
}

@Composable
private fun InspirationLightCard(
    item: InspirationItem,
    selected: Boolean,
    inMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongPressSelect: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val sectionShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val cardColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = sectionShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lightbulb),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "  ${formatRelativeTime(context, item.updatedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (inMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .let {
                                if (selected) it else it.background(
                                    color = Color.Transparent,
                                    shape = CircleShape
                                )
                            }
                    )
                } else {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = stringResource(R.string.settings_more)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_ai_task)) },
                            onClick = {
                                menuExpanded = false
                                onAiTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_ai_habit)) },
                            onClick = {
                                menuExpanded = false
                                onAiHabit()
                            }
                        )
                    }
                }
            }

            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.capture_card_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCaptureHint(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CaptureDetailSheet(
    text: String,
    loading: Boolean,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit,
    onDirectTask: () -> Unit,
    onDirectHabit: () -> Unit
) {
    var secondaryMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close)
                    )
                }
                Text(
                    stringResource(R.string.capture_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }

            RoundedTextField(
                value = text,
                onValueChange = onTextChange,
                hint = stringResource(R.string.capture_detail_placeholder),
                metrics = RoundedTextFieldMetrics(
                    singleLine = false,
                    minHeight = 180.dp,
                    cornerSize = 12.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )

            if (loading) {
                Text(
                    stringResource(R.string.capture_ai_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                stringResource(R.string.capture_ai_default_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onAiTask) { Text(stringResource(R.string.capture_ai_task)) }
                FilledTonalButton(onClick = onAiHabit) { Text(stringResource(R.string.capture_ai_habit)) }
                Box {
                    OutlinedButton(onClick = { secondaryMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = null
                        )
                        Text(" ${stringResource(R.string.capture_direct_secondary_short)}")
                    }
                    DropdownMenu(
                        expanded = secondaryMenuExpanded,
                        onDismissRequest = { secondaryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectHabit()
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            TextButton(onClick = onDelete) { Text(stringResource(R.string.capture_delete_item)) }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.capture_clipboard_label), text))
    Toast.makeText(context, context.getString(R.string.capture_clipboard_copied), Toast.LENGTH_SHORT).show()
}

private fun formatRelativeTime(context: Context, timestampMillis: Long): String {
    val nowMillis = System.currentTimeMillis()
    val diff = Duration.ofMillis((nowMillis - timestampMillis).absoluteValue)
    val minutes = diff.toMinutes()
    val hours = diff.toHours()
    val days = diff.toDays()
    return when {
        minutes < 1 -> context.getString(R.string.capture_relative_just_now)
        minutes < 60 -> context.getString(R.string.capture_relative_minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.capture_relative_hours_ago, hours)
        days < 7 -> context.getString(R.string.capture_relative_days_ago, days)
        else -> {
            val date = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMillis),
                ZoneId.systemDefault()
            )
            date.format(DateTimeFormatter.ofPattern(context.getString(R.string.capture_date_short_pattern)))
        }
    }
}
