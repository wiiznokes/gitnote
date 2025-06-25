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

                val res = try {
                    analyzeListItem(lineBefore)
                } catch (e: Exception) {
                    Log.d(TAG, "$e")
                    null
                }

                // we are in a list
                if (res != null) {
                    val newText = res.text()
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos) + res.padding + newText + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + res.padding.length + newText.length)
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
    val isTaskList: Boolean,
    val padding: String,
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

    return ListItemInfo(listType, isTaskList, padding)
}
