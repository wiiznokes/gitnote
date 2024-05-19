package io.github.wiiznokes.gitnote.ui.viewmodel


import androidx.lifecycle.ViewModel
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.NewRepoState
import io.github.wiiznokes.gitnote.data.RepoState
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.GitException
import io.github.wiiznokes.gitnote.manager.GitExceptionType
import io.github.wiiznokes.gitnote.ui.model.GitCreed
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
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    companion object {
        private const val TAG = "InitViewModel"
    }

    private fun prepareLocalStorageRepoPath() {
        val folder = NodeFs.Folder.fromPath(AppPreferences.appStorageRepoPath)
        folder.delete()
        folder.create()
    }

    fun createRepo(repoState: NewRepoState, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {

            if (repoState is NewRepoState.AppStorage) {
                prepareLocalStorageRepoPath()
            }

            NodeFs.Folder.fromPath(repoState.repoPath()).isEmptyDirectory().onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }


            gitManager.createRepo(repoState.repoPath()).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            prefs.initRepo(repoState)

            CoroutineScope(Dispatchers.IO).launch {
                storageManager.updateDatabase()
            }

            onSuccess()
        }

    }

    private suspend fun openRepoSuspend(repoState: NewRepoState): Result<Unit> {

        if (!NodeFs.Folder.fromPath(repoState.repoPath()).exist()) {
            val msg = uiHelper.getString(R.string.error_path_not_directory)
            uiHelper.makeToast(msg)
            return failure(Exception(msg))
        }

        gitManager.openRepo(repoState.repoPath()).onFailure {
            uiHelper.makeToast(it.message)
            return failure(it)
        }

        prefs.initRepo(repoState)

        // yes, there can be pending file not committed
        // but they will be committed in the updateDatabaseAndRepo function
        // anyway
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.updateDatabase()
        }

        return success(Unit)
    }

    fun openRepo(repoState: NewRepoState, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            openRepoSuspend(repoState).onSuccess {
                onSuccess()
            }
        }

    }


    fun checkPathForClone(repoPath: String): Result<Unit> {
        val result = NodeFs.Folder.fromPath(repoPath).isEmptyDirectory()
        result.onFailure {
            uiHelper.makeToast(it.message)
        }
        return result
    }

    private val _cloneState: MutableStateFlow<CloneState> = MutableStateFlow(CloneState.Idle)
    val cloneState: StateFlow<CloneState>
        get() = _cloneState.asStateFlow()


    fun cloneRepo(
        repoState: NewRepoState,
        repoUrl: String,
        gitCreed: GitCreed? = null,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            if (repoState is NewRepoState.AppStorage) {
                prepareLocalStorageRepoPath()
            }

            _cloneState.emit(CloneState.Cloning(0))

            gitManager.cloneRepo(
                repoPath = repoState.repoPath(),
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

            prefs.initRepo(repoState)
            prefs.remoteUrl.update(repoUrl)

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

        val repoState = when (prefs.repoState.get()) {
            RepoState.NoRepo -> return false
            RepoState.AppStorage -> {
                NewRepoState.AppStorage
            }

            RepoState.DeviceStorage -> {
                if (!StoragePermissionHelper.isPermissionGranted()) {
                    return false
                }
                val repoPath = try {
                    prefs.repoPath()
                } catch (e: Exception) {
                    return false
                }
                NewRepoState.DeviceStorage(repoPath)
            }
        }


        openRepoSuspend(repoState).onFailure {
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