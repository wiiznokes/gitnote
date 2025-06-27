package io.github.wiiznokes.gitnote.ui.viewmodel.edit

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory

private const val TAG = "MarkDownVM"


class MarkDownVM : TextVM {

    constructor(editType: EditType, previousNote: Note) : super(editType, previousNote)

    constructor(
        editType: EditType,
        previousNote: Note,
        name: String,
        content: String,
    ) : super(editType, previousNote, name, content)

    override fun onValueChange(v: TextFieldValue) {
        val newValue = markdownSmartEditor(content.value, v)
        super.onValueChange(newValue)
    }

    fun onTitle() {
        val newValue = onTitle(content.value)
        super.onValueChange(newValue)
    }

    fun onBold() {
        val newValue = addOrRemovePatternAtTheExtremitiesOfSelection(content.value, "**")
        super.onValueChange(newValue)
    }

    fun onItalic() {
        val newValue = addOrRemovePatternAtTheExtremitiesOfSelection(content.value, "_")
        super.onValueChange(newValue)
    }

    fun onCode() {
        val newValue = onCode(content.value)
        super.onValueChange(newValue)
    }

    fun onQuote() {
        val newValue = onQuote(content.value)
        //Log.d(TAG, "onQuote result: text=\"${v.text.replace("\n", "\\n")}\", start=${v.selection.start}, end=${v.selection.end}")
        super.onValueChange(newValue)
    }

    fun onLink() {
        val newValue = onLink(content.value)
        super.onValueChange(newValue)
    }
}


@Composable
fun newMarkDownVM(editParams: EditParams): MarkDownVM {

    return when (editParams) {
        is EditParams.Idle -> viewModel<MarkDownVM>(
            factory = viewModelFactory {
                MarkDownVM(editParams.editType, editParams.note)
            }
        )

        is EditParams.Saved -> {
            viewModel<MarkDownVM>(
                factory = viewModelFactory {
                    MarkDownVM(
                        editType = editParams.editType,
                        previousNote = editParams.note,
                        name = editParams.name,
                        content = editParams.content,
                    )
                }
            )
        }
    }
}
