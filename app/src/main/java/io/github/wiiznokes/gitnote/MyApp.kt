package io.github.wiiznokes.gitnote

import android.app.Application
import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

const val TAG = "MyApp (Application)"
class MyApp : Application() {

    companion object {
        lateinit var appModule: AppModule
    }

    private val scope = MainScope()
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        appModule = AppModuleImpl(this)


        scope.launch {
            appModule.appPreferences.preload()
        }
    }
}