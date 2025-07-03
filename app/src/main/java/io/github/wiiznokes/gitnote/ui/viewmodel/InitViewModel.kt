package io.github.wiiznokes.gitnote.ui.viewmodel


import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
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

    fun onReceiveCode(code: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = exchangeCodeForAccessToken(code)

                Log.d(TAG, "Access Token: $token")

                val repos = fetchUserRepos(token = token)
                for (repo in repos) {
                    Log.d(TAG, "$repo")
                }


                //createNewRepo(token = token, repoName = "repo_test2")

                val repoTest = repos.find {
                    it.fullRepoName == "wiiznokes/repo_test"
                }!!

                val info = getUserInfo(token = token)
                Log.d(TAG, "$info")

                val (pub, priv) = generateSshKeysLib()

                addDeployKey(token = token, publicKey = pub, fullRepoName = repoTest.fullRepoName)

                val cred = Cred.Ssh(username = "git", publicKey = pub, privateKey = priv)

                cloneRepo(StorageConfiguration.App, repoTest.url, cred, onSuccess = { println("clone successful")})

            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
            }
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
        val fullRepoName: String,
        val url: String,
        val lastModifiedTimeMillis: Long,
    )

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
                    fullRepoName = "$owner/$name",
                    url = url,
                    lastModifiedTimeMillis = timeMillis
                )
            )
        }


        repos.sortWith(compareByDescending { it.lastModifiedTimeMillis })

        return repos
    }

    fun createNewRepo(
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
        val name: String,
        val email: String,
        val username: String,
    )

    fun getUserInfo(token: String): UserInfo? {
        val url = URL("https://api.github.com/user")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Failed to fetch user info: HTTP $responseCode $error")
                null
            } else {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                UserInfo(
                    name = json.optString("name", ""),
                    email = json.optString("email", ""),
                    username = json.getString("login")
                )
            }
        } catch (e: Exception) {
            println("Error fetching user info: ${e.message}")
            null
        }
    }


    fun addDeployKey(token: String, publicKey: String, fullRepoName: String): Boolean {
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

        return try {
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                println("Deploy key added successfully")
                true
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("Failed to add deploy key: HTTP $responseCode $error")
                false
            }
        } catch (e: Exception) {
            println("Error adding deploy key: ${e.message}")
            false
        }
    }
}

sealed class CloneState {
    data object Idle : CloneState()
    data class Cloning(val percent: Int) : CloneState()
    data object Cloned : CloneState()
    data object Error : CloneState()


    fun isClickable(): Boolean = this is Idle || this is Error

}