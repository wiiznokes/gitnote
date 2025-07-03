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
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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

    private val clientId = "Ov23li8EPatIAsWPt9QT"

    // storing this secret in the repo is "ok"
    // the only risk is github app reputation and quotas
    // it would require a server to not store it here
    private val clientSecret = "12f3f4742855deaafb45e798bcc635608b9d6fe6"

    fun getLaunchOAuthScreenIntent(): Intent {
        val authUrl = "https://github.com/login/oauth/authorize?client_id=$clientId&scope=repo"
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
                exchangeCodeForAccessToken(code)
            } catch (e: Exception) {
                Log.e(TAG, "exchangeCodeForAccessToken: ${e.message}, $e")
                _authState.emit(AuthState.Error)
                return@launch
            }

            _authState.emit(AuthState.FetchRepos)

            repos = try {
                fetchUserRepos(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "fetchUserRepos: ${e.message}, $e")
                _authState.emit(AuthState.Error)
                return@launch
            }
            _authState.emit(AuthState.GetUserInfo)

            userInfo = try {
                getUserInfo(token = token)
            } catch (e: Exception) {
                Log.e(TAG, "getUserInfo: ${e.message}, $e")
                _authState.emit(AuthState.Error)
                return@launch
            }

            Log.d(TAG, "emit: Success")
            _authState.emit(AuthState.Success)
        }
    }

    private fun exchangeCodeForAccessToken(code: String): String {

        val url = URL("https://github.com/login/oauth/access_token")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val body = "client_id=$clientId&client_secret=$clientSecret&code=$code"
        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val accessToken = json.getString("access_token")

        return accessToken
    }

    data class RepoInfo(
        val name: String,
        val owner: String,
        val url: String,
        val lastModifiedTimeMillis: Long,
    ) {
        val fullRepoName = "$owner/$name"
    }

    private fun fetchUserRepos(token: String): List<RepoInfo> {
        val url = URL("https://api.github.com/user/repos?page=1&per_page=100")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val response = connection.inputStream.bufferedReader().use { it.readText() }


        val repos = mutableListOf<RepoInfo>()


        val jsonArray = JSONArray(response)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        for (i in 0 until jsonArray.length()) {
            val repo = jsonArray.getJSONObject(i)
            val name = repo.getString("name")
            val owner = repo.getJSONObject("owner").getString("login")
            val url = repo.getString("ssh_url")
            val updatedAt = repo.getString("updated_at")
            val timeMillis = dateFormat.parse(updatedAt)?.time ?: 0L

            repos.add(
                RepoInfo(
                    owner = owner,
                    name = name,
                    url = url,
                    lastModifiedTimeMillis = timeMillis
                )
            )
        }


        repos.sortWith(compareByDescending { it.lastModifiedTimeMillis })

        return repos
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
                addDeployKey(
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
                repoUrl = "git@github.com:$repoName.git",
                cred = Cred.Ssh(
                    username = "git",
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
                createNewRepoOnRemoteGithub(
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
                addDeployKey(
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
                repoUrl = "git@github.com:$repoName.git",
                cred = Cred.Ssh(
                    username = "git",
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

    fun createNewRepoOnRemoteGithub(
        token: String,
        repoName: String,
        description: String = "",
        isPrivate: Boolean = true
    ): Boolean {
        val url = URL("https://api.github.com/user/repos")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject().apply {
            put("name", repoName)
            put("description", description)
            put("private", isPrivate)
        }

        connection.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode

        return if (responseCode in 200..299) {
            println("Repo created successfully")
            true
        } else {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e(TAG, "Failed to create repo: HTTP $responseCode $error")
            false
        }

    }

    data class UserInfo(
        val username: String,
        val name: String,
        val email: String,
    )

    fun getUserInfo(token: String): UserInfo {
        val url = URL("https://api.github.com/user")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("Failed to fetch user info: HTTP $responseCode $error")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        return UserInfo(
            username = json.getString("login"),
            name = json.optString("name", ""),
            email = json.optString("email", ""),
        )
    }


    fun addDeployKey(token: String, publicKey: String, fullRepoName: String) {
        val url = URL("https://api.github.com/repos/$fullRepoName/keys")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject().apply {
            put("title", "GitNote")
            put("key", publicKey)
            put("read_only", false)
        }

        connection.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("Failed to add deploy key: HTTP $responseCode $error")
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