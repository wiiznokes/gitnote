package io.github.wiiznokes.gitnote.ui.viewmodel


import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.StorageConfig
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.provider.RepoInfo
import io.github.wiiznokes.gitnote.provider.UserInfo
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
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

    fun createRepo(storageConfig: StorageConfiguration, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {

            if (storageConfig is StorageConfiguration.App) {
                prepareLocalStorageRepoPath()
            }

            NodeFs.Folder.fromPath(storageConfig.repoPath()).isEmptyDirectory().onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }


            gitManager.createRepo(storageConfig.repoPath()).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            prefs.initRepo(storageConfig)

            CoroutineScope(Dispatchers.IO).launch {
                storageManager.updateDatabase()
            }

            onSuccess()
        }

    }

    private suspend fun openRepoSuspend(storageConfig: StorageConfiguration): Result<Unit> {

        if (!NodeFs.Folder.fromPath(storageConfig.repoPath()).exist()) {
            val msg = uiHelper.getString(R.string.error_path_not_directory)
            uiHelper.makeToast(msg)
            return failure(Exception(msg))
        }

        gitManager.openRepo(storageConfig.repoPath()).onFailure {
            uiHelper.makeToast(it.message)
            return failure(it)
        }

        prefs.initRepo(storageConfig)

        // yes, there can be pending file not committed
        // but they will be committed in the updateDatabaseAndRepo function
        // anyway
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.updateDatabase()
        }

        return success(Unit)
    }

    fun openRepo(repoState: StorageConfiguration, onSuccess: () -> Unit) {

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
        storageConfig: StorageConfiguration,
        repoUrl: String,
        cred: Cred? = null,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            if (storageConfig is StorageConfiguration.App) {
                prepareLocalStorageRepoPath()
            }

            _cloneState.emit(CloneState.Cloning(0))

            gitManager.cloneRepo(
                repoPath = storageConfig.repoPath(),
                repoUrl = repoUrl,
                cred = cred,
                progressCallback = {
                    _cloneState.tryEmit(CloneState.Cloning(it))
                }
            ).onFailure {
                uiHelper.makeToast(it.message)
                _cloneState.emit(CloneState.Error)
                return@launch
            }


            _cloneState.emit(CloneState.Cloned)

            prefs.initRepo(storageConfig)
            prefs.remoteUrl.update(repoUrl)

            prefs.updateCred(cred)

            CoroutineScope(Dispatchers.IO).launch {
                storageManager.updateDatabase()
            }

            onSuccess()
        }
    }

    suspend fun tryInit(): Boolean {

        if (!prefs.isInit.get()) {
            return false
        }

        val storageConfig = when (prefs.storageConfig.get()) {
            StorageConfig.App -> {
                StorageConfiguration.App
            }

            StorageConfig.Device -> {
                if (!StoragePermissionHelper.isPermissionGranted()) {
                    return false
                }
                val repoPath = try {
                    prefs.repoPath()
                } catch (e: Exception) {
                    return false
                }
                StorageConfiguration.Device(repoPath)
            }
        }

        openRepoSuspend(storageConfig).onFailure {
            CoroutineScope(Dispatchers.IO).launch {
                storageManager.closeRepo()
            }
            return false
        }
        return true
    }

    var provider: Provider? = null

    fun getLaunchOAuthScreenIntent(): Intent {
        val authUrl = provider!!.getLaunchOAuthScreenUrl()
        return Intent(Intent.ACTION_VIEW, authUrl.toUri())
    }

    var repos = listOf<RepoInfo>()
        private set

    private var token = String()

    lateinit var userInfo: UserInfo
        private set


    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun onReceiveCode(code: String) {

        CoroutineScope(Dispatchers.IO).launch {

            _authState.emit(AuthState.GetAccessToken)
            token = try {
                provider!!.exchangeCodeForAccessToken(code)
            } catch (e: Exception) {
                Log.e(TAG, "exchangeCodeForAccessToken: ${e.message}, $e")
                _authState.emit(AuthState.Error)
                return@launch
            }

            _authState.emit(AuthState.FetchRepos)

            repos = try {
                provider!!.fetchUserRepos(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "fetchUserRepos: ${e.message}, $e")
                _authState.emit(AuthState.Error)
                return@launch
            }
            _authState.emit(AuthState.GetUserInfo)

            userInfo = try {
                provider!!.getUserInfo(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "getUserInfo: ${e.message}, $e")
                _authState.emit(AuthState.Error)
                return@launch
            }

            Log.d(TAG, "emit: Success")
            _authState.emit(AuthState.Success)
        }
    }


    private val _authState2: MutableStateFlow<AuthState2> = MutableStateFlow(AuthState2.Idle)
    val authState2: StateFlow<AuthState2> = _authState2.asStateFlow()


    fun cloneRepoFromAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            val (publicKey, privateKey) = generateSshKeysLib()

            _authState2.emit(AuthState2.AddDeployKey)
            try {
                provider!!.addDeployKeyToRepo(
                    token = token,
                    publicKey = publicKey,
                    fullRepoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "addDeployKey: ${e.message}, $e")
                _authState2.emit(AuthState2.Error)
                return@launch
            }

            cloneRepo(
                storageConfig = storageConfig,
                repoUrl = provider!!.sshCloneUrlFromRepoName(repoName),
                cred = Cred.Ssh(
                    publicKey = publicKey,
                    privateKey = privateKey
                ),
                onSuccess = {
                    viewModelScope.launch {
                        _authState2.emit(AuthState2.Success)
                    }
                    onSuccess()
                }
            )
        }
    }

    fun createNewRepoOnRemote(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            _authState2.emit(AuthState2.CreateRepo)
            try {
                provider!!.createNewRepo(
                    token = token,
                    repoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "createNewRepoOnRemoteGithub: ${e.message}, $e")
                _authState2.emit(AuthState2.Error)
                return@launch
            }

            val (publicKey, privateKey) = generateSshKeysLib()

            _authState2.emit(AuthState2.AddDeployKey)
            try {
                provider!!.addDeployKeyToRepo(
                    token = token,
                    publicKey = publicKey,
                    fullRepoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "addDeployKey: ${e.message}, $e")
                _authState2.emit(AuthState2.Error)
                return@launch
            }

            cloneRepo(
                storageConfig = storageConfig,
                repoUrl = provider!!.sshCloneUrlFromRepoName(repoName),
                cred = Cred.Ssh(
                    publicKey = publicKey,
                    privateKey = privateKey
                ),
                onSuccess = {
                    viewModelScope.launch {
                        _authState2.emit(AuthState2.Success)
                    }
                    onSuccess()
                }
            )
        }
    }

}


sealed class AuthState2 {
    data object Idle : AuthState2()
    data object CreateRepo : AuthState2()
    data object AddDeployKey : AuthState2()
    data object Success : AuthState2()
    data object Error : AuthState2()

    fun isClickable(): Boolean = this is Idle || this is Error
}


sealed class AuthState {
    data object Idle : AuthState()
    data object GetAccessToken : AuthState()
    data object FetchRepos : AuthState()
    data object GetUserInfo : AuthState()
    data object Success : AuthState()
    data object Error : AuthState()
}

sealed class CloneState {
    data object Idle : CloneState()
    data class Cloning(val percent: Int) : CloneState()
    data object Cloned : CloneState()
    data object Error : CloneState()


    fun isClickable(): Boolean = this is Idle || this is Error

}