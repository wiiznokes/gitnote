package io.github.wiiznokes.gitnote.data

import android.content.Context
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.manager.PreferencesManager
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.CredType
import io.github.wiiznokes.gitnote.ui.model.NoteMinWidth
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.ui.model.SortType
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.theme.Theme
import kotlinx.coroutines.runBlocking
import kotlin.io.path.pathString

class AppPreferences(
    context: Context
) : PreferencesManager(context, "settings") {

    companion object {
        val appStorageRepoPath =
            MyApp.appModule.context.filesDir.toPath().resolve("repo").pathString
        const val DEFAULT_USERNAME = "gitnote"
    }

    val dynamicColor = booleanPreference("dynamicColor", true)
    val theme = enumPreference("theme", Theme.SYSTEM)

    val isInit = booleanPreference("isInit", false)
    val databaseCommit = stringPreference("")

    private val repoPath = stringPreference("repoPath")

    suspend fun repoPath(): String {
        if (!isInit.get()) {
            throw Exception("calling repoPath function with no repo initialized")
        }

        return when (storageConfig.get()) {
            StorageConfig.App -> appStorageRepoPath
            StorageConfig.Device -> repoPath.get()
        }
    }

    fun repoPathBlocking(): String = runBlocking { repoPath() }


    fun repoPathSafely(): String {
        return try {
            repoPathBlocking()
        } catch (e: Exception) {
            ""
        }
    }

    val remoteUrl = stringPreference("remoteUrl", "")

    val credType = enumPreference("credType", CredType.None)

    val username = stringPreference("username", "")

    suspend fun usernameOrDefault(): String =
        username.get().let { if (it.isNotEmpty()) it else DEFAULT_USERNAME }

    val userPassUsername = stringPreference("userPassUsername", "")
    val userPassPassword = stringPreference("userPassPassword", "")

    val sshUsername = stringPreference("sshUsername", "")
    val publicKey = stringPreference("publicKey", "")
    val privateKey = stringPreference("privateKey", "")

    suspend fun cred(): Cred? {
        return when (credType.get()) {
            CredType.None -> null
            CredType.UserPassPlainText -> {
                Cred.UserPassPlainText(
                    username = userPassUsername.get(),
                    password = userPassPassword.get()
                )
            }
            CredType.Ssh -> Cred.Ssh(
                username = this.sshUsername.get(),
                publicKey = this.publicKey.get(),
                privateKey = this.privateKey.get(),
            )
        }
    }

    suspend fun updateCred(cred: Cred?) {
        when (cred) {
            is Cred.Ssh -> {
                credType.update(CredType.Ssh)
                sshUsername.update(cred.username)
                publicKey.update(cred.publicKey)
                privateKey.update(cred.privateKey)
            }
            is Cred.UserPassPlainText -> {
                credType.update(CredType.UserPassPlainText)
                userPassUsername.update(cred.username)
                userPassPassword.update(cred.password)
            }
            null -> credType.update(CredType.None)
        }
    }

    val provider = enumPreference("provider", ProviderType.GitHub)

    val sortType = enumPreference("sortType", SortType.Modification)

    val sortOrder = enumPreference("sortOrder", SortOrder.Ascending)

    val noteMinWidth = enumPreference("noteMinWidth", NoteMinWidth.Default)
    val showFullNoteHeight = booleanPreference("showFullNoteHeight", false)

    val rememberLastOpenedFolder = booleanPreference("rememberLastOpenedFolder", false)
    val lastOpenedFolder = stringPreference("lastOpenedFolder", "")

    val showFullPathOfNotes = booleanPreference("showFullPathOfNotes", false)

    val defaultExtension = stringPreference("defaultExtension", "md")
    val showLinesNumber = booleanPreference("showLinesNumber", false)

    val folderFilters = setPreference(
        "folderFilters", setOf(
            ".*"
        )
    )

    val storageConfig = enumPreference("storageConfig", StorageConfig.App)

    suspend fun initRepo(storageConfig: StorageConfiguration) {
        databaseCommit.update("")
        isInit.update(true)

        when (storageConfig) {
            StorageConfiguration.App -> {
                this.storageConfig.update(StorageConfig.App)
            }

            is StorageConfiguration.Device -> {
                this.storageConfig.update(StorageConfig.Device)
                repoPath.update(storageConfig.path)
            }
        }
        lastOpenedFolder.update("")
    }

    suspend fun closeRepo() {
        isInit.update(false)
    }

    val isReadOnlyModeActive = booleanPreference("isReadOnlyModeActive", false)

}


enum class StorageConfig {
    App,
    Device
}

