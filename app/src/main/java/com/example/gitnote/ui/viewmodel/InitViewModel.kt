package com.example.gitnote.ui.viewmodel


import androidx.lifecycle.ViewModel
import com.example.gitnote.MyApp
import com.example.gitnote.data.AppPreferences
import com.example.gitnote.data.platform.FolderFs
import com.example.gitnote.helper.UiHelper
import com.example.gitnote.manager.GitException
import com.example.gitnote.manager.GitExceptionType
import com.example.gitnote.ui.model.GitCreed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class InitViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val gitManager = MyApp.appModule.gitManager
    private val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    companion object {
        private const val TAG = "InitViewModel"
    }


    fun createRepo(repoPath: String, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            FolderFs.fromPath(repoPath).isEmptyDirectory().onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }


            gitManager.createRepo(repoPath).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }


            prefs.repoPath.update(repoPath)
            prefs.isRepoInitialize.update(true)

            CoroutineScope(Dispatchers.IO).launch {
                storageManager.updateDatabase()
            }

            onSuccess()
        }

    }

    fun checkPathForClone(repoPath: String): Result<Unit> {
        val result = FolderFs.fromPath(repoPath).isEmptyDirectory()
        result.onFailure {
            uiHelper.makeToast(it.message)
        }
        return result
    }

    private suspend fun openRepoSuspend(repoPath: String): Result<Unit> {

        if (!FolderFs.fromPath(repoPath).exist()) {
            uiHelper.makeToast("Path is not a directory")
            return failure(GitException(GitExceptionType.WrongPath))
        }

        gitManager.openRepo(repoPath).onFailure {
            uiHelper.makeToast(it.message)
            return failure(it)
        }

        prefs.repoPath.update(repoPath)
        prefs.isRepoInitialize.update(true)

        // yes, there can be pending file not committed
        // but they will be committed in the updateDatabaseAndRepo function
        // anyway
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.updateDatabase()
        }

        return success(Unit)
    }

    fun openRepo(repoPath: String, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            openRepoSuspend(repoPath).onSuccess {
                onSuccess()
            }
        }

    }


    private val _cloneState: MutableStateFlow<CloneState> = MutableStateFlow(CloneState.Idle)
    val cloneState: StateFlow<CloneState>
        get() = _cloneState.asStateFlow()


    fun cloneRepo(
        repoPath: String,
        repoUrl: String,
        gitCreed: GitCreed? = null,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            _cloneState.emit(CloneState.Cloning(0))

            gitManager.cloneRepo(
                repoPath = repoPath,
                repoUrl = repoUrl,
                creed = gitCreed,
                progressCallback = {
                    _cloneState.tryEmit(CloneState.Cloning(it))
                }
            ).onFailure {
                uiHelper.makeToast(it.message)
                _cloneState.emit(CloneState.Error)
                return@launch
            }


            _cloneState.emit(CloneState.Cloned)

            prefs.repoPath.update(repoPath)
            prefs.remoteUrl.update(repoUrl)
            prefs.isRepoInitialize.update(true)
            gitCreed?.let {
                prefs.userName.update(it.userName)
                prefs.password.update(it.password)
            }

            CoroutineScope(Dispatchers.IO).launch {
                storageManager.updateDatabase()
            }

            onSuccess()
        }
    }

    suspend fun tryInit(): Boolean {

        if (!prefs.isRepoInitialize.get()) return false
        val repoPath = prefs.repoPath.get()

        openRepoSuspend(repoPath).onFailure {
            CoroutineScope(Dispatchers.IO).launch {
                storageManager.closeRepo()
            }
            return false
        }

        return true
    }

}

sealed class CloneState {
    data object Idle : CloneState()
    data class Cloning(val percent: Int) : CloneState()
    data object Cloned : CloneState()
    data object Error : CloneState()


    fun isClickable(): Boolean = this is Idle || this is Error

}