package io.github.wiiznokes.gitnote.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.zip.DataFormatException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


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
    lateinit var content: MutableState<TextFieldValue>
    lateinit var fileExtension: MutableState<FileExtension>

    private var isSaved: Boolean = false

    constructor(editType: EditType, previousNote: Note) : this() {
        this.editType = editType
        this.previousNote = previousNote

        this.name = previousNote.nameWithoutExtension().let {
            mutableStateOf(TextFieldValue(it, selection = TextRange(it.length)))
        }
        this.content = mutableStateOf(
            TextFieldValue(
                previousNote.content,
                selection = TextRange(0)
            )
        )
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
        this.editType = editType
        this.previousNote = previousNote
        this.name = mutableStateOf(TextFieldValue(name, selection = TextRange(name.length)))
        this.content = mutableStateOf(TextFieldValue(content, selection = TextRange(0)))
        this.fileExtension = mutableStateOf(fileExtension)

        Log.d(TAG, "init saved: $previousNote, $editType")
    }


    private val storageManager: StorageManager = MyApp.appModule.storageManager
    private val uiHelper: UiHelper = MyApp.appModule.uiHelper
    private val prefs = MyApp.appModule.appPreferences


    fun onFinish() {
        isSaved = true
    }

    fun onValidation(onSuccess: (() -> Unit)?) {

        when (editType) {
            EditType.Create -> create(
                parentPath = previousNote.parentPath(),
                name = name.value.text,
                fileExtension = fileExtension.value,
                content = content.value.text,
                id = previousNote.id
            ).onSuccess {
                if (onSuccess != null) {
                    onSuccess()
                } else {
                    editType = EditType.Update
                    previousNote = it
                }
            }

            EditType.Update -> update(
                previousNote = previousNote,
                parentPath = previousNote.parentPath(),
                name = name.value.text,
                fileExtension = fileExtension.value,
                content = content.value.text,
                id = previousNote.id
            ).onSuccess {
                if (onSuccess != null) {
                    onSuccess()
                } else {
                    previousNote = it
                }
            }
        }
    }

    /** Return early to note block the ui thread.
     * This is a best effort to catch problem
     */
    private fun update(
        previousNote: Note,
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

        prefs.repoPath.getBlocking().let { rootPath ->
            val previousFile = NodeFs.File.fromPath(rootPath, previousNote.relativePath)

            if (!previousFile.exist()) {
                Log.w(TAG, "previous file ${previousFile.path} does not exist")
            }

            val newFile = NodeFs.File.fromPath(rootPath, relativePath)

            if (newFile.path != previousFile.path) {
                if (newFile.exist()) {
                    uiHelper.makeToast(uiHelper.getString(R.string.error_file_already_exist))
                    return failure(EditException(EditExceptionType.NoteAlreadyExist))
                }
            }
        }

        val newNote = Note.new(
            relativePath = relativePath,
            content = content,
            id = id
        )

        CoroutineScope(Dispatchers.IO).launch {

            storageManager.updateNote(
                new = newNote,
                previous = previousNote
            ).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            uiHelper.makeToast("Note successfully updated")
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

        prefs.repoPath.getBlocking().let { rootPath ->
            if (NodeFs.File.fromPath(rootPath, relativePath).exist()) {
                uiHelper.makeToast("New note already exist")
                return failure(EditException(EditExceptionType.NoteAlreadyExist))
            }
        }

        val note = Note.new(
            relativePath = relativePath,
            content = content,
            id = id,
        )

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.createNote(note).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }
            uiHelper.makeToast("Note successfully created")
        }

        return success(note)
    }

    override fun onCleared() {

        fun isPreviousNoteTheSame(): Boolean =
            previousNote.nameWithoutExtension() == name.value.text
                    && previousNote.content == content.value.text
                    && previousNote.fileExtension().text == fileExtension.value.text


        if (isSaved || isPreviousNoteTheSame()) {
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
fun delegateEditVM(editType: EditType?, previousNote: Note?): EditViewModel {
    return if (editType != null && previousNote != null) {
        viewModel<EditViewModel>(
            factory = viewModelFactory {
                EditViewModel(editType, previousNote)
            }
        )
    } else {
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