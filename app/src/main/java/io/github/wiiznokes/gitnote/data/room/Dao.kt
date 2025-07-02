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
    suspend fun clearAndInit(rootPath: String, timestamps: HashMap<String, Long>) {
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

                        val relativePath = nodeFs.path.substring(startIndex = rootLength)
                        val note = Note.new(
                            relativePath = relativePath,
                            lastModifiedTimeMillis = timestamps.get(relativePath) ?: nodeFs.lastModifiedTime().toMillis(),
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

    /**
     * Delete all notes inside the note folder, and the note folder
     */
    suspend fun deleteNoteFolder(noteFolder: NoteFolder) {
        internalDeleteNotesIn(noteFolder.relativePath + '/')
        internalDeleteNoteFolder(noteFolder)
    }

    /**
     * Private
     * Note: always add a '/' at the end of relativePath param
     */
    @Query("DELETE FROM Notes WHERE relativePath LIKE :relativePath || '%'")
    suspend fun internalDeleteNotesIn(relativePath: String)

    /**
     * Private
     */
    @Delete
    suspend fun internalDeleteNoteFolder(noteFolder: NoteFolder)

    @Upsert
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun removeNote(note: Note)

    @Query("DELETE  FROM NoteFolders")
    fun removeAllNoteFolder()

    @Query("DELETE  FROM Notes")
    fun removeAllNote()

    fun clearDatabase() {
        removeAllNoteFolder()
        removeAllNote()
    }
}