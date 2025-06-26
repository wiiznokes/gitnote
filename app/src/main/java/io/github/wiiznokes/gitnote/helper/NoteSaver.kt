package io.github.wiiznokes.gitnote.helper

import android.content.Context
import android.util.Log
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.model.EditType
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

private const val TAG = "NoteSaver"

private const val EDIT_IS_UNSAVED = "EDIT_IS_UNSAVED"

private const val EDIT_EDIT_TYPE = "EDIT_EDIT_TYPE"
private const val EDIT_PREVIOUS_NOTE = "EDIT_PREVIOUS_NOTE"
private const val EDIT_NAME = "EDIT_NAME"
private const val EDIT_CONTENT = "EDIT_CONTENT"


class NoteSaver {
    companion object {

        fun save(
            shouldSave: Boolean,
            name: String,
            content: String,
            previousNote: Note,
            editType: EditType
        ) {
            try {
                if (shouldSave) {
                    Log.d(TAG, "EDIT_IS_UNSAVED")
                    writeObj(EDIT_IS_UNSAVED, true)
                    writeObj(EDIT_NAME, name)
                    writeObj(EDIT_CONTENT, content)
                    writeObj(EDIT_PREVIOUS_NOTE, previousNote)
                    writeObj(EDIT_EDIT_TYPE, editType)
                } else {
                    writeObj(EDIT_IS_UNSAVED, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getSaveState(): SaveInfo? {
            return try {
                SaveInfo(
                    editType = readObj(EDIT_EDIT_TYPE),
                    previousNote = readObj(EDIT_PREVIOUS_NOTE),
                    name = readObj(EDIT_NAME),
                    content = readObj(EDIT_CONTENT),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }


        fun isEditUnsaved(): Boolean {
            return try {
                readObj(EDIT_IS_UNSAVED)
            } catch (_: Exception) {
                false
            }
        }
    }
}

fun save(
    shouldSave: Boolean,
    name: String,
    content: String,
    previousNote: Note,
    editType: EditType
) {
    try {
        if (shouldSave) {
            writeObj(EDIT_IS_UNSAVED, true)
            writeObj(EDIT_NAME, name)
            writeObj(EDIT_CONTENT, content)
            writeObj(EDIT_PREVIOUS_NOTE, previousNote)
            writeObj(EDIT_EDIT_TYPE, editType)
        } else {
            writeObj(EDIT_IS_UNSAVED, false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

data class SaveInfo(
    val name: String,
    val content: String,
    val previousNote: Note,
    val editType: EditType
)


private fun <T : Serializable> writeObj(path: String, obj: T) {
    val fileOutputStream =
        MyApp.appModule.context.openFileOutput(path, Context.MODE_PRIVATE)
    val objectOutputStream = ObjectOutputStream(fileOutputStream)
    objectOutputStream.writeObject(obj)
    objectOutputStream.close()
    fileOutputStream.close()
}


private fun <T> readObj(path: String): T {
    val fileInputStream = MyApp.appModule.context.openFileInput(path)
    val objectInputStream = ObjectInputStream(fileInputStream)
    @Suppress("UNCHECKED_CAST") val res = objectInputStream.readObject() as T
    objectInputStream.close()
    fileInputStream.close()
    return res
}