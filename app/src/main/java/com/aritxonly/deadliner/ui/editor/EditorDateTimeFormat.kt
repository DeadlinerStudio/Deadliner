package com.aritxonly.deadliner.ui.editor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Shared display format for the quick draft and full editor. */
fun formatEditorDateTime(value: LocalDateTime): String = value.format(
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()),
)
