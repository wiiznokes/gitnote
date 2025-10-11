package io.github.wiiznokes.gitnote.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.ui.destination.SettingsDestination
import io.github.wiiznokes.gitnote.ui.utils.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.SettingsViewModel


// https://github.com/ReVanced/revanced-manager-compose/blob/dev/app/src/main/java/app/revanced/manager/ui/screen/settings/AboutSettingsScreen.kt
// https://github.com/ReVanced/revanced-manager-compose/blob/dev/app/src/main/java/app/revanced/manager/ui/screen/settings/LicensesScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNav(
    destination: SettingsDestination,
    onBackClick: () -> Unit,
    onCloseRepo: () -> Unit,
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

            SettingsDestination.Logs -> {
                LogsScreen(
                    onBackClick = {
                        navController.pop()
                    },
                    vm = vm
                )
            }

            SettingsDestination.Main -> {
                SettingsScreen(
                    onBackClick = onBackClick,
                    navController = navController,
                    onCloseRepo = onCloseRepo,
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
            SettingsDestination.Logs -> slide(backWard = true)
            SettingsDestination.Main -> slide()
        }
    }
}

