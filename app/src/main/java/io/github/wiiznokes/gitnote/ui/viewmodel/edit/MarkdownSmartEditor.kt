package io.github.wiiznokes.gitnote.ui.viewmodel.edit

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


private const val TAG = "markdownSmartEditor"

fun markdownSmartEditor(
    prev: TextFieldValue,
    v: TextFieldValue
): TextFieldValue {

    if (v.selection.start == v.selection.end) {

        val cursorPos = v.selection.start
        if (cursorPos > 0 && cursorPos <= v.text.length) {


            if (v.text[cursorPos - 1] == '\n') {

                // handle delete key when the line is:
                // - x
                //
                if (prev.text.length >= v.text.length) {
                    return v
                }

                val lineBefore = v.text.lastIndexOf('\n', startIndex = cursorPos - 2).let {
                    if (it == -1) 0 else it + 1
                }.let {
                    v.text.substring(it, cursorPos - 1)
                }

                val currentLine = v.text.indexOf('\n', startIndex = cursorPos).let {
                    if (it == -1) v.text.length else it
                }.let {
                    v.text.substring(cursorPos, it)
                }

                val res = analyzeListItemSafely(lineBefore)

                // remove empty list line
                if (currentLine.isBlank() && res?.shouldRemove() == true) {

                    val newPos = cursorPos - (lineBefore.length + 1)
                    return v.copy(
                        text = v.text.substring(0, newPos) + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(newPos)
                    )
                }

                // we are in a list
                // add a new empty similar list line
                if (res != null) {
                    val newText = res.text()
                    return v.copy(
                        text = v.text.substring(
                            0,
                            cursorPos
                        ) + res.padding + newText + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + res.padding.length + newText.length)
                    )
                }
                // no list found, but we can still add the padding
                else {
                    val padding = getPadding(lineBefore)
                    if (padding != null) {
                        return v.copy(
                            text = v.text.substring(0, cursorPos) + padding + v.text.substring(
                                cursorPos,
                                v.text.length
                            ),
                            selection = TextRange(cursorPos + padding.length)
                        )
                    }
                }
            } else {
                // remove padding, under certain conditions
                if (prev.text.length == v.text.length + 1) {

                    val start = v.text.lastIndexOf('\n', startIndex = cursorPos - 1).let {
                        if (it == -1) 0 else it + 1
                    }

                    val currentLine = v.text.substring(start, cursorPos)

                    if (currentLine.isBlank() && (prev.text[cursorPos] == ' ' || prev.text[cursorPos] == '\t')) {
                        return v.copy(
                            text = v.text.substring(0, start) + v.text.substring(
                                cursorPos,
                                v.text.length
                            ),
                            selection = TextRange(cursorPos - (cursorPos - start))
                        )
                    }

                }
            }
        }
    }

    return v
}

sealed class ListType {
    object Dash : ListType()
    object Asterisk : ListType()
    data class Number(val number: Int) : ListType()
}

data class ListItemInfo(
    val listType: ListType,
    val isTaskList: Boolean,
    val padding: String,
    val title: String?,
) {

    fun text(): String {
        val text = when (this.listType) {
            ListType.Asterisk -> "* "
            ListType.Dash -> "- "
            is ListType.Number -> "${this.listType.number + 1}. "
        }

        return if (this.isTaskList) {
            "$text[ ] "
        } else {
            text
        }
    }

    fun shouldRemove(): Boolean {
        return title?.isNotBlank() != true
    }
}

fun analyzeListItemSafely(line: String): ListItemInfo? {
    return try {
        analyzeListItem(line)
    } catch (e: Exception) {
        Log.d(TAG, "$e")
        null
    }
}

fun analyzeListItem(line: String): ListItemInfo? {
    val regex = Regex("""^(\s*)(?:(-)|(\*)|(\d+)\.)\s(?:\[([ xX])]\s)?(.+)?""")
    val match = regex.matchEntire(line) ?: return null

    val padding = match.groups[1]?.value ?: throw Exception("padding null: $line")

    val listType = when {
        match.groups[2]?.value != null -> ListType.Dash
        match.groups[3]?.value != null -> ListType.Asterisk
        match.groups[4]?.value != null -> match.groups[4]?.value?.toInt()?.let {
            ListType.Number(it)
        }

        else -> null
    }

    if (listType == null) {
        throw Exception("listType is null but we have a match: $line")
    }

    val isTaskList = match.groups[5] != null


    val title = match.groups[6]?.value

    return ListItemInfo(listType, isTaskList, padding, title)
}

fun getPadding(line: String): String? {
    val match = Regex("^\\s+").find(line)
    return match?.value
}


fun onTitle(v: TextFieldValue): TextFieldValue {
    val cursorPosMin = v.selection.min

    val pattern = "### "
    return if (v.selection.collapsed) {
        // check if line start with "### "
        val start = v.text.lastIndexOf('\n', startIndex = cursorPosMin - 1).let {
            if (it == -1) 0 else it + 1
        }

        // remove it
        if (v.text.substring(start).startsWith(pattern)) {
            v.copy(
                text = v.text.substring(0, start) + v.text.substring(
                    start + pattern.length,
                    v.text.length
                ),
                selection = TextRange(cursorPosMin - pattern.length)
            )
        }
        // add it
        else {
            v.copy(
                text = v.text.substring(0, start) + pattern + v.text.substring(
                    start,
                    v.text.length
                ),
                selection = TextRange(cursorPosMin + pattern.length)
            )
        }
    } else {
        // check if the text before cursorPosStart is "### "

        // remove it
        if (v.text.substring(0, cursorPosMin).endsWith(pattern)) {
            v.copy(
                text = v.text.substring(0, cursorPosMin - pattern.length) + v.text.substring(
                    cursorPosMin,
                    v.text.length
                ),
                selection = TextRange(
                    start = v.selection.start - pattern.length,
                    end = v.selection.end - pattern.length,
                )
            )
        }
        // add it
        else {
            v.copy(
                text = v.text.substring(0, cursorPosMin) + pattern + v.text.substring(
                    cursorPosMin,
                    v.text.length
                ),
                selection = TextRange(
                    start = v.selection.start + pattern.length,
                    end = v.selection.end + pattern.length,
                )
            )
        }
    }
}


fun addOrRemovePatternAtTheExtremitiesOfSelection(
    v: TextFieldValue,
    startPattern: String,
    endPattern: String
): TextFieldValue {
    val cursorPosMin = v.selection.min
    val cursorPosMax = v.selection.max

    // if already present, remove it
    return if (v.text.substring(0, cursorPosMin).endsWith(startPattern)
        && v.text.substring(cursorPosMax, v.text.length).startsWith(endPattern)
    ) {
        v.copy(
            text = v.text.substring(0, cursorPosMin - startPattern.length)
                    + v.text.substring(cursorPosMin, cursorPosMax)
                    + v.text.substring(cursorPosMax + endPattern.length, v.text.length),
            selection = TextRange(
                start = v.selection.start - startPattern.length,
                end = v.selection.end - startPattern.length,
            )
        )
    }
    // else, add it
    else {
        v.copy(
            text = v.text.substring(0, cursorPosMin)
                    + startPattern + v.text.substring(cursorPosMin, cursorPosMax) + endPattern
                    + v.text.substring(cursorPosMax, v.text.length),
            selection = TextRange(
                start = v.selection.start + startPattern.length,
                end = v.selection.end + startPattern.length,
            )
        )

    }
}

fun addOrRemovePatternAtTheExtremitiesOfSelection(
    v: TextFieldValue,
    pattern: String
): TextFieldValue {
    return addOrRemovePatternAtTheExtremitiesOfSelection(v, pattern, pattern)
}

fun onCode(v: TextFieldValue): TextFieldValue {
    val cursorPosMin = v.selection.min
    val cursorPosMax = v.selection.max

    return if (v.text.substring(cursorPosMin, cursorPosMax).contains('\n')) {
        addOrRemovePatternAtTheExtremitiesOfSelection(v, "```\n", "\n```")
    } else {
        addOrRemovePatternAtTheExtremitiesOfSelection(v, "`")
    }
}

fun onQuote(v: TextFieldValue): TextFieldValue {
    val cursorPosMin = if (v.text.getOrNull(v.selection.min) == '\n') {
        // if we are at the end of a line, decrement the cursor to include the line
        v.selection.min - 1
    } else {
        v.selection.min
    }

    val start = v.text.lastIndexOf('\n', startIndex = cursorPosMin).let {
        if (it == -1) 0 else it + 1
    }

    val cursorPosMax = v.selection.max
    val end = v.text.indexOf('\n', startIndex = cursorPosMax).let {
        if (it == -1) v.text.length else it
    }

    val subString = v.text.substring(start, end)

    val pattern = "> "

    val countOfNewLine = subString.count { it == '\n' }
    val countOfQuote = subString.split("\n$pattern").size - 1

    // each line start with the pattern, remove it
    return if (subString.startsWith(pattern) && countOfNewLine == countOfQuote) {
        v.copy(
            text = v.text.substring(0, start)
                    + v.text.substring(start + pattern.length, end).replace("\n> ", "\n")
                    + v.text.substring(end, v.text.length),
            selection = if (v.selection.reversed) TextRange(
                start = v.selection.start - (pattern.length + countOfNewLine * pattern.length),
                end = v.selection.end - pattern.length,
            ) else TextRange(
                start = v.selection.start - pattern.length,
                end = v.selection.end - (pattern.length + countOfNewLine * pattern.length),
            )
        )
    }
    // add the pattern to each line
    else {
        v.copy(
            text = v.text.substring(0, start)
                    + pattern
                    + v.text.substring(start, end).replace("\n", "\n> ")
                    + v.text.substring(end, v.text.length),
            selection = if (v.selection.reversed) TextRange(
                start = v.selection.start + pattern.length + countOfNewLine * pattern.length,
                end = v.selection.end + pattern.length,
            ) else TextRange(
                start = v.selection.start + pattern.length,
                end = v.selection.end + pattern.length + countOfNewLine * pattern.length,
            )
        )
    }
}