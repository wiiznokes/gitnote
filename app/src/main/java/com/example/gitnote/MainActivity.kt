package com.example.gitnote

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gitnote.MyApp.Companion.appModule
import com.example.gitnote.helper.StoragePermissionHelper
import com.example.gitnote.ui.destination.AppDestination
import com.example.gitnote.ui.destination.Destination
import com.example.gitnote.ui.destination.InitDestination
import com.example.gitnote.ui.screen.app.AppScreen
import com.example.gitnote.ui.screen.init.InitScreen
import com.example.gitnote.ui.theme.GitNoteTheme
import com.example.gitnote.ui.theme.Theme
import com.example.gitnote.ui.viewmodel.InitViewModel
import com.example.gitnote.ui.viewmodel.viewModelFactory
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.popAll
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            val vm = viewModel<InitViewModel>(
                factory = viewModelFactory { InitViewModel() }
            )

            val theme by vm.prefs.theme.getAsState()
            val dynamicColor by vm.prefs.dynamicColor.getAsState()



            GitNoteTheme(
                darkTheme = theme == Theme.SYSTEM && isSystemInDarkTheme() || theme == Theme.DARK,
                dynamicColor = dynamicColor
            ) {

                val startDestination: Destination = remember {
                    if (!StoragePermissionHelper.isPermissionGranted()) {
                        Destination.Init(InitDestination.LocalStoragePermission)
                    } else {
                        if (runBlocking { vm.tryInit() }) Destination.App(
                            AppDestination.Grid
                            //AppDestination.Settings(SettingsDestination.Main)
                        )
                        else Destination.Init(InitDestination.Main)
                    }
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
                                if (!StoragePermissionHelper.isPermissionGranted()) {
                                    navController.navigate(Destination.Init(InitDestination.LocalStoragePermission))
                                } else {
                                    navController.navigate(Destination.Init(InitDestination.Main))
                                }
                            }
                        )
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy")

        appModule.gitManager.shutdown()
    }
}



