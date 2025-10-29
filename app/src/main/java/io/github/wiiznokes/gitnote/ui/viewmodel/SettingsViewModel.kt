package io.github.wiiznokes.gitnote.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val storageManager = MyApp.appModule.storageManager
    val uiHelper = MyApp.appModule.uiHelper

    fun update(f: suspend () -> Unit) {
        viewModelScope.launch {
            f()
        }
    }

    fun closeRepo() {
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.closeRepo()
        }
    }

    fun reloadDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val res = storageManager.updateDatabase(force = true)
            res.onFailure {
                uiHelper.makeToast("$it")
            }
            res.onSuccess {
                uiHelper.makeToast(uiHelper.getString(R.string.success_reload))
            }
        }
    }
}