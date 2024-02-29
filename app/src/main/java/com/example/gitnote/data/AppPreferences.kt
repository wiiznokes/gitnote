package com.example.gitnote.data

import android.content.Context
import com.example.gitnote.manager.PreferencesManager
import com.example.gitnote.ui.model.NoteMinWidth
import com.example.gitnote.ui.model.GitCreed
import com.example.gitnote.ui.model.Provider
import com.example.gitnote.ui.model.SortOrder
import com.example.gitnote.ui.model.SortType
import com.example.gitnote.ui.theme.Theme

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

    val showFullPathOfNotes = booleanPreference("showFullPathOfNotes", false)

    val defaultExtension = stringPreference("defaultExtension", "md")
    val showLinesNumber = booleanPreference("showLinesNumber", false)

    val folderFilters = setPreference(
        "folderFilters", setOf(
            ".*"
        )
    )

    suspend fun onCloseRepo() {
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
