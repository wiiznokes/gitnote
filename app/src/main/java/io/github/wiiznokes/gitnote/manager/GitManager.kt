package io.github.wiiznokes.gitnote.manager

import android.util.Log
import androidx.annotation.Keep
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.model.GitCreed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


enum class GitExceptionType {
    InitLib,
    RepoAlreadyInit,
    RepoNotInit,
    WrongPath,
    Other
}

class GitException(
    val type: GitExceptionType,
    message: String?
) : Exception(getMessage(type, message)) {

    companion object {
        private fun getMessage(
            type: GitExceptionType,
            message: String?
        ): String? =
            message ?: if (type != GitExceptionType.Other) type.name else null

    }

    constructor(message: String) : this(GitExceptionType.Other, message)
    constructor(type: GitExceptionType) : this(type, null)
}

class GitManager {

    companion object {
        private const val TAG = "GitManager"

        init {
            Log.d(TAG, "init")
            System.loadLibrary("git_wrapper")
        }
    }

    private val uiHelper = MyApp.appModule.uiHelper

    private val locker = Mutex()
    private var isRepoInitialized = false
    private var isLibInitialized = false
    private var job: Deferred<Unit>? = null

    private suspend fun <T> safelyAccessLibGit2(f: suspend () -> T): Result<T> = locker.withLock {

        try {
            if (!isLibInitialized) {
                val res = initLib()
                Log.d(TAG, "res on init = $res")
                if (res < 0) {
                    throw GitException(GitExceptionType.InitLib)
                }
                isLibInitialized = true
            }

            val deferredValue = CompletableDeferred<T>()
            coroutineScope {
                job = async { deferredValue.complete(f()) }
                job?.await()
                success(deferredValue.await())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            failure(e)
        } finally {
            job = null
        }
    }


    suspend fun createRepo(repoPath: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "create repo: $repoPath")

        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)

        val res = createRepoLib(repoPath)
        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_create_repo, res.toString()))
        }
        isRepoInitialized = true
    }


    suspend fun openRepo(repoPath: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "open repo: $repoPath")
        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)

        val res = openRepoLib(repoPath)
        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_open_repo, res))
        }
        isRepoInitialized = true
    }

    private var actualCb: ((Int) -> Unit)? = null

    /**
     * This function is called from native code
     */
    @Keep
    fun progressCb(progress: Int) {
        if (actualCb!= null) {
            actualCb?.invoke(progress)
        }
    }

    suspend fun cloneRepo(
        repoPath: String,
        repoUrl: String,
        creed: GitCreed?,
        progressCallback: (Int) -> Unit
    ): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "clone repo: $repoPath, $repoUrl, $creed")
        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)

        actualCb = progressCallback

        val res = cloneRepoLib(
            repoPath = repoPath,
            remoteUrl = repoUrl,
            username = creed?.userName,
            password = creed?.password,
            progressCallback = this
        )

        actualCb = null

        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_clone_repo, res))
        }

        isRepoInitialized = true

    }

    // todo: update this shit
    suspend fun lastCommit(): String = safelyAccessLibGit2 {
        Log.d(TAG, "last commit")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)
        lastCommitLib()
    }.getOrDefault("") ?: ""

    suspend fun commitAll(username: String, message: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "commit all: $username")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        var res = isChangeLib()

        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_commit_file_change, res))
        }

        if (res == 0) {
            // nothing to commit
            Log.d(TAG, "nothing to commit")
            return@safelyAccessLibGit2
        }

        res = commitAllLib(username, message)
        if (res < 0) {
            throw GitException(uiHelper.getString(R.string.error_commit_repo, res.toString()))
        }

    }

    suspend fun push(creed: GitCreed?): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "push: $creed")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)
        val res = pushLib(
            username = creed?.userName,
            password = creed?.password,
        )

        if (res < 0) {
            Log.d(TAG, "push: $res")
            val msg = uiHelper.getString(R.string.error_push_repo, res.toString())
            Log.d(TAG, "push: $msg")

            throw Exception(uiHelper.getString(R.string.error_push_repo, res.toString()))
        }

    }

    suspend fun pull(creed: GitCreed?): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "pull: $creed")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        val res = pullLib(
            username = creed?.userName,
            password = creed?.password,
        )

        if (res < 0) {
            throw Exception(uiHelper.getString(R.string.error_pull_repo, res.toString()))
        }
    }

    suspend fun getTimestamps(): Result<HashMap<String, Long>> = safelyAccessLibGit2 {
        Log.d(TAG, "getTimestamps")

        val h: HashMap<String, Long> = HashMap()

        val res = getTimestampsLib(h)

        if (res < 0) {
            throw Exception("getTimestampsLib error $res")
        }
        h
    }


    fun closeRepo() {
        job?.cancel()
        if (isRepoInitialized) closeRepoLib()
        isRepoInitialized = false
    }

    fun shutdown() {
        job?.cancel()
        closeRepo()
        if (isLibInitialized) freeLib()
        isLibInitialized = false
    }

}

private external fun initLib(): Int

private external fun createRepoLib(repoPath: String): Int

private external fun openRepoLib(repoPath: String): Int

private external fun cloneRepoLib(
    repoPath: String,
    remoteUrl: String,
    username: String?,
    password: String?,
    progressCallback: GitManager
): Int


private external fun lastCommitLib(): String?

private external fun commitAllLib(username: String, message: String): Int
private external fun pushLib(
    username: String?, password: String?,
    progressCallback: ((Int) -> Unit)? = null
): Int


private external fun pullLib(
    username: String?, password: String?,
    progressCallback: ((Int) -> Unit)? = null
): Int

private external fun freeLib()


private external fun closeRepoLib()

private external fun isChangeLib(): Int

private external fun getTimestampsLib(timestamps: HashMap<String, Long>): Int
