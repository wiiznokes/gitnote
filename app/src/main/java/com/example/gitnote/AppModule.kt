package com.example.gitnote

import android.content.Context
import androidx.room.Room
import com.example.gitnote.data.AppPreferences
import com.example.gitnote.data.room.RepoDatabase
import com.example.gitnote.helper.UiHelper
import com.example.gitnote.manager.GitManager
import com.example.gitnote.manager.StorageManager


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
        Room
            .databaseBuilder(
                context = appContext,
                klass = RepoDatabase::class.java,
                name = "appDatabase"
            )
            .build()
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