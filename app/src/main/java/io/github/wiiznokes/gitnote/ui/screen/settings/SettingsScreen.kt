package io.github.wiiznokes.gitnote.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.ui.destination.SettingsDestination
import io.github.wiiznokes.gitnote.ui.util.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.SettingsViewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController


// https://github.com/ReVanced/revanced-manager-compose/blob/dev/app/src/main/java/app/revanced/manager/ui/screen/settings/AboutSettingsScreen.kt
// https://github.com/ReVanced/revanced-manager-compose/blob/dev/app/src/main/java/app/revanced/manager/ui/screen/settings/LicensesScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    destination: SettingsDestination,
    onBackClick: () -> Unit,
    onStorageFailure: () -> Unit,
) {

    val navController =
        rememberNavController(startDestination = destination)

    BackHandler {
        if (navController.backstack.entries.size <= 1) {
            onBackClick()
        } else {
            navController.pop()
        }
    }


    val vm: SettingsViewModel = viewModel()


    AnimatedNavHost(
        controller = navController,
        transitionSpec = SettingsNavTransitionSpec
    ) {
        when (it) {
            SettingsDestination.Libraries -> {
                LibrariesScreen(
                    onBackClick = {
                        navController.pop()
                    }
                )
            }

            SettingsDestination.Logs -> {
                LogsScreen(
                    onBackClick = {
                        navController.pop()
                    }
                )
            }

            SettingsDestination.Main -> {
                MainSettingsScreen(
                    onBackClick = onBackClick,
                    navController = navController,
                    onCloseRepo = onStorageFailure,
                    vm = vm
                )
            }

            SettingsDestination.FolderFilters -> {
                FolderFiltersScreen(
                    onBackClick = {
                        navController.pop()
                    },
                    vm = vm
                )
            }
        }
    }
}

private object SettingsNavTransitionSpec : NavTransitionSpec<SettingsDestination> {

    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: SettingsDestination,
        to: SettingsDestination
    ): ContentTransform {

        return when (from) {
            SettingsDestination.FolderFilters -> slide(backWard = true)
            SettingsDestination.Libraries -> slide(backWard = true)
            SettingsDestination.Logs -> slide(backWard = true)
            SettingsDestination.Main -> slide()
        }
    }
}

