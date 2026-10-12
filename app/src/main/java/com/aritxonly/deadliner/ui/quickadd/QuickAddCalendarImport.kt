package com.aritxonly.deadliner.ui.quickadd

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.calendar.CalendarImportDialog
import com.aritxonly.deadliner.calendar.CalendarAccountFilterDialog
import com.aritxonly.deadliner.calendar.formatCalendarImportEvent
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.CalendarEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Only the platform permission/loading state differs; picker UI belongs to the regular editor. */
@Composable
internal fun rememberQuickAddCalendarImport(onImported: (CalendarEvent) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var accounts by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                events = withContext(Dispatchers.IO) { CalendarHelper(context).queryAllCalendarEvents() }
                query = ""; selectedId = null; showImport = true
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { Toast.makeText(context, "日历读取失败，请重试", Toast.LENGTH_SHORT).show() }
            finally { loading = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) load() else Toast.makeText(context, context.getString(R.string.permission_calendar_error,
            "Missing calendar permission"), Toast.LENGTH_SHORT).show()
    }
    if (showImport && !showFilter) CalendarImportDialog(events, query, selectedId,
        eventLabel = { formatCalendarImportEvent(context, it) }, onQueryChange = { query = it },
        onSelect = { selectedId = it }, onFilter = {
            scope.launch {
                try {
                    accounts = withContext(Dispatchers.IO) { CalendarHelper(context).getAllCalendarAccounts() }
                        .map { it.accountName }.filter(String::isNotBlank)
                    selectedAccounts = GlobalUtils.filteredCalendars?.filterNotNull()?.toSet() ?: emptySet()
                    if (accounts.isEmpty()) Toast.makeText(context, R.string.no_valid_calendar_account, Toast.LENGTH_SHORT).show()
                    else showFilter = true
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { Toast.makeText(context, "日历账号读取失败", Toast.LENGTH_SHORT).show() }
            }
        }, onConfirm = {
            events.firstOrNull { it.id == selectedId }?.let { onImported(it); showImport = false }
        }, onDismiss = { showImport = false })
    if (showFilter) CalendarAccountFilterDialog(accounts, selectedAccounts,
        onSelectionChange = { selectedAccounts = it }, onConfirm = {
            GlobalUtils.filteredCalendars = selectedAccounts
            showFilter = false
            showImport = false
            load()
        }, onDismiss = { showFilter = false })
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) load()
        else permission.launch(Manifest.permission.READ_CALENDAR)
    }
}
