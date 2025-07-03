package io.github.wiiznokes.gitnote.data

import android.content.Context
import android.os.Parcelable
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.manager.PreferencesManager
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.CredType
import io.github.wiiznokes.gitnote.ui.model.NoteMinWidth
import io.github.wiiznokes.gitnote.ui.model.Provider
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.ui.model.SortType
import io.github.wiiznokes.gitnote.ui.theme.Theme
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
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

    val databaseCommit = stringPreference("")

    private val repoPath = stringPreference("repoPath")

    suspend fun repoPath(): String {
        return when (repoState.get()) {
            RepoState.NoRepo -> throw Exception("calling repoPath function with no repo initialized")
            RepoState.AppStorage -> appStorageRepoPath
            RepoState.DeviceStorage -> repoPath.get()
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

    val privateKey = stringPreference("privateKey", "")
    val publicKey = stringPreference("publicKey", "")

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
                privateKey = this.privateKey.get(),
                publicKey = this.publicKey.get()
            )
        }
    }

    suspend fun updateCred(cred: Cred?) {
        when (cred) {
            is Cred.Ssh -> {
                credType.update(CredType.Ssh)
                privateKey.update(cred.privateKey)
                publicKey.update(cred.publicKey)
            }
            is Cred.UserPassPlainText -> {
                credType.update(CredType.UserPassPlainText)
                userPassUsername.update(cred.username)
                userPassPassword.update(cred.password)
            }
            null -> credType.update(CredType.None)
        }
    }

    val provider = enumPreference("provider", Provider.GitHub)

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


    val repoState = enumPreference("repoState", RepoState.NoRepo)

    suspend fun initRepo(repoState: NewRepoState) {
        when (repoState) {
            NewRepoState.AppStorage -> {
                this.repoState.update(RepoState.AppStorage)
            }

            is NewRepoState.DeviceStorage -> {
                this.repoState.update(RepoState.DeviceStorage)
                this.repoPath.update(repoState.path)
            }
        }
        lastOpenedFolder.update("")
    }

    suspend fun closeRepo() {
        repoState.update(RepoState.NoRepo)
        databaseCommit.update("")
    }

    val isReadOnlyModeActive = booleanPreference("isReadOnlyModeActive", false)

}


enum class RepoState {
    NoRepo,
    AppStorage,
    DeviceStorage
}

@Parcelize
sealed class NewRepoState : Parcelable {
    data object AppStorage : NewRepoState()
    class DeviceStorage(val path: String) : NewRepoState()

    fun repoPath(): String {
        return when (this) {
            AppStorage -> AppPreferences.appStorageRepoPath
            is DeviceStorage -> this.path
        }
    }
}
