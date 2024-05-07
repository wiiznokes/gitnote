package io.github.wiiznokes.gitnote

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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

}

class AppModuleImpl(
    private val appContext: Context
) : AppModule {

    override val repoDatabase: RepoDatabase by lazy {
        RepoDatabase.buildDatabase(appContext)
    }

    override val uiHelper: UiHelper by lazy {
        UiHelper(appContext)
    }
    override val storageManager: StorageManager by lazy {
        StorageManager()
    }
    override val gitManager: GitManager by lazy {
        GitManager()
    }
    override val appPreferences: AppPreferences by lazy {
        AppPreferences(appContext)
    }
}