package io.github.wiiznokes.gitnote.data

import android.content.Context
import io.github.wiiznokes.gitnote.manager.PreferencesManager
import io.github.wiiznokes.gitnote.ui.model.GitCreed
import io.github.wiiznokes.gitnote.ui.model.NoteMinWidth
import io.github.wiiznokes.gitnote.ui.model.Provider
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.ui.model.SortType
import io.github.wiiznokes.gitnote.ui.theme.Theme

class AppPreferences(
    context: Context
) : PreferencesManager(context, "settings") {
    val dynamicColor = booleanPreference("dynamicColor", true)
    val theme = enumPreference("theme", Theme.SYSTEM)


    val isRepoInitialize = booleanPreference("isRepoInitialize", false)
    val databaseCommit = stringPreference("")

    val repoPath = stringPreference("repoPath")
    val remoteUrl = stringPreference("remoteUrl", "")
    val userName = stringPreference("userName", "")
    val password = stringPreference(
        "password",
        ""
    )
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

    suspend fun initRepo(repoPath: String) {
        isRepoInitialize.update(true)
        this.repoPath.update(repoPath)
        lastOpenedFolder.update("")
    }

    suspend fun closeRepo() {
        isRepoInitialize.update(false)
        databaseCommit.update("")
    }

    suspend fun gitCreed(): GitCreed? {
        val userName = this.userName.get()
        val password = this.password.get()

        return if (userName.isEmpty() or password.isEmpty()) {
            null
        } else {
            GitCreed(
                userName = userName,
                password = password
            )
        }
    }


}
