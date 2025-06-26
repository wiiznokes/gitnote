package io.github.wiiznokes.gitnote

import io.github.wiiznokes.gitnote.ui.viewmodel.edit.ListType
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.analyzeListItem
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegexTest {

    @Test
    fun shouldRemoveLineValidMatches() {
        val validCases = listOf(
            "- ",
            "-    ",
            "* ",
            "2. ",
            "122. ",
            "- [ ] ",
            "- [x] ",
            " - ",
            "\t- ",
            " \t - ",
        )
        for (input in validCases) {
            val info = analyzeListItem(input)

            assertNotNull(info, input)
            assertTrue(info.shouldRemove(), "$input $info")
        }
    }

    @Test
    fun shouldRemoveLineInvalidMatches() {
        val invalidCases = listOf(
            "- abc",
            "-      \tabc",
            "* abc",
            "2. abc",
            "122. abc",
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
            val info = analyzeListItem(input)

            if (info != null) {
                assertFalse(info.shouldRemove(), input)
            }
        }
    }

    @Test
    fun dashListValidMatches() {
        val validCases = listOf(
            "- abc",
            " - abc",
            " -       abc",
            "\t- abc",
            " \t - abc",
        )
        for (input in validCases) {
            val info = analyzeListItem(input)

            assertNotNull(info, input)
            assertTrue(info.listType is ListType.Dash)
            assertFalse(info.isTaskList)
        }
    }

    @Test
    fun asteriskListValidMatches() {
        val validCases = listOf(
            "* abc",
            " * abc",
            " *        abc",
            "\t* abc",
            " \t * abc",
        )
        for (input in validCases) {
            val info = analyzeListItem(input)

            assertNotNull(info, input)
            assertTrue(info.listType is ListType.Asterisk)
            assertFalse(info.isTaskList)
        }
    }

    @Test
    fun numberListValidMatches() {
        val validCases = listOf(
            "1. abc",
            "122. abc",
        )
        for (input in validCases) {
            val info = analyzeListItem(input)

            assertNotNull(info, input)
            assertTrue(info.listType is ListType.Number)
            assertFalse(info.isTaskList)
        }
    }

    @Test
    fun braceListValidMatches() {
        val validCases = listOf(
            "- [x] abc",
            "- [ ] abc",
            "* [x] abc",
            "* [ ] abc",
            "1. [x] abc",
            "23. [ ] abc",
            "   - [x] task",
            "\t* [ ] task",
            " \t 12. [X] task"
        )
        for (input in validCases) {
            val info = analyzeListItem(input)

            assertNotNull(info, input)
            assertTrue(info.isTaskList)
        }
    }


}