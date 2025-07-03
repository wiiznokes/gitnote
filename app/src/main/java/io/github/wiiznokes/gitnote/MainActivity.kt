package io.github.wiiznokes.gitnote

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popAll
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.MyApp.Companion.appModule
import io.github.wiiznokes.gitnote.data.StorageConfig
import io.github.wiiznokes.gitnote.helper.NoteSaver
import io.github.wiiznokes.gitnote.ui.destination.AppDestination
import io.github.wiiznokes.gitnote.ui.destination.Destination
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.destination.InitDestination
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.screen.app.AppScreen
import io.github.wiiznokes.gitnote.ui.screen.init.InitScreen
import io.github.wiiznokes.gitnote.ui.theme.GitNoteTheme
import io.github.wiiznokes.gitnote.ui.theme.Theme
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }


    val vm: InitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")


        setContent {

            val theme by vm.prefs.theme.getAsState()
            val dynamicColor by vm.prefs.dynamicColor.getAsState()



            GitNoteTheme(
                darkTheme = theme == Theme.SYSTEM && isSystemInDarkTheme() || theme == Theme.DARK,
                dynamicColor = dynamicColor
            ) {

                val startDestination: Destination = remember {
                    if (runBlocking { vm.tryInit() }) {
                        if (NoteSaver.isEditUnsaved()) {
                            val saveInfo = NoteSaver.getSaveState()
                            if (saveInfo == null) {
                                Log.d(TAG, "can't retrieve the last saved note state")
                                Destination.App(AppDestination.Grid)
                            } else {
                                Log.d(TAG, "launch as EDIT_IS_UNSAVED")
                                Destination.App(
                                    AppDestination.Edit(EditParams.Saved(
                                        note = saveInfo.previousNote,
                                        editType = saveInfo.editType,
                                        name = saveInfo.name,
                                        content = saveInfo.content
                                    ))
                                )
                            }

                        } else {
                            Destination.App(
                                AppDestination.Grid
                                //AppDestination.Settings(SettingsDestination.Main)
                            )
                        }
                    }
                    else Destination.Init(InitDestination.Main)
//                    else Destination.Init(InitDestination.Remote(StorageConfiguration.App))
                }


                val navController =
                    rememberNavController(startDestination = startDestination)

                NavBackHandler(navController)

                AnimatedNavHost(
                    controller = navController
                ) { destination ->
                    when (destination) {
                        is Destination.Init -> {
                            InitScreen(
                                vm = vm,
                                startDestination = destination.initDestination,
                                onInitSuccess = {
                                    navController.popUpTo(
                                        inclusive = true
                                    ) {
                                        it is Destination.Init
                                    }
                                    navController.navigate(Destination.App(AppDestination.Grid))
                                }
                            )
                        }


                        is Destination.App -> AppScreen(
                            appDestination = destination.appDestination,
                            onStorageFailure = {
                                navController.popAll()
                                navController.navigate(Destination.Init(InitDestination.Main))
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent $intent")

        val uri = intent.data ?: return
        if (uri.scheme == "gitnote-identity" && uri.host == "register-callback") {
            val code = uri.getQueryParameter("code")

            if (code != null) {
                Log.d(TAG, "ViewModel in onNewIntent: ${vm.hashCode()}")
                vm.onReceiveCode(code)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy")

        appModule.gitManager.shutdown()
    }
}



