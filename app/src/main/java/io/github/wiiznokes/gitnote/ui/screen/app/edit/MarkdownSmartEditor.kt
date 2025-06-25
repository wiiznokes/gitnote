package io.github.wiiznokes.gitnote.ui.screen.app.edit

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


private const val TAG = "markdownSmartEditor"

val shouldRemoveLineRegex = Regex("""^\s*(?:[-*]|\.\d+|- \[[ x]])\s*$""")

fun markdownSmartEditor(
    prev: TextFieldValue,
    v: TextFieldValue
): TextFieldValue {
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

                if (currentLine.isNotEmpty()) {
                    return v
                }

                // handle delete key when the line is:
                // - x
                //
                if (prev.text.length >= v.text.length) {
                    return v
                }

                // remove
                if (shouldRemoveLineRegex.containsMatchIn(lineBefore)) {
                    Log.d(TAG, "remove: $lineBefore")
                    val newPos = cursorPos - (lineBefore.length + 1)
                    return TextFieldValue(
                        text = v.text.substring(0, newPos) + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(newPos)
                    )
                }

                // add a new line
                if (lineBefore.startsWith("- ")) {
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos) + "- " + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + 2)
                    )
                }

                if (lineBefore.startsWith("* ")) {
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos) + "* " + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + 2)
                    )
                }
            }
        }
    }
    return v
}



