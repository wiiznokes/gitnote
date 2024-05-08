package io.github.wiiznokes.gitnote

import android.content.Context
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.GitManager
import io.github.wiiznokes.gitnote.manager.StorageManager


interface AppModule {
    val repoDatabase: RepoDatabase
    val uiHelper: UiHelper
    val storageManager: StorageManager
    val gitManager: GitManager
    val appPreferences: AppPreferences
    val context: Context

}

class AppModuleImpl(
    override val context: Context
) : AppModule {

    override val repoDatabase: RepoDatabase by lazy {
        RepoDatabase.buildDatabase(context)
    }

    override val uiHelper: UiHelper by lazy {
        UiHelper(context)
    }
    override val storageManager: StorageManager by lazy {
        StorageManager()
    }
    override val gitManager: GitManager by lazy {
        GitManager()
    }
    override val appPreferences: AppPreferences by lazy {
        AppPreferences(context)
    }
}