package io.github.wiiznokes.gitnote.ui.viewmodel.edit

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.helper.NoteSaver
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.zip.DataFormatException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

data class History(
    val index: Int,
    val size: Int,
)

enum class EditExceptionType {
    NoteAlreadyExist,
}

class EditException(
    val type: EditExceptionType,
) : Exception(type.name)

private const val TAG = "TextVM"

open class TextVM() : ViewModel() {

    lateinit var editType: EditType
    private lateinit var previousNote: Note
    lateinit var name: MutableState<TextFieldValue>

    private val _content: MutableStateFlow<TextFieldValue> = MutableStateFlow(TextFieldValue())
    val content: StateFlow<TextFieldValue>
        get() = _content.asStateFlow()

    private val history = mutableListOf<TextFieldValue>()

    private val _historyManager: MutableStateFlow<History> = MutableStateFlow(History(index = 0, size = 1))
    val historyManager: StateFlow<History>
        get() = _historyManager.asStateFlow()


    var shouldSaveWhenQuitting: Boolean = true

    val shouldForceNotReadOnlyMode: MutableState<Boolean> = mutableStateOf(false)

    constructor(editType: EditType, previousNote: Note) : this() {

        shouldForceNotReadOnlyMode.value = editType == EditType.Create

        this.editType = editType
        this.previousNote = previousNote

        this.name = previousNote.nameWithoutExtension().let {
            mutableStateOf(TextFieldValue(it, selection = TextRange(it.length)))
        }

        val textFieldValue = TextFieldValue(
            previousNote.content,
            selection = TextRange(0)
        )

        viewModelScope.launch { _content.emit(textFieldValue.copy()) }
        history.add(textFieldValue)

        Log.d(TAG, "init: $previousNote, $editType")
    }

    constructor(
        editType: EditType,
        previousNote: Note,
        name: String,
        content: String,
    ) : this() {

        shouldForceNotReadOnlyMode.value = editType == EditType.Create

        this.editType = editType
        this.previousNote = previousNote
        this.name = mutableStateOf(TextFieldValue(name, selection = TextRange(name.length)))
        val textFieldValue = TextFieldValue(
            content,
            selection = TextRange(0)
        )
        viewModelScope.launch { _content.emit(textFieldValue.copy()) }
        history.add(textFieldValue)

        Log.d(TAG, "init saved: $previousNote, $editType")
    }

    open fun onValueChange(v: TextFieldValue) {

        // don't bloat history with different selection
        if (content.value.text == v.text) {
            viewModelScope.launch { _content.emit(v.copy()) }
            return
        }


        viewModelScope.launch { _content.emit(v.copy()) }

        val historyManager = historyManager.value

        var i = (history.size - 1) - historyManager.index
        while (i > 0) {
            history.removeAt(history.lastIndex)
            i--
        }

        history.add(v)
        viewModelScope.launch {
            _historyManager.emit(History(
                size = history.size,
                index = historyManager.index + 1
            ))
        }
    }


    fun undo() {
        val historyManager = historyManager.value
        viewModelScope.launch {
            _historyManager.emit(History(
                size = historyManager.size,
                index = historyManager.index - 1
            ))
        }
        viewModelScope.launch { _content.emit(history[historyManager.index - 1].copy()) }
    }

    fun redo() {
        val historyManager = historyManager.value
        viewModelScope.launch {
            _historyManager.emit(History(
                size = historyManager.size,
                index = historyManager.index + 1
            ))
        }
        viewModelScope.launch { _content.emit(history[historyManager.index + 1].copy()) }
    }

    fun setReadOnlyMode(value: Boolean) {
        shouldForceNotReadOnlyMode.value = false

        viewModelScope.launch {
            prefs.isReadOnlyModeActive.update(value)
        }
    }

    private val storageManager: StorageManager = MyApp.appModule.storageManager
    private val uiHelper: UiHelper = MyApp.appModule.uiHelper
    val prefs = MyApp.appModule.appPreferences

    fun save(onSuccess: () -> Unit = {}) {

        if (isPreviousNoteTheSame()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_note_is_the_same))
            onSuccess()
            return
        }

        when (editType) {
            EditType.Create -> create(
                parentPath = previousNote.parentPath(),
                name = name.value.text,
                fileExtension = previousNote.fileExtension(),
                content = content.value.text,
                id = previousNote.id
            ).onSuccess {
                editType = EditType.Update
                previousNote = it
                onSuccess()
            }

            EditType.Update -> update(
                previousNote = previousNote,
                parentPath = previousNote.parentPath(),
                name = name.value.text,
                fileExtension = previousNote.fileExtension(),
                content = content.value.text,
            ).onSuccess {
                previousNote = it
                onSuccess()
            }
        }
    }

    /** Return early to not block the ui thread.
     * This is a best effort to catch problem
     */
    private fun update(
        previousNote: Note,
        parentPath: String,
        name: String,
        fileExtension: FileExtension,
        content: String,
    ): Result<Note> {

        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(fileExtension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_extension))
            return failure(DataFormatException("extension invalid: $name"))
        }

        val relativePath = "$parentPath/$name.${fileExtension.text}"

        val newNote = Note.new(
            relativePath = relativePath,
            content = content,
        )

        prefs.repoPathBlocking().let { repoPath ->
            val previousFile = previousNote.toFileFs(repoPath)
            if (!previousFile.exist()) {
                Log.w(TAG, "previous file ${previousFile.path} does not exist")
            }

            val newFile = newNote.toFileFs(repoPath)
            if (newFile.path != previousFile.path) {
                if (newFile.exist()) {
                    uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
                    return failure(EditException(EditExceptionType.NoteAlreadyExist))
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {

            storageManager.updateNote(
                new = newNote,
                previous = previousNote
            ).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            uiHelper.makeToast(uiHelper.getString(R.string.success_note_update))
        }
        return success(newNote)
    }

    /** Return early to note block the ui thread.
     * This is a best effort to catch problem
     */
    private fun create(
        parentPath: String,
        name: String,
        fileExtension: FileExtension,
        content: String,
        id: Int
    ): Result<Note> {


        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(fileExtension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_extension))
            return failure(DataFormatException("extension invalid: $name"))
        }

        val relativePath = "$parentPath/$name.${fileExtension.text}"

        val note = Note.new(
            relativePath = relativePath,
            content = content,
            id = id,
        )

        if (note.toFileFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
            return failure(EditException(EditExceptionType.NoteAlreadyExist))
        }

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.createNote(note).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }
            uiHelper.makeToast(uiHelper.getString(R.string.success_note_create))
        }

        return success(note)
    }

    private fun isPreviousNoteTheSame(): Boolean =
        previousNote.nameWithoutExtension() == name.value.text
                && previousNote.content == content.value.text

    override fun onCleared() {
        NoteSaver.save(
            shouldSave = shouldSaveWhenQuitting && !isPreviousNoteTheSame(),
            name = name.value.text,
            content = content.value.text,
            previousNote = previousNote,
            editType = editType
        )
    }
}


@Composable
fun newEditViewModel(editParams: EditParams): TextVM {

    return when (editParams) {
        is EditParams.Idle -> viewModel<TextVM>(
            factory = viewModelFactory {
                TextVM(editParams.editType, editParams.note)
            }
        )

        is EditParams.Saved -> {
            viewModel<TextVM>(
                factory = viewModelFactory {
                    TextVM(
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

