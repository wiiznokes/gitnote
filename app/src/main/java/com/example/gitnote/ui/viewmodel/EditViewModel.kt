package com.example.gitnote.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.example.gitnote.MyApp
import com.example.gitnote.R
import com.example.gitnote.data.platform.NodeFs
import com.example.gitnote.data.room.Note
import com.example.gitnote.helper.NameValidation
import com.example.gitnote.helper.UiHelper
import com.example.gitnote.manager.StorageManager
import com.example.gitnote.ui.model.EditType
import com.example.gitnote.ui.model.FileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.zip.DataFormatException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


enum class EditExceptionType {
    NoteAlreadyExist,
}

class EditException(
    val type: EditExceptionType,
) : Exception(type.name)

class EditViewModel(
    var editType: EditType,
    private var previousNote: Note,
    ) : ViewModel() {


    init {
        Log.d(TAG, "init: $previousNote, $editType")
    }

    val name = previousNote.nameWithoutExtension().let {
        mutableStateOf(TextFieldValue(it, selection = TextRange(it.length)))
    }

    val content = mutableStateOf(
        TextFieldValue(
            previousNote.content,
            selection = TextRange(0)
        )
    )

    val fileExtension = mutableStateOf(previousNote.fileExtension())

    companion object {
        private const val TAG = "EditViewModel"
    }


    private val storageManager: StorageManager = MyApp.appModule.storageManager

    private val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val prefs = MyApp.appModule.appPreferences


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
            uiHelper.makeToast(uiHelper.getString(R.string.invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(fileExtension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.invalid_extension))
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
                    uiHelper.makeToast("New file already exist")
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
            uiHelper.makeToast(uiHelper.getString(R.string.invalid_name))
            return failure(DataFormatException("name invalid: $name"))
        }

        if (!NameValidation.check(fileExtension.text)) {
            uiHelper.makeToast(uiHelper.getString(R.string.invalid_extension))
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




}