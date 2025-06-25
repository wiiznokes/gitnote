package io.github.wiiznokes.gitnote.ui.screen.app.edit

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


private const val TAG = "markdownSmartEditor"

val shouldRemoveLineRegex = Regex("""^\s*(?:[-*]|\d+\.|- \[[ x]])\s*$""")

fun markdownSmartEditor(
    prev: TextFieldValue,
    v: TextFieldValue
): TextFieldValue {

    // handle delete key when the line is:
    // - x
    //
    if (prev.text.length >= v.text.length) {
        return v
    }

    if (v.selection.start == v.selection.end) {
        val cursorPos = v.selection.start
        if (cursorPos > 0 && cursorPos <= v.text.length) {
            if (v.text[cursorPos - 1] == '\n') {

                val lineBefore = v.text.substring(0, cursorPos - 1).lastIndexOf('\n').let {
                    if (it == -1) 0 else it + 1
                }.let {
                    v.text.substring(it, cursorPos - 1)
                }

                val currentLine = v.text.indexOf('\n', startIndex = cursorPos).let {
                    if (it == -1) v.text.length else it
                }.let {
                    v.text.substring(cursorPos, it)
                }

                // remove
                if (currentLine.isEmpty() && shouldRemoveLineRegex.containsMatchIn(lineBefore)) {
                    val newPos = cursorPos - (lineBefore.length + 1)
                    return TextFieldValue(
                        text = v.text.substring(0, newPos) + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(newPos)
                    )
                }

                val res = analyzeListItem(lineBefore)

                // we are in a list
                if (res != null) {
                    val newText = res.text()
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos) + newText + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + newText.length)
                    )
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
    val isTaskList: Boolean
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
}

fun analyzeListItem(line: String): ListItemInfo? {
    val regex = Regex("""^\s*(?:(-)|(\*)|(\d+)\.)\s(?:\[([ xX])]\s)?(.+)?""")
    val match = regex.matchEntire(line) ?: return null

    val listType = when {
        match.groups[1]?.value != null -> ListType.Dash
        match.groups[2]?.value != null -> ListType.Asterisk
        match.groups[3]?.value != null -> match.groups[3]?.value?.toInt()?.let {
            ListType.Number(it)
        }
        else -> null
    }

    if (listType == null) {
        Log.w(TAG, "listType is null but we have a match")
        return null
    }

    val isTaskList = match.groups[4] != null

    return ListItemInfo(listType, isTaskList)
}
