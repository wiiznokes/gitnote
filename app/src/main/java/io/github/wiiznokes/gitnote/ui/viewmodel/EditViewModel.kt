package io.github.wiiznokes.gitnote.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.screen.app.edit.markdownSmartEditor
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

class EditViewModel() : ViewModel() {

    companion object {
        private const val TAG = "EditViewModel"
    }

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

    lateinit var fileExtension: MutableState<FileExtension>

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

        this.fileExtension = mutableStateOf(previousNote.fileExtension())

        Log.d(TAG, "init: $previousNote, $editType")
    }

    constructor(
        editType: EditType,
        previousNote: Note,
        name: String,
        content: String,
        fileExtension: FileExtension,
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
        this.fileExtension = mutableStateOf(fileExtension)

        Log.d(TAG, "init saved: $previousNote, $editType")
    }

    fun onValueChange(v: TextFieldValue) {
        // don't bloat history with different selection
        if (content.value.text == v.text) {
            viewModelScope.launch { _content.emit(v.copy()) }
            return
        }

        val newValue = if (fileExtension.value is FileExtension.Md) {
            markdownSmartEditor(content.value, v)
        } else {
            v
        }
        viewModelScope.launch { _content.emit(newValue.copy()) }

        val historyManager = historyManager.value

        var i = (history.size - 1) - historyManager.index
        while (i > 0) {
            history.removeAt(history.lastIndex)
            i--
        }

        history.add(newValue)
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
                fileExtension = fileExtension.value,
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
                fileExtension = fileExtension.value,
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
                && previousNote.fileExtension().text == fileExtension.value.text

    override fun onCleared() {


        if (!shouldSaveWhenQuitting || isPreviousNoteTheSame()) {
            writeObj(EDIT_IS_UNSAVED, false)
            return
        }

        writeObj(EDIT_IS_UNSAVED, true)
        Log.d(TAG, "EDIT_IS_UNSAVED")
        writeObj(EDIT_NAME, name.value.text)
        writeObj(EDIT_CONTENT, content.value.text)
        writeObj(EDIT_FILE_EXTENSION, fileExtension.value)
        writeObj(EDIT_PREVIOUS_NOTE, previousNote)
        writeObj(EDIT_EDIT_TYPE, editType)
    }
}

private const val EDIT_IS_UNSAVED = "EDIT_IS_UNSAVED"

private const val EDIT_EDIT_TYPE = "EDIT_EDIT_TYPE"
private const val EDIT_PREVIOUS_NOTE = "EDIT_PREVIOUS_NOTE"
private const val EDIT_NAME = "EDIT_NAME"
private const val EDIT_CONTENT = "EDIT_CONTENT"
private const val EDIT_FILE_EXTENSION = "EDIT_FILE_EXTENSION"

fun isEditUnsaved(): Boolean {
    return try {
        readObj(EDIT_IS_UNSAVED)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun newEditViewModel(editParams: EditParams): EditViewModel {

    return when (editParams) {
        is EditParams.Idle -> viewModel<EditViewModel>(
            factory = viewModelFactory {
                EditViewModel(editParams.editType, editParams.note)
            }
        )

        EditParams.Saved -> {
            viewModel<EditViewModel>(
                factory = viewModelFactory {
                    EditViewModel(
                        editType = readObj(EDIT_EDIT_TYPE),
                        previousNote = readObj(EDIT_PREVIOUS_NOTE),
                        name = readObj(EDIT_NAME),
                        content = readObj(EDIT_CONTENT),
                        fileExtension = readObj(EDIT_FILE_EXTENSION),
                    )
                }
            )
        }
    }
}

private fun <T : Serializable> writeObj(path: String, obj: T) {
    try {
        val fileOutputStream =
            MyApp.appModule.context.openFileOutput(path, Context.MODE_PRIVATE)
        val objectOutputStream = ObjectOutputStream(fileOutputStream)
        objectOutputStream.writeObject(obj)
        objectOutputStream.close()
        fileOutputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


private fun <T> readObj(path: String): T {
    val fileInputStream = MyApp.appModule.context.openFileInput(path)
    val objectInputStream = ObjectInputStream(fileInputStream)
    @Suppress("UNCHECKED_CAST") val res = objectInputStream.readObject() as T
    objectInputStream.close()
    fileInputStream.close()
    return res
}