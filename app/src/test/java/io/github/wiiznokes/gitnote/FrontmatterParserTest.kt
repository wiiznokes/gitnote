package io.github.wiiznokes.gitnote

import io.github.wiiznokes.gitnote.helper.FrontmatterParser
import org.junit.Assert.assertEquals
import org.junit.Test

class FrontmatterParserTest {

    @Test
    fun testParseTags() {
        val content = """
            ---
            title: Test Note
            tags:
              - work
              - project
            ---
            # Test Note
            
            This is a test note.
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(listOf("work", "project"), tags)
    }

    @Test
    fun testParseTagsNoTags() {
        val content = """
            ---
            title: Test Note
            ---
            # Test Note
            
            This is a test note.
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(emptyList<String>(), tags)
    }

    @Test
    fun testParseTagsNoFrontmatter() {
        val content = """
            # Test Note
            
            This is a test note.
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(emptyList<String>(), tags)
    }

    @Test
    fun testParseTagsSingleTag() {
        val content = """
            ---
            tags:
              - meeting
            ---
            # Meeting Notes
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(listOf("meeting"), tags)
    }

    @Test
    fun testParseTagsWithOtherFields() {
        val content = """
            ---
            title: Meeting Notes
            created: 2025-12-27T09:00:00Z
            tags:
              - meeting
              - planning
              - quarterly
            updated: 2025-12-31T08:00:00Z
            ---
            # Meeting Notes
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(listOf("meeting", "planning", "quarterly"), tags)
    }

    @Test
    fun testParseTagsInBodyNotParsed() {
        val content = """
            ---
            title: Test Note
            ---
            # Test Note
            
            This note has tags in the body:
            tags:
              - work
              - project
            
            But they should not be parsed.
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(emptyList<String>(), tags)
    }

    @Test
    fun testParseTagsMixedFrontmatterAndBody() {
        val content = """
            ---
            title: Test Note
            tags:
              - frontmatter-tag
            ---
            # Test Note
            
            This note has tags in the body:
            tags:
              - body-tag
            
            Only frontmatter tags should be parsed.
        """.trimIndent()

        val tags = FrontmatterParser.parseTags(content)
        assertEquals(listOf("frontmatter-tag"), tags)
    }
}