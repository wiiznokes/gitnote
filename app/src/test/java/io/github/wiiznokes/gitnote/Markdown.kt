package io.github.wiiznokes.gitnote

import io.github.wiiznokes.gitnote.ui.screen.app.edit.shouldRemoveLineRegex
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegexTest {


    @Test
    fun shouldRemoveLineValidMatches() {
        val validCases = listOf(
            "- ",
            "-    ",
            "* ",
            ".2 ",
            ".122 ",
            "- [ ] ",
            "- [x] ",
            " - ",
            "\t- ",
            " \t - ",
        )
        for (input in validCases) {
            assertTrue(shouldRemoveLineRegex.containsMatchIn(input), "Should match: '$input'")
        }
    }

    @Test
    fun shouldRemoveLineInvalidMatches() {
        val invalidCases = listOf(
            "- abc",
            "-      \tabc",
            "* abc",
            ".2 abc",
            ".122 abc",
            "- [ ] abc",
            "-[ ] ",
            "- [] ",
            "- [x] abc",
            "abc",
            " - abc",
            "\t- abc",
            " \t - abc",
            "abc - abc",
        )

        for (input in invalidCases) {
            assertFalse(shouldRemoveLineRegex.containsMatchIn(input), "Should NOT match: '$input'")
        }
    }
}