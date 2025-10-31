package io.github.wiiznokes.gitnote.manager

import android.util.Log
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "StorageManager"

sealed interface SyncState {

    data class Ok(val isConsumed: Boolean) : SyncState

    object Error : SyncState

    object Pull : SyncState

    object Push : SyncState

    fun isLoading(): Boolean {
        return this is Pull || this is Push
    }
}

sealed class Progress {
    data object Timestamps : Progress()

    data class GeneratingDatabase(val path: String) : Progress()
}

class StorageManager {


    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase

    private val uiHelper = MyApp.appModule.uiHelper

    private val dao = this.db.repoDatabaseDao

    private val gitManager: GitManager = MyApp.appModule.gitManager

    private val locker = Mutex()

    private val _syncState: MutableStateFlow<SyncState> = MutableStateFlow(SyncState.Ok(true))
    val syncState: StateFlow<SyncState> = _syncState


    suspend fun updateDatabaseAndRepo(): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateDatabaseAndRepo")

        val cred = prefs.cred()
        val remoteUrl = prefs.remoteUrl.get()



        gitManager.commitAll(
            prefs.usernameOrDefault(),
            "commit from gitnote to update the repo of the app"
        ).onFailure {
            uiHelper.makeToast(it.message)
        }

        if (remoteUrl.isNotEmpty()) {
            _syncState.emit(SyncState.Pull)
            gitManager.pull(cred).onFailure {
                uiHelper.makeToast(it.message)
            }
        }

        if (remoteUrl.isNotEmpty()) {
            _syncState.emit(SyncState.Push)
            // todo: maybe async this call
            gitManager.push(cred).onFailure {
                uiHelper.makeToast(it.message)
            }
        }
        _syncState.emit(SyncState.Ok(false))

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
    private suspend fun updateDatabaseWithoutLocker(
        force: Boolean = false,
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> {

        val fsCommit = gitManager.lastCommit()
        val databaseCommit = prefs.databaseCommit.get()

        Log.d(TAG, "fsCommit: $fsCommit, databaseCommit: $databaseCommit")
        if (!force && fsCommit == databaseCommit) {
            Log.d(TAG, "last commit is already loaded in data base")
            return success(Unit)
        }

        val repoPath = prefs.repoPath()
        Log.d(TAG, "repoPath = $repoPath")

        progressCb?.invoke(Progress.Timestamps)
        val timestamps = gitManager.getTimestamps().getOrThrow()

        dao.clearAndInit(repoPath, timestamps, progressCb)
        prefs.databaseCommit.update(fsCommit)

        return success(Unit)
    }

    /**
     * See the documentation of [updateDatabaseWithoutLocker]
     */
    suspend fun updateDatabase(
        force: Boolean = false,
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> = locker.withLock {
        updateDatabaseWithoutLocker(force, progressCb)
    }

    /**
     * Best effort
     */
    suspend fun updateNote(new: Note, previous: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateNote: previous = $previous")
        Log.d(TAG, "updateNote: new = $new")

        update(
            commitMessage = "gitnote changed ${previous.relativePath}"
        ) {
            dao.removeNote(previous)
            dao.insertNote(new)

            val rootPath = prefs.repoPath()
            val previousFile = previous.toFileFs(rootPath)

            previousFile.delete().onFailure {
                val message =
                    uiHelper.getString(R.string.error_delete_file, previousFile.path, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            val newFile = new.toFileFs(rootPath)
            newFile.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            newFile.write(new.content).onFailure {
                val message = uiHelper.getString(R.string.error_write_file, it.message)
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

        update(
            commitMessage = "gitnote created ${note.relativePath}"
        ) {
            dao.insertNote(note)

            val file = note.toFileFs(prefs.repoPath())

            file.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }
            file.write(note.content).onFailure {
                val message = uiHelper.getString(R.string.error_write_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }
    }


    suspend fun deleteNote(note: Note): Result<Unit> = locker.withLock {

        Log.d(TAG, "deleteNote: $note")
        update(
            commitMessage = "gitnote deleted ${note.relativePath}"
        ) {
            dao.removeNote(note)

            val file = note.toFileFs(prefs.repoPath())
            file.delete().onFailure {
                val message = uiHelper.getString(R.string.error_delete_file, file.path, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }
            success(Unit)
        }
    }

    suspend fun deleteNotes(notes: List<Note>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNotes: ${notes.size}")

        update(
            commitMessage = "gitnote deleted ${notes.size} notes"
        ) {
            // optimization because we only see the db state on screen
            notes.forEach { note ->
                dao.removeNote(note)
            }

            val repoPath = prefs.repoPath()
            notes.forEach { note ->

                Log.d(TAG, "deleting $note")
                val file = note.toFileFs(repoPath)

                file.delete().onFailure {
                    val message =
                        uiHelper.getString(R.string.error_delete_file, file.path, it.message)
                    Log.e(TAG, message)
                    uiHelper.makeToast(message)
                }
            }
            success(Unit)
        }
    }

    suspend fun createNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNoteFolder: $noteFolder")

        update(
            commitMessage = "gitnote created folder ${noteFolder.relativePath}"
        ) {
            dao.insertNoteFolder(noteFolder)

            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_folder, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }
    }

    suspend fun deleteNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNoteFolder: $noteFolder")

        update(
            commitMessage = "gitnote deleted folder ${noteFolder.relativePath}"
        ) {
            dao.deleteNoteFolder(noteFolder)

            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.delete().onFailure {
                val msg = uiHelper.getString(R.string.error_delete_folder, it.message)
                Log.e(TAG, msg)
                uiHelper.makeToast(msg)
            }

            success(Unit)
        }
    }

    suspend fun closeRepo() = locker.withLock {
        prefs.closeRepo()
        gitManager.closeRepo()
        dao.clearDatabase()
    }


    private suspend fun <T> update(
        commitMessage: String,
        f: suspend () -> Result<T>
    ): Result<T> {

        val cred = prefs.cred()
        val remoteUrl = prefs.remoteUrl.get()

        gitManager.commitAll(
            prefs.usernameOrDefault(),
            "commit from gitnote, before doing a change"
        ).onFailure {
            return failure(it)
        }

        if (remoteUrl.isNotEmpty()) {
            _syncState.emit(SyncState.Pull)
            gitManager.pull(cred).onFailure {
                it.printStackTrace()
            }
        }

        updateDatabaseWithoutLocker().onFailure {
            return failure(it)
        }

        val payload = f().fold(
            onFailure = {
                return failure(it)
            },
            onSuccess = {
                it
            }
        )

        gitManager.commitAll(prefs.usernameOrDefault(), commitMessage).onFailure {
            return failure(it)
        }

        if (remoteUrl.isNotEmpty()) {
            _syncState.emit(SyncState.Push)
            gitManager.push(cred).onFailure {
                it.printStackTrace()
            }
        }

        prefs.databaseCommit.update(gitManager.lastCommit())

        _syncState.emit(SyncState.Ok(false))
        return success(payload)
    }

    suspend fun consumeOkSyncState() {
        _syncState.emit(SyncState.Ok(true))
    }
}