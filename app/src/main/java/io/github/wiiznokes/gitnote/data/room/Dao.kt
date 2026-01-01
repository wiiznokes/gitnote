package io.github.wiiznokes.gitnote.data.room

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.manager.Progress
import io.github.wiiznokes.gitnote.manager.isExtensionSupported
import io.github.wiiznokes.gitnote.ui.model.FolderDisplayMode
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.ui.screen.app.DrawerFolderModel
import io.requery.android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.nio.ByteOrder


private const val TAG = "Dao"

private const val LIMIT_FILE_SIZE_DB = 2 * 1024 * 1024

@Dao
interface RepoDatabaseDao {

    // todo: use @Transaction
    // todo: don't clear the all database each time
    suspend fun clearAndInit(
        rootPath: String,
        timestamps: HashMap<String, Long>,
        progressCb: ((Progress) -> Unit)? = null
    ) {
        Log.d(TAG, "clearAndInit")
        clearDatabase()

        val rootFs = NodeFs.Folder.fromPath(rootPath)
        val rootFolder = NoteFolder.new(
            relativePath = "",
        )
        insertNoteFolder(rootFolder)

        val rootLength = rootFs.path.length + 1

        suspend fun initRec(folder: NodeFs.Folder) {

            folder.forEachNodeFs { nodeFs ->

                when (nodeFs) {
                    is NodeFs.File -> {
                        if (!isExtensionSupported(nodeFs.extension.text)) {
                            //Log.d(TAG, "skipped ${nodeFs.path} because extension not supported")
                            return@forEachNodeFs
                        }

                        val fileSize = nodeFs.fileSize()
                        if (fileSize > LIMIT_FILE_SIZE_DB) {
                            Log.d(
                                TAG,
                                "skipped ${nodeFs.path} because size was above $LIMIT_FILE_SIZE_DB ($fileSize)"
                            )
                            return@forEachNodeFs
                        }

                        val relativePath = nodeFs.path.substring(startIndex = rootLength)
                        val note = Note.new(
                            relativePath = relativePath,
                            lastModifiedTimeMillis = timestamps.get(relativePath)
                                ?: nodeFs.lastModifiedTime().toMillis(),
                            content = nodeFs.readText(),
                        )
                        insertNote(note)
                        //Log.d(TAG, "add note: $note")
                    }

                    is NodeFs.Folder -> {
                        if (nodeFs.isHidden() || nodeFs.isSym()) {
                            return@forEachNodeFs
                        }
                        val noteFolder = NoteFolder.new(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                        )
                        //Log.d(TAG, "add noteFolder: $noteFolder")
                        insertNoteFolder(noteFolder)
                        progressCb?.invoke(Progress.GeneratingDatabase(noteFolder.relativePath))
                        initRec(nodeFs)
                    }
                }
            }
        }

        initRec(rootFs)
    }


    @Query("SELECT * FROM NoteFolders WHERE relativePath = ''")
    suspend fun rootNoteFolder(): NoteFolder

    @Query(
        """
    SELECT EXISTS(
        SELECT 1 FROM Notes WHERE relativePath = :relativePath
    )
    """
    )
    suspend fun isNoteExist(relativePath: String): Boolean

    @Query("SELECT * FROM Notes")
    fun allNotes(): Flow<List<Note>>

    @RawQuery(observedEntities = [Note::class])
    fun gridNotesRaw(query: SupportSQLiteQuery): PagingSource<Int, GridNote>

    fun gridNotes(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
        folderDisplayMode: FolderDisplayMode,
        tag: String? = null,
    ): PagingSource<Int, GridNote> {

        val (sortColumn, order) = when (sortOrder) {
            SortOrder.AZ -> "fileName" to "ASC"
            SortOrder.ZA -> "fileName" to "DESC"
            SortOrder.MostRecent -> "lastModifiedTimeMillis" to "DESC"
            SortOrder.Oldest -> "lastModifiedTimeMillis" to "ASC"
        }

        val whereClause = when (folderDisplayMode) {
            FolderDisplayMode.CurrentFolderOnly -> {
                if (currentNoteFolderRelativePath.isEmpty()) {
                    "WHERE relativePath NOT LIKE '%/%'"
                } else {
                    "WHERE relativePath LIKE :currentNoteFolderRelativePath || '/%' AND relativePath NOT LIKE :currentNoteFolderRelativePath || '/%/%'"
                }
            }
            FolderDisplayMode.IncludeSubfolders -> {
                "WHERE relativePath LIKE :currentNoteFolderRelativePath || '%'"
            }
        }

        val sql = """
            WITH notes_with_filename AS (
                SELECT *, 
                       fullName(relativePath) AS fileName,
                       CASE WHEN content LIKE '%completed?: yes%' THEN 1 ELSE 0 END AS completed
                FROM Notes
                $whereClause
                ${if (tag != null) "AND content LIKE '%  - ' || :tag || '%'" else ""}
            )
            SELECT *,
                   CASE 
                       WHEN COUNT(*) OVER (PARTITION BY fileName) = 1 THEN 1
                       ELSE 0
                   END AS isUnique
            FROM notes_with_filename
            ORDER BY completed ASC, $sortColumn $order
        """.trimIndent()

        val args = if (tag != null) arrayOf(currentNoteFolderRelativePath, tag) else arrayOf(currentNoteFolderRelativePath)
        val query = SimpleSQLiteQuery(sql, args)
        return this.gridNotesRaw(query)
    }

    fun gridNotesWithQuery(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
        query: String,
        folderDisplayMode: FolderDisplayMode,
        tag: String? = null,
    ): PagingSource<Int, GridNote> {

        val (sortColumn, order) = when (sortOrder) {
            SortOrder.AZ -> "fileName" to "ASC"
            SortOrder.ZA -> "fileName" to "DESC"
            SortOrder.MostRecent -> "lastModifiedTimeMillis" to "DESC"
            SortOrder.Oldest -> "lastModifiedTimeMillis" to "ASC"
        }

        fun ftsEscape(query: String): String {

            // todo: change this when FTS5 is supported by room https://issuetracker.google.com/issues/146824830
            val specialChars: List<CharSequence> =
                listOf("\"", "*", "-", "(", ")", "<", ">", ":", "^", "~", "'", "AND", "OR", "NOT")

            if (specialChars.any { query.contains(it) }) {
                val escapedQuery = query.replace("\"", "\"\"")
                return "\"$escapedQuery\" * "
            } else {
                return "$query*"
            }
        }

        val whereClause = when (folderDisplayMode) {
            FolderDisplayMode.CurrentFolderOnly -> {
                if (currentNoteFolderRelativePath.isEmpty()) {
                    "Notes.relativePath NOT LIKE '%/%'"
                } else {
                    "Notes.relativePath LIKE :currentNoteFolderRelativePath || '/%' AND Notes.relativePath NOT LIKE :currentNoteFolderRelativePath || '/%/%'"
                }
            }
            FolderDisplayMode.IncludeSubfolders -> {
                "Notes.relativePath LIKE :currentNoteFolderRelativePath || '%'"
            }
        }

        val sql = """
            WITH notes_with_filename AS (
                SELECT Notes.*, 
                       rank(matchinfo(NotesFts, 'pcx')) AS score, 
                       fullName(Notes.relativePath) as fileName,
                       CASE WHEN Notes.content LIKE '%completed?: yes%' THEN 1 ELSE 0 END AS completed
                FROM Notes
                JOIN NotesFts ON NotesFts.rowid = Notes.rowid
                WHERE
                    $whereClause
                    AND
                    NotesFts MATCH :query
                    ${if (tag != null) "AND Notes.content LIKE '%  - ' || :tag || '%'" else ""}
            )
            SELECT *,
                   CASE 
                       WHEN COUNT(*) OVER (PARTITION BY fileName) = 1 THEN 1
                       ELSE 0
                   END AS isUnique
            FROM notes_with_filename
            ORDER BY completed ASC, score DESC, $sortColumn $order
        """.trimIndent()

        val args = if (tag != null) arrayOf(currentNoteFolderRelativePath, ftsEscape(query), tag) else arrayOf(currentNoteFolderRelativePath, ftsEscape(query))
        val query = SimpleSQLiteQuery(sql, args)


        return this.gridNotesRaw(query)
    }


    @RawQuery(observedEntities = [Note::class, NoteFolder::class])
    fun gridDrawerFoldersRaw(query: SupportSQLiteQuery): Flow<List<DrawerFolderModel>>

    fun drawerFolders(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
    ): Flow<List<DrawerFolderModel>> {

        val (sortColumn, order) = when (sortOrder) {
            SortOrder.AZ -> "folderName" to "ASC"
            SortOrder.ZA -> "folderName" to "DESC"
            SortOrder.MostRecent -> "MAX(n.lastModifiedTimeMillis)" to "DESC"
            SortOrder.Oldest -> "MAX(n.lastModifiedTimeMillis)" to "ASC"
        }

        val sql = """
            SELECT f.relativePath, f.id, COUNT(n.relativePath) as noteCount, fullName(f.relativePath) as folderName,
                   EXISTS(SELECT 1 FROM NoteFolders AS sub WHERE parentPath(sub.relativePath) = f.relativePath) as hasChildren
            FROM NoteFolders AS f
            LEFT JOIN Notes AS n ON n.relativePath LIKE f.relativePath || '%'
            WHERE parentPath(f.relativePath) = ?
            GROUP BY f.relativePath, f.id, folderName, hasChildren
            ORDER BY $sortColumn $order
        """.trimIndent()

        val query = SimpleSQLiteQuery(sql, arrayOf(currentNoteFolderRelativePath))
        return this.gridDrawerFoldersRaw(query)
    }

    @RawQuery(observedEntities = [Note::class, NoteFolder::class])
    fun noteFoldersRaw(query: SupportSQLiteQuery): Flow<List<NoteFolder>>

    // todo: use pages
    fun noteFolders(
        currentNoteFolderRelativePath: String,
    ): Flow<List<NoteFolder>> {

        val sql = """
            SELECT relativePath, id, fullName(relativePath) as folderName
            FROM NoteFolders
            WHERE parentPath(relativePath) = ?
            ORDER BY folderName ASC
        """.trimIndent()

        val query = SimpleSQLiteQuery(sql, arrayOf(currentNoteFolderRelativePath))
        return this.noteFoldersRaw(query)
    }

    data class Testing(
        val relativePath: String,
        val id: Int,
        val noteCount: Int,
    )

    @RawQuery
    fun debugQuery(query: SupportSQLiteQuery): List<Testing>

    fun testing() {

        val sql = """
            SELECT f.relativePath, f.id, COUNT(n.relativePath) as noteCount
            FROM NoteFolders AS f
            LEFT JOIN Notes AS n ON n.relativePath LIKE f.relativePath || '%'
            WHERE parentPath(f.relativePath) = ?
            GROUP BY f.relativePath
            ORDER BY MAX(n.lastModifiedTimeMillis) DESC
        """.trimIndent()

        val query = SimpleSQLiteQuery(sql, arrayOf(""))
        val results = this.debugQuery(query)

        Log.d("SQL_DEBUG", results.joinToString("\n"))
    }


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

object Rank : SQLiteDatabase.Function {
    override fun callback(
        args: SQLiteDatabase.Function.Args?,
        result: SQLiteDatabase.Function.Result?
    ) {
        if (args == null || result == null) return

        val blob = args.getBlob(0) ?: return

        val buffer = ByteBuffer.wrap(blob).order(ByteOrder.nativeOrder())

        val phraseCount = buffer.int
        val columnCount = buffer.int

        var score = 0.0

        for (phrase in 0 until phraseCount) {
            for (column in 0 until columnCount) {

                val hitsThisRow = buffer.int
                buffer.int
                buffer.int

                if (hitsThisRow != 0) {
                    // relativePath column
                    if (column == 0) {
                        result.set(2.0)
                        return
                    }
                    // content column
                    else {
                        score = 1.0
                    }
                }
            }
        }

        result.set(score)
    }

}

object ParentPath : SQLiteDatabase.Function {
    override fun callback(
        args: SQLiteDatabase.Function.Args?,
        result: SQLiteDatabase.Function.Result?
    ) {
        if (args == null || result == null) return

        val path = args.getString(0) ?: return

        if (path == "") return

        result.set(path.substringBeforeLast("/", missingDelimiterValue = ""))
    }
}

object FullName : SQLiteDatabase.Function {
    override fun callback(
        args: SQLiteDatabase.Function.Args?,
        result: SQLiteDatabase.Function.Result?
    ) {
        if (args == null || result == null) return

        val path = args.getString(0) ?: return

        result.set(path.substringAfterLast("/"))
    }
}