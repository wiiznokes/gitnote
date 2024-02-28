package com.example.gitnote.manager

import android.util.Log
import com.example.gitnote.MyApp
import com.example.gitnote.data.AppPreferences
import com.example.gitnote.data.platform.FileFs
import com.example.gitnote.data.platform.FolderFs
import com.example.gitnote.data.room.Note
import com.example.gitnote.data.room.NoteFolder
import com.example.gitnote.data.room.RepoDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.success


class StorageManager {


    companion object {
        private const val TAG = "StorageManager"
    }

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase

    private val uiHelper = MyApp.appModule.uiHelper

    private val dao = this.db.repoDatabaseDao

    private val gitManager: GitManager = MyApp.appModule.gitManager


    private val locker = Mutex()


    suspend fun updateDatabaseAndRepo(): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateDatabaseAndRepo")

        val creed = prefs.gitCreed()

        gitManager.pull(creed).onFailure {
            uiHelper.makeToast(it.message)
        }

        // todo: maybe async this call
        gitManager.commitAll(prefs.userName.get()).onFailure {
            uiHelper.makeToast(it.message)
        }

        // todo: maybe async this call
        gitManager.push(creed).onFailure {
            uiHelper.makeToast(it.message)
        }

        updateDatabaseWithoutLocker()

        return success(Unit)
    }

    /**
     * Update the database with the last files
     * available of the fs, and update with the
     * head commit of the repo.
     *
     * /!\ Warning there can be pending file added to the database
     * that are not committed to the repo.
     * The caller must ensure that all files has been committed
     * to keep the database in sync with the remote repo
     */
    private suspend fun updateDatabaseWithoutLocker(): Result<Unit> {

        val fsCommit = gitManager.lastCommit()
        val databaseCommit = prefs.databaseCommit.get()

        Log.d(TAG, "fsCommit: $fsCommit, databaseCommit: $databaseCommit")
        if (fsCommit == databaseCommit) {
            Log.d(TAG, "last commit is already loaded in data base")
            return success(Unit)
        }

        val repoPath = prefs.repoPath.get()
        Log.d(TAG, "repoPath = $repoPath")

        dao.clearAndInit(repoPath)
        prefs.databaseCommit.update(fsCommit)

        return success(Unit)
    }

    /**
     * See the documentation of [updateDatabaseWithoutLocker]
     */
    suspend fun updateDatabase(): Result<Unit> = locker.withLock {
        updateDatabaseWithoutLocker()
    }

    /**
     * Best effort
     */
    suspend fun updateNote(new: Note, previous: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateNote: previous = $previous")
        Log.d(TAG, "updateNote: new = $new")

        update {
            dao.removeNote(previous)
            dao.insertNote(new)

            val rootPath = prefs.repoPath.get()
            val previousFile = FileFs.fromPath(rootPath, previous.relativePath)

            previousFile.delete().onFailure {
                val message = "Can't delete previous note: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            val newFile = FileFs.fromPath(rootPath, new.relativePath)

            newFile.create().onFailure {
                val message = "Can't create new file: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            newFile.write(new.content).onFailure {
                val message = "Can't write content to new note: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }

    }

    /**
     * Best effort
     */
    suspend fun createNote(note: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNote: $note")

        update {
            dao.insertNote(note)

            val rootPath = prefs.repoPath.get()
            val file = FileFs.fromPath(rootPath, note.relativePath)

            file.create().onFailure {
                val message = "Can't create new file: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }
            file.write(note.content).onFailure {
                val message = "Can't write new file: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }
    }


    suspend fun deleteNote(note: Note): Result<Unit> = locker.withLock {

        Log.d(TAG, "deleteNote: $note")
        update {
            dao.removeNote(note)

            val rootPath = prefs.repoPath.get()
            val file = FileFs.fromPath(rootPath, note.relativePath)
            file.delete().onFailure {
                val message = "Can't delete file ${file.path}: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

        }
        return success(Unit)
    }

    suspend fun deleteNotes(noteRelativePaths: List<String>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNotes: ${noteRelativePaths.size}")

        update {

            // optimization because we only see the db state on screen
            noteRelativePaths.forEach { noteRelativePath ->
                val removedNoteCount = dao.removeNote(noteRelativePath)
                if (removedNoteCount != 1) {
                    Log.e(TAG, "removed note count was != 1 from the doa: $removedNoteCount")
                }
            }

            val rootPath = prefs.repoPath.get()
            noteRelativePaths.forEach { noteRelativePath ->

                Log.d(TAG, "deleting $noteRelativePath")
                val file = FileFs.fromPath(rootPath, noteRelativePath)

                file.delete().onFailure {
                    val message = "Can't delete file ${file.path}: ${it.message}"
                    Log.e(TAG, message)
                    uiHelper.makeToast(message)
                }
            }
        }

        return success(Unit)
    }

    suspend fun createNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNoteFolder: $noteFolder")

        update {
            dao.insertNoteFolder(noteFolder)

            val rootPath = prefs.repoPath.get()
            val folder = FolderFs.fromPath(rootPath, noteFolder.relativePath)

            folder.create().onFailure {
                val message = "Can't create new folder: ${it.message}"
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }
    }

    suspend fun closeRepo() = locker.withLock {
        gitManager.closeRepo()
        dao.clearDatabase()
        prefs.onCloseRepo()
    }


    private suspend fun <T> update(
        f: suspend () -> T
    ): T {
        gitManager.pull(prefs.gitCreed())
        gitManager.commitAll(prefs.userName.get())
        updateDatabaseWithoutLocker()

        val payload = f()

        gitManager.commitAll(prefs.userName.get())
        gitManager.push(prefs.gitCreed())
        prefs.databaseCommit.update(gitManager.lastCommit())
        return payload
    }
}