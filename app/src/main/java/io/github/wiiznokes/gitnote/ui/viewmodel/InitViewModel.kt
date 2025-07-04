package io.github.wiiznokes.gitnote.ui.viewmodel


import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.provider.GithubProvider
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.provider.RepoInfo
import io.github.wiiznokes.gitnote.provider.UserInfo
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState.AuthState
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState.AuthStep2
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState.CloneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class InitViewModelFactory(private val flow: SharedFlow<String>) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InitViewModel::class.java)) {
            return InitViewModel(flow) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class InitViewModel(val authFlow: SharedFlow<String>) : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val gitManager = MyApp.appModule.gitManager
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    companion object {
        private const val TAG = "InitViewModel"
    }

    private val _initState: MutableStateFlow<InitState> = MutableStateFlow(InitState.Idle)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    var provider: Provider? = null
        private set

    var repos = listOf<RepoInfo>()
        private set

    private var token = String()

    lateinit var userInfo: UserInfo
        private set

    init {

        CoroutineScope(Dispatchers.Default).launch {
            authFlow.collect {
                Log.d(TAG, "received $it")
                onReceiveCode(it)
            }
        }
    }

    private fun prepareLocalStorageRepoPath() {
        val folder = NodeFs.Folder.fromPath(AppPreferences.appStorageRepoPath)
        folder.delete()
        folder.create()
    }

    fun createLocalRepo(storageConfig: StorageConfiguration, onSuccess: () -> Unit) {

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



    fun checkPathForClone(repoPath: String): Result<Unit> {
        val result = NodeFs.Folder.fromPath(repoPath).isEmptyDirectory()
        result.onFailure {
            uiHelper.makeToast(it.message)
        }
        return result
    }


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

            _initState.emit(CloneState.Cloning(0))

            gitManager.cloneRepo(
                repoPath = storageConfig.repoPath(),
                repoUrl = repoUrl,
                cred = cred,
                progressCallback = {
                    _initState.tryEmit(CloneState.Cloning(it))
                }
            ).onFailure {
                uiHelper.makeToast(it.message)
                _initState.emit(CloneState.Error)
                return@launch
            }

            prefs.initRepo(storageConfig)
            prefs.remoteUrl.update(repoUrl)

            prefs.updateCred(cred)

            CoroutineScope(Dispatchers.IO).launch {
                storageManager.updateDatabase()
            }

            onSuccess()
        }
    }


    fun setProvider(provider: ProviderType?) {
        this.provider = when (provider) {
            ProviderType.GitHub -> GithubProvider()
            null -> null
        }
    }

    fun getLaunchOAuthScreenIntent(): Intent {
        val authUrl = provider!!.getLaunchOAuthScreenUrl()
        return Intent(Intent.ACTION_VIEW, authUrl.toUri())
    }


    fun onReceiveCode(code: String) {

        CoroutineScope(Dispatchers.IO).launch {

            _initState.emit(AuthState.GetAccessToken)
            token = try {
                provider!!.exchangeCodeForAccessToken(code)
            } catch (e: Exception) {
                Log.e(TAG, "exchangeCodeForAccessToken: ${e.message}, $e")
                _initState.emit(AuthState.Error)
                return@launch
            }

            _initState.emit(AuthState.FetchRepos)

            repos = try {
                provider!!.fetchUserRepos(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "fetchUserRepos: ${e.message}, $e")
                _initState.emit(AuthState.Error)
                return@launch
            }
            _initState.emit(AuthState.GetUserInfo)

            userInfo = try {
                provider!!.getUserInfo(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "getUserInfo: ${e.message}, $e")
                _initState.emit(AuthState.Error)
                return@launch
            }

            Log.d(TAG, "emit: Success")
            _initState.emit(AuthState.Success)
        }
    }

    fun cloneRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            val (publicKey, privateKey) = generateSshKeysLib()

            _initState.emit(AuthStep2.AddDeployKey)
            try {
                provider!!.addDeployKeyToRepo(
                    token = token,
                    publicKey = publicKey,
                    fullRepoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "addDeployKey: ${e.message}, $e")
                _initState.emit(AuthStep2.Error)
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
                    onSuccess()
                }
            )
        }
    }

    fun createRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            _initState.emit(AuthStep2.CreateRepo)
            try {
                provider!!.createNewRepo(
                    token = token,
                    repoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "createNewRepoOnRemoteGithub: ${e.message}, $e")
                _initState.emit(AuthStep2.Error)
                return@launch
            }

            val (publicKey, privateKey) = generateSshKeysLib()

            _initState.emit(AuthStep2.AddDeployKey)
            try {
                provider!!.addDeployKeyToRepo(
                    token = token,
                    publicKey = publicKey,
                    fullRepoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "addDeployKey: ${e.message}, $e")
                _initState.emit(AuthStep2.Error)
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
                    onSuccess()
                }
            )
        }
    }

}


sealed class InitState {
    data object Idle: InitState()

    open fun isClickable() : Boolean = true
    open fun isLoading(): Boolean = false
    open fun message(): String = ""

    sealed class AuthState: InitState() {
        data object Idle : AuthState()
        data object GetAccessToken : AuthState()
        data object FetchRepos : AuthState()
        data object GetUserInfo : AuthState()
        data object Success : AuthState()
        data object Error : AuthState()

        override fun isClickable(): Boolean = this is Idle || this is Error || this is Success
        override fun isLoading(): Boolean = this is GetAccessToken || this is FetchRepos || this is GetUserInfo

        override fun message(): String {
            return when (this) {
                Error -> "Error"
                FetchRepos -> "Fetching repositories"
                GetAccessToken -> "Getting the access token"
                GetUserInfo -> "Getting user information"
                Idle -> ""
                Success -> "Success"
            }
        }
    }

    sealed class AuthStep2: InitState() {
        data object Idle : AuthStep2()
        data object CreateRepo : AuthStep2()
        data object AddDeployKey : AuthStep2()
        data object Error : AuthStep2()

        override fun isClickable(): Boolean = this is Idle || this is Error
        override fun isLoading(): Boolean = this is CreateRepo || this is AddDeployKey

        override fun message(): String {
            return when (this) {
                AddDeployKey -> "Adding deploy key to the repository"
                CreateRepo -> "Creating the repository"
                Error -> "Error"
                Idle -> ""
            }
        }
    }


    sealed class CloneState: InitState() {
        data object Idle : CloneState()
        data class Cloning(val percent: Int) : CloneState()
        data object Error : CloneState()


        override fun isClickable(): Boolean = this is Idle || this is Error
        override fun isLoading(): Boolean = this is Cloning

        override fun message(): String {
            return when (this) {
                is Cloning -> "$percent %"
                Error -> "Error"
                Idle -> ""
            }
        }

    }
}
