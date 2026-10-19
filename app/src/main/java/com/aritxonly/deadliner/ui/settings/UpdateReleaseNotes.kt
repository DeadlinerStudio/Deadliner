package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.SoftGlassDropdownPreference
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.AlertDialog
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.util.Locale
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults

@Composable
internal fun UpdateReleaseNotes(state: UpdateState.Available, showFilters: Boolean, onDismissFilters: () -> Unit) {
    val sections = remember(state.notes, state.latest) { parseReleaseNotes(state.notes, state.latest) }
    val languages = remember(sections) { sections.mapNotNull { it.language }.distinct() }
    val configuration = LocalConfiguration.current
    val locales = (0 until configuration.locales.size()).map { configuration.locales[it] }
    val preferred = preferredReleaseNotesLanguage(languages, locales)
    var selectedLanguage by rememberSaveable(state.notes) { mutableStateOf("system") }
    var selectedTitle by rememberSaveable(state.notes) { mutableStateOf<String?>(null) }
    var showOriginal by rememberSaveable(state.notes) { mutableStateOf(false) }
    val language = when (selectedLanguage) { "system" -> preferred; "all" -> null; else -> selectedLanguage }
    val localizedSections = remember(sections, language) { filterReleaseNotes(sections, language, null) }
    val titles = remember(localizedSections) { localizedSections.mapNotNull { it.title }.distinct() }
    val title = selectedTitle?.takeIf { it in titles }
    val visible = remember(localizedSections, title) { filterReleaseNotes(localizedSections, null, title) }
    val canFilter = languages.isNotEmpty() || titles.size > 1
    AlertDialog(show = showFilters, onDismissRequest = onDismissFilters,
        miuixTitle = stringResource(R.string.update_log_filters),
        confirmButton = {
            TextButton(onClick = onDismissFilters, modifier = Modifier.testTag("update-filters-done")) {
                Text(stringResource(R.string.update_filters_done))
            }
        },
        text = {
          Column {
            if (!canFilter) Text(stringResource(R.string.update_no_filters),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (languages.isNotEmpty()) {
                val options = listOf("system", "all") + languages
                SoftGlassDropdownPreference(
                    title = stringResource(R.string.update_log_language),
                    items = listOf(stringResource(R.string.update_follow_system), stringResource(R.string.update_all_languages)) +
                        languages.map { Locale.forLanguageTag(it).getDisplayName(Locale.forLanguageTag(it)) },
                    selectedIndex = options.indexOf(selectedLanguage).coerceAtLeast(0),
                    onSelectedIndexChange = { selectedLanguage = options[it]; selectedTitle = null; showOriginal = false },
                    modifier = Modifier.testTag("update-language-filter"),
                )
            }
            if (titles.size > 1) SoftGlassDropdownPreference(
                title = stringResource(R.string.update_log_section),
                items = listOf(stringResource(R.string.update_all_sections)) + titles.map(::releaseSectionLabel),
                selectedIndex = if (title == null) 0 else titles.indexOf(title) + 1,
                onSelectedIndexChange = { selectedTitle = titles.getOrNull(it - 1); showOriginal = false },
                modifier = Modifier.testTag("update-section-filter"),
            )
          }
        },
    )
    Column(Modifier.fillMaxWidth().testTag("update-release-notes"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (canFilter || showOriginal) Row(Modifier.fillMaxWidth()
            .padding(horizontal = SettingsItemDefaults.HorizontalPadding), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.update_release_notes), Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            MiuixTextButton(
                text = stringResource(if (showOriginal) R.string.update_formatted_notes else R.string.update_original_notes),
                onClick = { showOriginal = !showOriginal },
                modifier = Modifier.widthIn(min = 48.dp).testTag("update-original-toggle"),
                minHeight = 48.dp, insideMargin = PaddingValues(0.dp),
                textStyle = MaterialTheme.typography.labelLarge,
                colors = MiuixButtonDefaults.textButtonColors(
                    color = Color.Transparent, disabledColor = Color.Transparent,
                    textColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        Column {
            when {
                state.notes.isBlank() -> ReleaseNoteCard(null, stringResource(R.string.update_no_release_notes))
                showOriginal -> ReleaseNoteCard(null, state.notes)
                visible.isEmpty() -> ReleaseNoteCard(null, state.notes) // Unknown formats always remain readable.
                else -> visible.forEachIndexed { index, section ->
                    key(section.language, section.title, index) {
                        ReleaseNoteCard(section.title, section.markdown, if (selectedLanguage == "all") section.language else null)
                    }
                }
            }
        }
        if (updateDownloadUri(state) == null) Text(stringResource(R.string.update_no_download),
            Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun releaseSectionLabel(title: String): String = title.replace(Regex("[*_`]"), "")

@Composable
private fun ReleaseNoteCard(title: String?, markdown: String, language: String? = null) {
    SettingsSection(modifier = Modifier.testTag("update-note-card")) {
        Column(Modifier.padding(SettingsItemDefaults.HorizontalPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (language != null) Text(Locale.forLanguageTag(language).getDisplayName(Locale.forLanguageTag(language)),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (title == null) Text(stringResource(R.string.update_release_notes), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            ReleaseMarkdown(if (title == null) markdown else "## $title\n\n$markdown")
        }
    }
}

@Composable
private fun ReleaseMarkdown(markdown: String) {
    val colors = MaterialTheme.colorScheme
    MarkdownText(markdown = markdown, modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant),
        linkColor = colors.primary, syntaxHighlightColor = colors.surfaceContainerHighest,
        syntaxHighlightTextColor = colors.onSurface, headingBreakColor = colors.outlineVariant, isTextSelectable = true)
}
