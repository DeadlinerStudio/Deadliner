package com.aritxonly.deadliner.ui.settings

import java.util.Locale

/** Presentation-only sections; Markdown bodies retain their original lists, links and code. */
internal data class ReleaseNoteSection(val title: String?, val markdown: String, val language: String?)

private val markdownHeading = Regex("^ {0,3}(#{1,6})[ \\t]+(.+?)(?:[ \\t]+#+)?[ \\t]*$")
private val markdownFence = Regex("^ {0,3}(`{3,}|~{3,})(.*)$")

/** Language headings can occur at any level; only H2 divides the actual content into cards. */
internal fun parseReleaseNotes(markdown: String, releaseVersion: String? = null): List<ReleaseNoteSection> {
    val sections = mutableListOf<ReleaseNoteSection>()
    var language: String? = null
    var title: String? = null
    val body = mutableListOf<String>()
    var fence: String? = null
    fun flush() {
        // A blank-separated rule before a language/section boundary belongs to the document
        // layout. Avoid a heavy trailing divider in its card; preserve Setext headings/code.
        if (fence == null) {
            while (body.lastOrNull()?.isBlank() == true) body.removeAt(body.lastIndex)
            if (body.size >= 2 && body[body.lastIndex - 1].isBlank() &&
                body.last().trim().matches(Regex("([-*_])(?:[ \\t]*\\1){2,}"))) body.removeAt(body.lastIndex)
        }
        val text = body.joinToString("\n").trim()
        if (text.isNotBlank() || title != null) sections += ReleaseNoteSection(title, text, language)
        body.clear()
        title = null
    }
    for (line in markdown.replace("\r\n", "\n").replace('\r', '\n').lines()) {
        val marker = markdownFence.matchEntire(line)
        val openFence = fence
        if (openFence != null) {
            body += line
            if (marker != null && marker.groupValues[1].first() == openFence.first() &&
                marker.groupValues[1].length >= openFence.length && marker.groupValues[2].isBlank()) fence = null
            continue
        }
        if (marker != null) {
            fence = marker.groupValues[1]
            body += line
            continue
        }
        val heading = markdownHeading.matchEntire(line)
        val headingText = heading?.groupValues?.get(2)
        // The page already presents the target version. Strip only a standalone matching
        // heading from the formatted view; other versions, prose and fenced code remain.
        val plainHeading = headingText?.replace(Regex("[*_`]"), "")?.trim()?.removePrefix("v")?.removePrefix("V")
        val targetVersion = releaseVersion?.trim()?.removePrefix("v")?.removePrefix("V")
        if (!targetVersion.isNullOrBlank() && plainHeading == targetVersion) continue
        val headingLanguage = headingText?.let(::releaseHeadingLanguage)
        if (headingLanguage != null) {
            flush()
            language = headingLanguage
        } else if (heading?.groupValues?.get(1) == "##") {
            flush()
            if (headingText?.replace(Regex("[*_`]"), "")?.lowercase(Locale.ROOT) in
                listOf("what's changed", "what’s changed", "contributors")) language = null
            title = headingText
        } else {
            body += line
        }
    }
    flush()
    // Empty bilingual wrapper headings and separators carry no release content. The original
    // document remains available in the UI, including GitHub's generated footer and formatting.
    val cleaned = sections.filterNot { section ->
        val onlySeparator = section.markdown.lines().all { it.isBlank() || it.trim().matches(Regex("([-*_])(?:[ \\t]*\\1){2,}")) }
        onlySeparator && (section.title == null || section.title.contains("changelog", ignoreCase = true) || section.title.contains("更新日志"))
    }
    return cleaned.flatMap { section ->
        // Older releases use bold root-list labels in place of section headings. Only use
        // that compatibility path when this language has no actual H2 content sections.
        if (section.title == null && cleaned.none { it.language == section.language && it.title != null })
            splitLegacyReleaseGroups(section) else listOf(section)
    }
}

private val legacyReleaseGroup = Regex("^( {0,3})[-*+][ \\t]+\\*\\*(.+?)\\*\\*[ \\t]*[:：][ \\t]*$")

private fun splitLegacyReleaseGroups(section: ReleaseNoteSection): List<ReleaseNoteSection> {
    val lines = section.markdown.lines()
    var fence: String? = null
    val candidates = mutableListOf<Pair<Int, MatchResult>>()
    lines.forEachIndexed { index, line ->
        val marker = markdownFence.matchEntire(line)
        val opened = fence
        if (opened != null) {
            if (marker != null && marker.groupValues[1].first() == opened.first() &&
                marker.groupValues[1].length >= opened.length && marker.groupValues[2].isBlank()) fence = null
        } else if (marker != null) fence = marker.groupValues[1]
        else legacyReleaseGroup.matchEntire(line)?.let { group ->
            val next = lines.drop(index + 1).firstOrNull { it.isNotBlank() }
            if (next != null && next.takeWhile { it == ' ' }.length > group.groupValues[1].length)
                candidates += index to group
        }
    }
    val indent = candidates.minOfOrNull { it.second.groupValues[1].length } ?: return listOf(section)
    val groups = candidates.filter { it.second.groupValues[1].length == indent }
    val prefix = lines.take(groups.first().first).joinToString("\n").trim()
    return groups.mapIndexed { index, (start, group) ->
        val nested = lines.subList(start + 1, groups.getOrNull(index + 1)?.first ?: lines.size)
        val childIndent = nested.filter { it.isNotBlank() && it.takeWhile { c -> c == ' ' }.length > indent }
            .minOfOrNull { it.takeWhile { c -> c == ' ' }.length } ?: 0
        val body = nested.joinToString("\n") { line ->
            if (line.takeWhile { it == ' ' }.length >= childIndent) line.drop(childIndent) else line
        }.trim()
        ReleaseNoteSection(group.groupValues[2], listOfNotNull(prefix.takeIf { index == 0 && it.isNotBlank() }, body)
            .joinToString("\n\n"), section.language)
    }
}

private fun releaseHeadingLanguage(title: String): String? {
    val text = title.replace(Regex("[*`~]"), "").trim().trim('_').dropWhile { !it.isLetter() }
        .trimEnd { !it.isLetterOrDigit() && it != ')' && it != '）' }.lowercase(Locale.ROOT)
    return when (text) {
        "中文", "chinese", "zh" -> "zh"
        "简体中文", "简体", "simplified chinese", "zh-hans", "中文（简体）", "中文(简体)" -> "zh-Hans"
        "繁體中文", "繁体中文", "traditional chinese", "zh-hant", "中文（繁體）", "中文(繁體)" -> "zh-Hant"
        "english", "en" -> "en"
        else -> Regex("^(zh|en)[-_](cn|tw|hk|us|gb|au|ca)$").matchEntire(text)?.let {
            "${it.groupValues[1]}-${it.groupValues[2].uppercase(Locale.ROOT)}"
        }
    }
}

/** Prefer exact region/script, then language. Never return a missing translation or empty view. */
internal fun preferredReleaseNotesLanguage(tags: List<String>, locales: List<Locale>): String? {
    for (locale in locales) {
        tags.firstOrNull { it.equals(locale.toLanguageTag(), ignoreCase = true) }?.let { return it }
        if (locale.language == "zh") {
            val traditional = locale.script == "Hant" || locale.country in listOf("TW", "HK", "MO")
            val script = if (traditional) "Hant" else "Hans"
            tags.firstOrNull { it.equals("zh-$script", true) }?.let { return it }
        }
        tags.firstOrNull { it.equals(locale.language, true) }?.let { return it }
        tags.firstOrNull { Locale.forLanguageTag(it).language == locale.language }?.let { return it }
    }
    return tags.firstOrNull { Locale.forLanguageTag(it).language == "en" } ?: tags.firstOrNull()
}

internal fun filterReleaseNotes(sections: List<ReleaseNoteSection>, language: String?, title: String?): List<ReleaseNoteSection> =
    sections.filter { (language == null || it.language == null || it.language == language) && (title == null || it.title == title) }
