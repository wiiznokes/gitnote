package com.example.gitnote.manager

import android.util.Log
import com.example.gitnote.ui.model.GitCreed
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
            System.loadLibrary("gitnote")
        }
    }

    private val locker = Mutex()
    private var isRepoInitialized = false
    private var isLibInitialized = false
    private var job: Deferred<Unit>? = null

    private suspend fun <T> safelyAccessLibGit2(f: suspend () -> T): Result<T> = locker.withLock {

        try {
            if (!isLibInitialized) {
                if (initLib() < 0) {
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
            throw GitException("Can't create repo: $res")
        }
        isRepoInitialized = true
    }


    suspend fun openRepo(repoPath: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "open repo: $repoPath")
        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)

        val res = openRepoLib(repoPath)
        if (res < 0) {
            throw GitException("can't open repo: $res")
        }
        isRepoInitialized = true
    }

    suspend fun cloneRepo(
        repoPath: String,
        repoUrl: String,
        creed: GitCreed?,
        progressCallback: (Int) -> Unit
    ): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "clone repo: $repoPath, $repoUrl, $creed")
        if (isRepoInitialized) throw GitException(GitExceptionType.RepoAlreadyInit)


        val res = cloneRepoLib(
            repoPath = repoPath,
            remoteUrl = repoUrl,
            username = creed?.userName,
            password = creed?.password,
            // cause error because of proguard
            // https://stackoverflow.com/questions/77936218/how-to-tell-proguard-to-not-remove-a-lamda-function-parameter
            //progressCallback = progressCallback
        )

        if (res < 0) {
            throw GitException("can't clone repo: $res")
        }

        isRepoInitialized = true

    }

    // todo: update this shit
    suspend fun lastCommit(): String = safelyAccessLibGit2 {
        Log.d(TAG, "last commit")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)
        lastCommitLib()
    }.getOrDefault("") ?: ""

    suspend fun commitAll(username: String): Result<Unit> = safelyAccessLibGit2 {
        Log.d(TAG, "commit all: $username")
        if (!isRepoInitialized) throw GitException(GitExceptionType.RepoNotInit)

        var res = isChangeLib()

        if (res < 0) {
            throw GitException("can't know if there is files change: $res")
        }

        if (res == 0) {
            // nothing to commit
            Log.d(TAG, "nothing to commit")
            return@safelyAccessLibGit2
        }

        res = commitAllLib(username)
        if (res < 0) {
            throw GitException("can't commit: $res")
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
            throw Exception("Can't push: $res")
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
            throw Exception("Can't pull: $res")
        }
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
    progressCallback: ((Int) -> Unit)? = null
): Int


private external fun lastCommitLib(): String?

private external fun commitAllLib(username: String): Int
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


