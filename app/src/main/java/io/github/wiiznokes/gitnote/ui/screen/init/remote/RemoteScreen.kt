package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.*
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.util.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel


private const val TAG = "RemoteScreen"


@Composable
fun RemoteScreen(
    vm: InitViewModel,
    storageConfig: StorageConfiguration,
    onInitSuccess: () -> Unit,
    onBackClick: () -> Unit
) {

    val navController: NavController<RemoteDestination> =
        rememberNavController(startDestination = SelectProvider)

    NavBackHandler(navController)


    AnimatedNavHost(
        controller = navController,
        transitionSpec = RemoteNavTransitionSpec
    ) { remoteDestination ->

        when (remoteDestination) {
            is SelectProvider -> SelectProviderScreen(
                onBackClick = onBackClick,
                vm = vm,
                onProviderSelected = {
                    if (vm.provider != null) {
                        navController.navigate(SelectSetupAutomatically)
                    } else {
                        navController.navigate(EnterUrl)
                    }
                }
            )

            is SelectSetupAutomatically -> SelectSetupAutomaticallyScreen(
                onBackClick = { navController.pop() },
                onAutomatically = { navController.navigate(AuthorizeGitNote) },
                onManually = { navController.navigate(EnterUrl) }
            )

            is AuthorizeGitNote -> AuthorizeGitNoteScreen(
                onBackClick = { navController.pop() },
                vm = vm,
                onSuccess = { navController.navigate(PickRepo) }

            )

            is EnterUrl -> {

                if (vm.provider != null) {
                    EnterUrlWithProviderScreen(
                        onBackClick = { navController.pop() },
                        onUrl = { url ->
                            navController.navigate(
                                SelectGenerateNewKeys(url = url)
                            )
                        }
                    )
                } else {
                    EnterUrlScreen(
                        onBackClick = { navController.pop() },
                        onUrl = { url ->
                            navController.navigate(
                                SelectGenerateNewKeys(url = url)
                            )
                        }
                    )
                }

            }

            is PickRepo -> PickRepoScreen(
                onBackClick = { navController.pop() },
                onSuccess = onInitSuccess,
                vm = vm,
                storageConfig = storageConfig,
            )

            is SelectGenerateNewKeys -> SelectGenerateNewKeysScreen(
                onBackClick = { navController.pop() },
                onGenerate = {
                    navController.navigate(
                        GenerateNewKeys(
                            url = remoteDestination.url
                        )
                    )
                },
            )

            is GenerateNewKeys -> {

                if (vm.provider != null) {
                    GenerateNewKeysWithProviderScreen(
                        onBackClick = { navController.pop() },
                        onSuccess = onInitSuccess,
                        vm = vm,
                        url = remoteDestination.url,
                        storageConfig = storageConfig,
                    )
                } else {
                    GenerateNewKeysScreen(
                        onBackClick = { navController.pop() },
                    )
                }

            }
        }

    }
}


private object RemoteNavTransitionSpec : NavTransitionSpec<RemoteDestination> {


    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: RemoteDestination,
        to: RemoteDestination
    ): ContentTransform {

        return when (action) {
            is NavAction.Navigate -> slide()
            is NavAction.Pop -> slide(backWard = true)
            else -> TODO()
        }

    }
}

