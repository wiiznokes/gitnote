package io.github.wiiznokes.gitnote

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MyApp : Application() {

    companion object {
        lateinit var appModule: AppModule
    }

    private val scope = MainScope()
    override fun onCreate() {
        super.onCreate()

        appModule = AppModuleImpl(this)


        scope.launch {
            appModule.appPreferences.preload()
        }
    }
}