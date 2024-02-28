package com.example.gitnote.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.example.gitnote.MyApp
import com.example.gitnote.data.AppPreferences
import com.example.gitnote.data.platform.FileSystem
import com.example.gitnote.data.platform.FolderFs
import com.example.gitnote.helper.UiHelper


class FileExplorerViewModel(val path: String?) : ViewModel() {

    companion object {
        const val TAG = "FileExplorerViewModel"
    }

    val uiHelper: UiHelper = MyApp.appModule.uiHelper
    val prefs: AppPreferences = MyApp.appModule.appPreferences


    var currentDir: FolderFs = path?.let { path ->
        // todo: check if exist
        FolderFs.fromPath(path).let {
            if (it.exist()) it else null
        }
    } ?: FileSystem.defaultDir

    val folders: SnapshotStateList<FolderFs> = mutableStateListOf()

    init {
        folders.addAll(currentDir.listFolder())
    }

    fun createDir(name: String): Boolean {

        // todo: add it to the list
        currentDir.createFolder(name).onFailure {
            uiHelper.makeToast(it.message)
            return false
        }

        Log.d(TAG, "$name created")
        return true
    }
}