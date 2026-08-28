package com.aritxonly.deadliner.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.aritxonly.deadliner.logging.AppLog
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.model.CalendarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.deadliner.domain.task.Task
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.abs

/**
 * Android calendar adapter for shared KMP tasks.
 */
class CalendarHelper(private val context: Context) {

    companion object {
        private const val CALENDAR_ACCOUNT_NAME = "Deadliner"
        private const val CALENDAR_DISPLAY_NAME = "Deadliner Calendar"
        private const val CALENDAR_ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL
        private val ICS_DATE_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Ensures a calendar exists and returns its ID.
     */
    private fun getOrCreateCalendarId(): Long {
        val resolver = context.contentResolver
        // Query primary calendar
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(CALENDAR_ACCOUNT_NAME)

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }

        // Create a new local calendar
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }

        val insertUri = uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            .build()

        val result: Uri = resolver.insert(insertUri, values)
            ?: throw IllegalStateException("Failed to create calendar account")
        return ContentUris.parseId(result)
    }

    suspend fun insertEvent(task: Task): Long = withContext(Dispatchers.IO) {
        val calendarId = getOrCreateCalendarId()
        val resolver = context.contentResolver
        val endMillis = parseToMillis(task.dueAt.orEmpty())
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DTSTART, endMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.DESCRIPTION, task.note)
        }
        val existingId = task.calendarEventId
        if (existingId != null) {
            resolver.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId), values, null, null)
            return@withContext existingId
        }
        val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to insert calendar event")
        val eventId = ContentUris.parseId(uri)
        resolver.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 10)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        })
        eventId
    }

    fun getAllCalendarAccounts(): List<CalendarInfo> {
        val resolver = context.contentResolver

        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        val calendars = mutableListOf<CalendarInfo>()

        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                val accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE))

                calendars.add(CalendarInfo(id, name, accountName, accountType))
            }
        }

        return calendars
    }

    fun queryAllCalendarEvents(): List<CalendarEvent> {
        val accounts = getAllCalendarAccounts()
        val events = mutableListOf<CalendarEvent>()
        val filtered = GlobalUtils.filteredCalendars?:setOf()
        val customFilters = GlobalUtils.customCalendarFilterListSelected?:setOf()
        for (account in accounts) {
            if (filtered.contains(account.accountName)) continue
            val id = account.id
            events.addAll(queryCalendarEvents(id))
        }
        events.sortBy {
            abs(Duration.between(
                LocalDateTime.now(),
                GlobalUtils.safeParseDateTime(it.startMillis.toDateTimeString())
            ).toMillis())
        }
        events.filterNot { customFilters.contains(it.title) }
        return events
    }

    fun queryCalendarEvents(calendarId: Long): List<CalendarEvent> {
        val resolver = context.contentResolver
        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.RRULE
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val events = mutableListOf<CalendarEvent>()
        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            // 先把列索引拿出来，省得每次都查
            val idxId    = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val idxTitle = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val idxStart = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val idxEnd   = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val idxDesc  = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val idxRule  = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)

            while (cursor.moveToNext()) {
                val id    = cursor.getLong(idxId)
                // 下面几个字段都可能为 null，加一个默认值
                val title = cursor.getString(idxTitle) ?: context.getString(R.string.untitled)
                val dtStart = cursor.getLong(idxStart)
                val dtEnd   = cursor.getLong(idxEnd)
                val desc    = cursor.getString(idxDesc)  ?: ""
                val rule    = cursor.getString(idxRule)  ?: ""

                events.add(CalendarEvent(
                    id = id,
                    title = title,
                    startMillis = dtStart,
                    endMillis   = dtEnd,
                    description = desc,
                    rrule       = rule
                ))
            }
        } ?: run {
            AppLog.e("CalendarHelper", "queryCalendarEvents: cursor 为 null")
        }

        return events
    }

    /**
     * Parses a date-time string ("yyyy-MM-dd HH:mm") into milliseconds.
     */
    private fun parseToMillis(dateTime: String): Long {
        val ldt = GlobalUtils.parseDateTime(dateTime)
            ?: throw IllegalArgumentException("Invalid date format: $dateTime")

        return ldt.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
