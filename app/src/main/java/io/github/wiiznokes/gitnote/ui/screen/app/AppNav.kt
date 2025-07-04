package io.github.wiiznokes.gitnote.ui.screen.app

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.ui.destination.AppDestination
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.destination.SettingsDestination
import io.github.wiiznokes.gitnote.ui.screen.app.edit.EditScreen
import io.github.wiiznokes.gitnote.ui.screen.app.grid.GridScreen
import io.github.wiiznokes.gitnote.ui.screen.settings.SettingsNav
import io.github.wiiznokes.gitnote.ui.utils.crossFade
import io.github.wiiznokes.gitnote.ui.utils.slide


private const val TAG = "AppScreen"

@Composable
fun AppScreen(
    appDestination: AppDestination,
    onStorageFailure: () -> Unit,
) {

    val navController =
        rememberNavController(startDestination = appDestination)

    NavBackHandler(navController)

    AnimatedNavHost(
        controller = navController,
        transitionSpec = AppNavTransitionSpec
    ) {
        when (it) {

            is AppDestination.Grid -> {
                GridScreen(
                    onSettingsClick = {
                        navController.navigate(
                            AppDestination.Settings(
                                SettingsDestination.Main
                            )
                        )
                    },
                    onEditClick = { note, editType ->
                        navController.navigate(AppDestination.Edit(EditParams.Idle(note, editType)))
                    },
                    onStorageFailure = onStorageFailure
                )
            }

            is AppDestination.Edit -> EditScreen(
                editParams = it.params,
                onFinished = {
                    navController.pop()

                    if (it.params is EditParams.Saved) {
                        navController.navigate(AppDestination.Grid)
                    }
                }
            )

            is AppDestination.Settings -> SettingsNav(
                onBackClick = { navController.pop() },
                destination = it.settingsDestination,
                onStorageFailure = onStorageFailure
            )
        }
    }
}

private object AppNavTransitionSpec : NavTransitionSpec<AppDestination> {

    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: AppDestination,
        to: AppDestination
    ): ContentTransform {

        return when (from) {
            is AppDestination.Edit -> crossFade()
            AppDestination.Grid -> {
                if (to is AppDestination.Settings) {
                    slide()
                } else {
                    crossFade()
                }
            }

            is AppDestination.Settings -> slide(backWard = true)
        }
    }
}

