package com.aritxonly.deadliner.ui.settings

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ReleaseNotesParserTest {
    @Test fun bilingualReleaseHeadersAndH2CardsPreserveLowerHeadingsAndMarkdown() {
        val markdown = """
            ## 📌 更新日志 / Changelog
            #### 🌏 中文
            ### v5.0.0
            ## ✨ 新增功能
            ### 快速添加
            - **识别** [任务](https://example.invalid)
              - 保留嵌套列表
            ## 🔧 修复
            - 修复闪烁
            ---
            #### 🌍 English
            ### v5.0.0
            ## ✨ Features
            - Add a task
            ## 🔧 Fixes
            - Fix flicker
            ## What's Changed
            - Release by @author
        """.trimIndent()
        val sections = parseReleaseNotes(markdown.replace("\n", "\r\n"))
        assertFalse(sections.any { it.title?.contains("Changelog") == true })
        val chinese = filterReleaseNotes(sections, "zh", null)
        assertEquals(listOf(null, "✨ 新增功能", "🔧 修复", "What's Changed"), chinese.map { it.title })
        assertEquals("### v5.0.0", chinese.first().markdown)
        assertTrue(chinese[1].markdown.contains("### 快速添加\n- **识别** [任务](https://example.invalid)\n  - 保留嵌套列表"))
        assertFalse(chinese.any { it.markdown.contains("Add a task") })
        assertEquals(listOf("- Fix flicker"), filterReleaseNotes(sections, "en", "🔧 Fixes").map { it.markdown })
        assertTrue(filterReleaseNotes(sections, null, null).any { it.title == "What's Changed" })
    }

    @Test fun fencedCodeAndIndentedHeadingsNeverSplitCardsOrChangeLanguage() {
        val markdown = "## Real\n~~~markdown\n## Not a card\n#### English\n~~~\n```kotlin\n## Also code\n````\n    ## Indented code\n### Subheading\n# Version"
        val sections = parseReleaseNotes(markdown)
        assertEquals(1, sections.size)
        assertEquals("Real", sections.single().title)
        assertTrue(sections.single().markdown.contains("#### English"))
        assertTrue(sections.single().markdown.endsWith("### Subheading\n# Version"))
        assertNull(sections.single().language)
    }

    @Test fun plainUnknownFormatsAndCommonIntroductionRemainVisibleAcrossLanguages() {
        val plain = "An announcement\n\n- One item\n  - A nested item\n\n[Full notes](https://example.invalid)"
        assertEquals(plain, parseReleaseNotes(plain).single().markdown)
        val sections = parseReleaseNotes("Notice for everyone\n#### 中文\n## 修复\n内容\n#### English\n## Fixes\nContent")
        val english = filterReleaseNotes(sections, "en", null)
        assertTrue(english.any { it.markdown == "Notice for everyone" })
        assertTrue(english.any { it.markdown == "Content" })
        assertFalse(english.any { it.markdown == "内容" })
        assertTrue(parseReleaseNotes(" \n\n").isEmpty())
    }

    @Test fun latestReleaseStyleOfBulletGroupsStaysInOneCardPerLanguage() {
        val sections = parseReleaseNotes("## 📌 更新日志 / Changelog\n#### 🌏 中文\n### v5.0.0\n- 🎨 新界面：\n  - 细节\n- 🌈 新配色：\n  - 颜色\n---\n#### 🌍 English\n### v5.0.0\n- New UI:\n  - Details")
        assertEquals(2, sections.size)
        assertTrue(filterReleaseNotes(sections, "zh", null).single().markdown.contains("- 🌈 新配色"))
        assertTrue(filterReleaseNotes(sections, "en", null).single().markdown.contains("- New UI:"))
    }

    @Test fun localeSelectionUsesRegionScriptThenLanguageAndEnglishFallback() {
        val tags = listOf("zh-Hans", "zh-Hant", "en-US", "en-GB")
        assertEquals("zh-Hant", preferredReleaseNotesLanguage(tags, listOf(Locale.TAIWAN)))
        assertEquals("zh-Hans", preferredReleaseNotesLanguage(tags, listOf(Locale.CHINA)))
        assertEquals("en-GB", preferredReleaseNotesLanguage(tags, listOf(Locale.UK)))
        assertEquals("en-US", preferredReleaseNotesLanguage(tags, listOf(Locale.JAPAN)))
        assertEquals("en-GB", preferredReleaseNotesLanguage(tags, listOf(Locale.FRANCE, Locale.UK)))
        assertEquals("zh", preferredReleaseNotesLanguage(listOf("zh"), listOf(Locale.US)))
        assertNull(preferredReleaseNotesLanguage(emptyList(), listOf(Locale.US)))
    }

    @Test fun explicitRegionalHeadingsAreRecognizedWithoutMisreadingFeatureTitles() {
        val sections = parseReleaseNotes("## zh-TW\n## 修正 ##\n繁體內容\n## en-US\n## English support\nA new feature")
        assertEquals(listOf("zh-TW", "en-US"), sections.map { it.language })
        assertEquals(listOf("修正", "English support"), sections.map { it.title })
    }

    @Test fun usersExactBilingualReleaseSplitsIntoSixGroupsPerLanguageAndOneSharedFooter() {
        val original = javaClass.getResource("/updates/v5.0.0-bilingual.md")!!.readText()
        val sections = parseReleaseNotes(original)
        val chinese = sections.filter { it.language == "zh" }
        val english = sections.filter { it.language == "en" }
        assertEquals(6, chinese.size)
        assertEquals(6, english.size)
        assertEquals("🎨 全新界面与设计系统", chinese.first().title)
        assertEquals("✨ 引导与细节优化", chinese.last().title)
        assertTrue(chinese.first().markdown.startsWith("### v5.0.0"))
        assertTrue(chinese[1].markdown.contains("**高级材质效果**"))
        assertFalse(chinese.last().markdown.endsWith("---"))
        assertTrue(english[4].markdown.contains("**BYOK**"))
        assertTrue(chinese.first().markdown.contains("* 新增 **简洁模式**"))
        val footer = sections.single { it.language == null }
        assertEquals("What's Changed", footer.title)
        assertTrue(footer.markdown.contains("https://github.com/DeadlinerStudio/Deadliner/compare/v4.0.2...v5.0.0"))
        assertEquals(7, filterReleaseNotes(sections, "zh", null).size)
        assertEquals(7, filterReleaseNotes(sections, "en", null).size)
    }

    @Test fun legacyGroupsKeepNestedFormattingAndDoNotOverrideH2OrSplitOrdinaryLists() {
        val legacy = "* **Group**:\n    * Item\n        * **Nested**:\n            * Child\n\n* **Next**：\n    * Another"
        val sections = parseReleaseNotes(legacy)
        assertEquals(listOf("Group", "Next"), sections.map { it.title })
        assertTrue(sections.first().markdown.contains("* Item\n    * **Nested**:\n        * Child"))
        assertEquals(1, parseReleaseNotes("## Explicit H2\n$legacy").size)
        assertEquals(1, parseReleaseNotes("* Ordinary\n    * Nested\n* Another").size)
        assertEquals(1, parseReleaseNotes("```md\n$legacy\n```").size)
    }

    @Test fun formattedNotesOmitOnlyTheMatchingVersionHeadingAndPreserveRealContent() {
        val sample = javaClass.getResource("/updates/v5.0.0-bilingual.md")!!.readText()
        val sections = parseReleaseNotes(sample, "v5.0.0")
        assertEquals(13, sections.size)
        assertFalse(sections.any { it.markdown.contains("### v5.0.0") })
        assertTrue(sections.first().markdown.startsWith("* 全面刷新"))
        val mixed = "### v5.0.0\n## Fixes\n### v4.9.0\nUpdated v5.0.0 behavior\n```md\n### v5.0.0\n```\n### v5.0.0 Compatibility"
        val body = parseReleaseNotes(mixed, "5.0.0").single().markdown
        assertTrue(body.contains("### v4.9.0"))
        assertTrue(body.contains("Updated v5.0.0 behavior"))
        assertTrue(body.contains("```md\n### v5.0.0\n```"))
        assertTrue(body.contains("### v5.0.0 Compatibility"))
    }
}
