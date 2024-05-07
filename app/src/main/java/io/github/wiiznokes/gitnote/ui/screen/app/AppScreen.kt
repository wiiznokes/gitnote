package io.github.wiiznokes.gitnote.ui.screen.app

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import io.github.wiiznokes.gitnote.ui.destination.AppDestination
import io.github.wiiznokes.gitnote.ui.destination.SettingsDestination
import io.github.wiiznokes.gitnote.ui.screen.app.grid.GridScreen
import io.github.wiiznokes.gitnote.ui.screen.settings.SettingsScreen
import io.github.wiiznokes.gitnote.ui.util.crossFade
import io.github.wiiznokes.gitnote.ui.util.slide
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController


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
                        navController.navigate(AppDestination.Edit(note, editType))
                    },
                    onStorageFailure = onStorageFailure
                )
            }

            is AppDestination.Edit -> EditScreen(
                initialNote = it.note,
                initialEditType = it.editType,
                onFinished = {
                    navController.pop()
                }
            )

            is AppDestination.Settings -> SettingsScreen(
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

