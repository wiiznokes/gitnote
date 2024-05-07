package io.github.wiiznokes.gitnote.data.room

import android.util.Log
import android.webkit.MimeTypeMap
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import kotlinx.coroutines.flow.Flow

private const val TAG = "Dao"
@Dao
interface RepoDatabaseDao {

    // todo: use @Transaction
    // todo: don't clear the all database each time
    suspend fun clearAndInit(rootPath: String) {
        Log.d(TAG, "clearAndInit")
        clearDatabase()

        val rootFs = NodeFs.Folder.fromPath(rootPath)
        val rootFolder = NoteFolder(
            relativePath = "",
        )
        insertNoteFolder(rootFolder)

        val rootLength = rootFs.path.length + 1

        suspend fun initRec(folder: NodeFs.Folder) {

            folder.forEachNodeFs { nodeFs ->

                when (nodeFs) {
                    is NodeFs.File -> {
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(nodeFs.extension.text)
                        if (mimeType == null || !mimeType.startsWith("text")) {
                            //Log.d(TAG, "skipped ${nodeFs.path} with mime type $mimeType")
                            return@forEachNodeFs
                        }

                        val note = Note.new(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                            lastModifiedTimeMillis = nodeFs.lastModifiedTime().toMillis(),
                            content = nodeFs.readText(),
                        )
                        insertNote(note)
                        //Log.d(TAG, "add note: $note")
                    }

                    is NodeFs.Folder -> {
                        if (nodeFs.isHidden() || nodeFs.isSym()) {
                            return@forEachNodeFs
                        }
                        val noteFolder = NoteFolder(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                        )
                        //Log.d(TAG, "add noteFolder: $noteFolder")
                        insertNoteFolder(noteFolder)
                        initRec(nodeFs)
                    }
                }
            }
        }

        initRec(rootFs)
    }


    @Query("SELECT * FROM NoteFolders WHERE relativePath = ''")
    suspend fun rootNoteFolder(): NoteFolder

    @Query("SELECT * FROM NoteFolders")
    fun allNoteFolders(): Flow<List<NoteFolder>>


    @Query("SELECT * FROM Notes")
    fun allNotes(): Flow<List<Note>>


    @Upsert
    suspend fun insertNoteFolder(noteFolder: NoteFolder)


    // todo
    // suspend fun removeNoteFolder(noteFolder: NoteFolder)

    @Upsert
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun removeNote(note: Note)

    @Query("DELETE  FROM Notes WHERE relativePath = :relativePath")
    suspend fun removeNote(relativePath: String): Int


    @Query("DELETE  FROM NoteFolders")
    fun removeAllNoteFolder()

    @Query("DELETE  FROM Notes")
    fun removeAllNote()

    fun clearDatabase() {
        removeAllNoteFolder()
        removeAllNote()
    }
}