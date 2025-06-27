@file:Suppress("IfThenToElvis")

package io.github.wiiznokes.gitnote.ui.viewmodel.edit

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.max


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

                val res = ListItemInfo.parseSafely(lineBefore)

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
                    val newText = res.prefix(numberOp = { it + 1 })
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

    fun prefix(numberOp: (Int) -> Int = { it }): String {
        return when (this) {
            Asterisk -> "* "
            Dash -> "- "
            is Number -> "${numberOp(number)}. "
        }
    }
}

data class ListItemInfo(
    val listType: ListType = ListType.Dash,
    val isTaskList: Boolean = false,
    val isChecked: Boolean = false,
    val padding: String = "",
    val title: String? = null,
) {

    companion object {

        fun parseSafely(line: String): ListItemInfo? {
            return try {
                parse(line)
            } catch (e: Exception) {
                Log.d(TAG, "$e")
                null
            }
        }

        fun parse(line: String): ListItemInfo? {
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

            val isChecked = match.groups[5]?.value != " "

            val title = match.groups[6]?.value

            return ListItemInfo(
                listType = listType,
                isTaskList = isTaskList,
                isChecked = isChecked,
                padding = padding,
                title = title
            )
        }


        fun fromLineFallBack(line: String): ListItemInfo {
            return ListItemInfo(
                padding = getPadding(line) ?: ""
            )
        }

    }

    fun prefix(numberOp: (Int) -> Int = { it }): String {
        var text = listType.prefix(numberOp)
        if (this.isTaskList) {
            text += if (isChecked) "[x] " else "[ ] "
        }
        return text
    }

    fun line(numberOp: (Int) -> Int = { it }, minusPaddingInTitle: Boolean = false): String {
        var finalText = padding
        finalText += prefix(numberOp)

        if (title != null) {
            finalText += if (minusPaddingInTitle) {
                if (title.startsWith(padding))
                    title.substring(padding.length)
                else title
            } else title
        }

        return finalText
    }

    fun lineWithoutPrefix(): String {
        var finalText = padding
        finalText += title
        return finalText
    }

    fun shouldRemove(): Boolean {
        return title?.isNotBlank() != true
    }


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
        if (v.text.startsWith(pattern, startIndex = start)) {
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
        && v.text.startsWith(endPattern, startIndex = cursorPosMax)
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

fun onLink(v: TextFieldValue): TextFieldValue {
    val cursorPosMin = v.selection.min
    val cursorPosMax = v.selection.max

    val startPattern = "["
    val endPattern = "](url)"

    // remove url pattern
    return if (v.text.startsWith(startPattern, startIndex = cursorPosMin - startPattern.length)
        && v.text.startsWith(endPattern, startIndex = cursorPosMax)
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
    // add it
    else {
        v.copy(
            text = v.text.substring(0, cursorPosMin)
                    + startPattern
                    + v.text.substring(cursorPosMin, cursorPosMax)
                    + endPattern
                    + v.text.substring(cursorPosMax, v.text.length),
            selection = if (v.selection.collapsed) TextRange(
                start = v.selection.start + startPattern.length,
                end = v.selection.end + startPattern.length,
            ) else TextRange(
                start = cursorPosMax + 3,
                end = cursorPosMax + 6,
            )
        )
    }

}

fun Int.max(b: Int): Int = max(this, b)

/**
 * @param f1: this callback will be called on each line selected. Return true if you want to short circuit.
 * @param f2: this callback is also called on each line selected, after f1. Return the modified line
 */
private fun multiLinePrefixModifier(
    v: TextFieldValue,
    f1: (String) -> Boolean,
    f2: (String, Int) -> String
): TextFieldValue {
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

    val lines = subString.lines()

    for (line in lines) {
        if (f1(line)) break
    }

    var res = v.text.substring(0, start)

    var deltaAll = 0
    var deltaFirstLine = 0

    for (line in lines.withIndex()) {
        val newLine = f2(line.value, line.index + 1)
        val delta = newLine.length - line.value.length
        if (line.index == 0) {
            deltaFirstLine = delta
        }
        deltaAll += delta
        res += if (line.index == lines.size - 1) {
            newLine
        } else {
            "$newLine\n"
        }
    }

    res += v.text.substring(end, v.text.length)

    return v.copy(
        text = res,
        selection = if (v.selection.reversed) TextRange(
            start = (v.selection.start + deltaAll).max(start),
            end = (v.selection.end + deltaFirstLine).max(start),
        ) else TextRange(
            start = (v.selection.start + deltaFirstLine).max(start),
            end = (v.selection.end + deltaAll).max(start),
        )
    )
}

fun onQuote(v: TextFieldValue): TextFieldValue {

    var atListOneListToConvert = false
    val pattern = "> "

    return multiLinePrefixModifier(
        v = v,
        f1 = { line ->

            if (!line.startsWith(pattern)) {
                atListOneListToConvert = true
            }
            atListOneListToConvert
        },
        f2 = { line, lineNumber ->
            return@multiLinePrefixModifier if (atListOneListToConvert) {
                if (line.startsWith(pattern)) line else pattern + line
            } else {
                line.substring(startIndex = pattern.length)
            }
        }
    )
}


fun onUnorderedList(v: TextFieldValue): TextFieldValue {

    var atListOneListToConvert = false
    var defaultListInfo: ListItemInfo? = null

    return multiLinePrefixModifier(
        v = v,
        f1 = {
            val res = ListItemInfo.parseSafely(line = it)
            if (res == null) {
                atListOneListToConvert = true
            } else {
                if (defaultListInfo == null) {
                    defaultListInfo = res
                }
                if (res.listType != ListType.Dash) {
                    atListOneListToConvert = true
                }
            }
            atListOneListToConvert && defaultListInfo != null
        },
        f2 = { line, lineNumber ->

            val res = ListItemInfo.parseSafely(line)
            return@multiLinePrefixModifier if (res != null) {
                if (atListOneListToConvert) {
                    res.copy(listType = ListType.Dash).line()
                } else {
                    res.lineWithoutPrefix()
                }

            } else {
                if (defaultListInfo == null) {
                    defaultListInfo = ListItemInfo()
                }

                defaultListInfo.copy(
                    listType = ListType.Dash,
                    isChecked = false,
                    title = line,
                    padding = getPadding(line) ?: ""
                ).line(minusPaddingInTitle = true)
            }
        }
    )
}

fun onNumberedList(v: TextFieldValue): TextFieldValue {

    var atListOneListToConvert = false
    var defaultListInfo: ListItemInfo? = null

    return multiLinePrefixModifier(
        v = v,
        f1 = {
            val res = ListItemInfo.parseSafely(line = it)
            if (res == null) {
                atListOneListToConvert = true
            } else {
                if (defaultListInfo == null) {
                    defaultListInfo = res
                }
                if (res.listType !is ListType.Number) {
                    atListOneListToConvert = true
                }
            }
            atListOneListToConvert && defaultListInfo != null
        },
        f2 = { line, lineNumber ->

            val res = ListItemInfo.parseSafely(line)
            return@multiLinePrefixModifier if (res != null) {
                if (atListOneListToConvert) {
                    res.copy(listType = ListType.Number(lineNumber)).line()
                } else {
                    res.lineWithoutPrefix()
                }
            } else {
                if (defaultListInfo == null) {
                    defaultListInfo = ListItemInfo()
                }

                defaultListInfo.copy(
                    listType = ListType.Number(lineNumber),
                    isChecked = false,
                    title = line,
                    padding = getPadding(line) ?: ""
                ).line(minusPaddingInTitle = true)
            }
        }
    )
}

fun onTaskList(v: TextFieldValue): TextFieldValue {

    var atListOneListToConvert = false
    var defaultListInfo: ListItemInfo? = null

    return multiLinePrefixModifier(
        v = v,
        f1 = {
            val res = ListItemInfo.parseSafely(line = it)
            if (res == null) {
                atListOneListToConvert = true
            } else {
                if (defaultListInfo == null) {
                    defaultListInfo = res
                }
                if (!res.isTaskList) {
                    atListOneListToConvert = true
                }
            }
            atListOneListToConvert && defaultListInfo != null
        },
        f2 = { line, lineNumber ->

            val res = ListItemInfo.parseSafely(line)
            return@multiLinePrefixModifier if (res != null) {
                if (atListOneListToConvert) {
                    res.copy(
                        isTaskList = true,
                    ).line(numberOp = { lineNumber })
                } else {
                    res.lineWithoutPrefix()
                }

            } else {
                if (defaultListInfo == null) {
                    defaultListInfo = ListItemInfo()
                }

                defaultListInfo.copy(
                    isTaskList = true,
                    isChecked = false,
                    title = line,
                    padding = getPadding(line) ?: ""
                ).line(numberOp = { lineNumber }, minusPaddingInTitle = true)
            }
        }
    )
}