package com.example.gitnote.data.room

import android.webkit.MimeTypeMap
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.gitnote.data.platform.FileFs
import com.example.gitnote.data.platform.FolderFs
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDatabaseDao {

    // todo: use @Transaction
    // todo: don't clear the all database each time
    suspend fun clearAndInit(rootPath: String) {
        clearDatabase()

        val rootFs = FolderFs.fromPath(rootPath)
        val rootFolder = NoteFolder(
            relativePath = "",
        )
        insertNoteFolder(rootFolder)

        val rootLength = rootFs.path.length + 1

        suspend fun initRec(folderFs: FolderFs) {

            folderFs.forEachNodeFs { nodeFs ->

                when (nodeFs) {
                    is FileFs -> {
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(nodeFs.extension.text)
                        if (mimeType == null || !mimeType.startsWith("text")) {
                            //Log.d(TAG, "skipped ${nodeFs.path} with mime type $mimeType")
                            return@forEachNodeFs
                        }

                        val note = Note(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                            content = nodeFs.readText(),
                        )
                        insertNote(note)
                        //Log.d(TAG, "add note: $note")
                    }

                    is FolderFs -> {
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

    // todo: maybe remove all @Insert function because
    //  they are not safe to use. Imagine you want to create
    //  a new note. The note could exist on remote, get pulled
    //  and then loaded into the database (this is the logic of
    //  create note), so this function would fail, while an upsert
    //  would have been acceptable.
    //  The project should stabilize before doing this tho
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