package com.example.gitnote.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitnote.MyApp
import com.example.gitnote.data.AppPreferences
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
}