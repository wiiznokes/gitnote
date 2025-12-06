package io.github.wiiznokes.gitnote.ui.viewmodel


import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.Progress
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.provider.GithubProvider
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.provider.RepoInfo
import io.github.wiiznokes.gitnote.provider.UserInfo
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SetupViewModel"

interface SetupViewModelI {

    fun launch(f: suspend () -> Unit) {}

    fun cloneRepo(
        storageConfig: StorageConfiguration,
        remoteUrl: String,
        cred: Cred? = null,
        onSuccess: () -> Unit
    ) {
    }

    fun createRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {
    }

    fun cloneRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {
    }
}

class SetupViewModelMock : SetupViewModelI

class SetupViewModel(val authFlow: SharedFlow<String>) : ViewModel(), SetupViewModelI {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val gitManager = MyApp.appModule.gitManager
    val uiHelper: UiHelper = MyApp.appModule.uiHelper

    private val storageManager = MyApp.appModule.storageManager

    private val _initState: MutableStateFlow<InitState> = MutableStateFlow(InitState.Idle)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    var provider: Provider? = null
        private set

    var repos = listOf<RepoInfo>()
        private set

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

    private var shouldCancel = false
    fun cancelClone(): Boolean {
        if (gitManager.isRepoInitialized) {
            return false
        }
        shouldCancel = true
        return true
    }

    fun setStateToIdle() {
        viewModelScope.launch {
            delay(50)
            _initState.emit(InitState.Idle)
        }
    }

    fun createLocalRepo(storageConfig: StorageConfiguration, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {

            storageConfig.prepareStorageRepoPath()

            NodeFs.Folder.fromPath(storageConfig.repoPath()).isEmptyDirectory().onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }


            gitManager.createRepo(storageConfig.repoPath()).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            prefs.applyGitAuthorDefaults(gitManager.currentSignature())
            prefs.initRepo(storageConfig)

            storageManager.updateDatabase()

            onSuccess()
        }

    }

    fun openRepo(storageConfig: StorageConfiguration, onSuccess: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            if (!NodeFs.Folder.fromPath(storageConfig.repoPath()).exist()) {
                val msg = uiHelper.getString(R.string.error_path_not_directory)
                uiHelper.makeToast(msg)
                return@launch
            }

            gitManager.openRepo(storageConfig.repoPath()).onFailure {
                uiHelper.makeToast(it.message)
                return@launch
            }

            prefs.applyGitAuthorDefaults(gitManager.currentSignature())
            prefs.initRepo(storageConfig)

            storageManager.updateDatabaseAndRepo()

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

    override fun launch(f: suspend () -> Unit) {
        viewModelScope.launch { f() }
    }

    private fun runCloneJob(f: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            f()
        }
    }

    override fun cloneRepo(
        storageConfig: StorageConfiguration,
        remoteUrl: String,
        cred: Cred?,
        onSuccess: () -> Unit
    ) {

        runCloneJob {
            cloneRepoInternal(
                storageConfig = storageConfig,
                remoteUrl = remoteUrl,
                cred = cred,
                onSuccess = onSuccess
            )
        }
    }

    suspend fun cloneRepoInternal(
        storageConfig: StorageConfiguration,
        remoteUrl: String,
        cred: Cred?,
        onSuccess: () -> Unit
    ) {
        shouldCancel = false

        storageConfig.applyUrlName(remoteUrl)
        storageConfig.prepareStorageRepoPath()

        _initState.emit(InitState.Cloning(0))

        gitManager.cloneRepo(
            repoPath = storageConfig.repoPath(),
            repoUrl = remoteUrl,
            cred = cred,
            progressCallback = {
                _initState.tryEmit(InitState.Cloning(it))
                !shouldCancel
            }
        ).onFailure {
            _initState.emit(InitState.Error(if (shouldCancel) "Clone canceled" else it.message))
            return
        }
        if (shouldCancel) return

        prefs.initRepo(storageConfig)
        prefs.remoteUrl.update(remoteUrl)

        prefs.updateCred(cred)
        prefs.applyGitAuthorDefaults(gitManager.currentSignature())

        storageManager.updateDatabase(
            progressCb = {
                viewModelScope.launch {
                    _initState.emit(
                        when (it) {
                            is Progress.GeneratingDatabase -> InitState.GeneratingDatabase(it.path)
                            Progress.Timestamps -> InitState.CalculatingTimestamps
                        }
                    )
                }
            }
        )

        onSuccess()

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

            _initState.emit(InitState.GettingAccessToken)
            val token = try {
                provider!!.exchangeCodeForAccessToken(code)
            } catch (e: Exception) {
                Log.e(TAG, "exchangeCodeForAccessToken: ${e.message}, $e")
                _initState.emit(InitState.Error(e.message))
                return@launch
            }
            prefs.appAuthToken.update(token)

            fetchInfos(token = token)
        }
    }

    fun fetchInfos(token: String) {
        CoroutineScope(Dispatchers.IO).launch {

            _initState.emit(InitState.FetchingRepos)

            repos = try {
                provider!!.fetchUserRepos(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "fetchUserRepos: ${e.message}, $e")
                _initState.emit(InitState.Error(e.message))
                return@launch
            }
            _initState.emit(InitState.GettingUserInfo)

            userInfo = try {
                provider!!.getUserInfo(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "getUserInfo: ${e.message}, $e")
                _initState.emit(InitState.Error(e.message))
                return@launch
            }

            Log.d(TAG, "emit: Success")
            _initState.emit(InitState.FetchingInfosSuccess)
        }
    }

    override fun cloneRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {

        runCloneJob {
            val (publicKey, privateKey) = generateSshKeysLib()

            _initState.emit(InitState.AddingDeployKey)
            try {
                provider!!.addDeployKeyToRepo(
                    token = prefs.appAuthToken.get(),
                    publicKey = publicKey,
                    fullRepoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "addDeployKey: ${e.message}, $e")
                _initState.emit(InitState.Error(e.message))
                return@runCloneJob
            }

            cloneRepoInternal(
                storageConfig = storageConfig,
                remoteUrl = provider!!.sshCloneUrlFromRepoName(repoName),
                cred = Cred.Ssh(
                    publicKey = publicKey,
                    privateKey = privateKey,
                    passphrase = null
                ),
                onSuccess = {
                    onSuccess()
                }
            )
        }
    }

    override fun createRepoAutomatic(
        repoName: String,
        storageConfig: StorageConfiguration,
        onSuccess: () -> Unit
    ) {

        runCloneJob {

            _initState.emit(InitState.CreatingRemoteRepo)
            try {
                provider!!.createNewRepo(
                    token = prefs.appAuthToken.get(),
                    repoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "createNewRepoOnRemoteGithub: ${e.message}, $e")
                _initState.emit(InitState.Error(e.message))
                return@runCloneJob
            }

            val (publicKey, privateKey) = generateSshKeysLib()

            _initState.emit(InitState.AddingDeployKey)
            try {
                provider!!.addDeployKeyToRepo(
                    token = prefs.appAuthToken.get(),
                    publicKey = publicKey,
                    fullRepoName = repoName
                )
            } catch (e: Exception) {
                Log.e(TAG, "addDeployKey: ${e.message}, $e")
                _initState.emit(InitState.Error(e.message))
                return@runCloneJob
            }

            cloneRepoInternal(
                storageConfig = storageConfig,
                remoteUrl = provider!!.sshCloneUrlFromRepoName(repoName),
                cred = Cred.Ssh(
                    publicKey = publicKey,
                    privateKey = privateKey,
                    passphrase = null
                ),
                onSuccess = {
                    onSuccess()
                }
            )
        }
    }

}


sealed class InitState {
    data object Idle : InitState()
    data class Error(val message: String? = null) : InitState()

    data object GettingAccessToken : InitState()
    data object FetchingRepos : InitState()
    data object GettingUserInfo : InitState()

    data object FetchingInfosSuccess : InitState()

    data object CreatingRemoteRepo : InitState()
    data object AddingDeployKey : InitState()

    data class Cloning(val percent: Int) : InitState()

    data object CalculatingTimestamps : InitState()
    data class GeneratingDatabase(val path: String) : InitState()


    fun message(): String {
        return when (this) {
            AddingDeployKey -> "Adding deploy key"
            CalculatingTimestamps -> "Calculating timestamps"
            is Cloning -> "Cloning: $percent %"
            CreatingRemoteRepo -> "Creating repository"
            is Error -> if (message != null) "Error: $message" else "Error"
            FetchingRepos -> "Fetching repositories"
            is GeneratingDatabase -> "Generating database, path: $path"
            GettingAccessToken -> "Getting the access token"
            GettingUserInfo -> "Getting user information"
            Idle -> ""
            FetchingInfosSuccess -> ""
        }
    }

    fun isLoading(): Boolean = this !is Idle && this !is Error && this !is FetchingInfosSuccess
}
