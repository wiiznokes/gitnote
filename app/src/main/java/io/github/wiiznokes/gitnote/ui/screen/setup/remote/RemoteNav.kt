package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.AuthorizeGitNote
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.EnterUrl
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.GenerateNewKeys
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.PickRepo
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.SelectGenerateNewKeys
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.SelectProvider
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination.SelectSetupAutomatically
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.utils.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModel


private const val TAG = "RemoteScreen"


@Composable
fun RemoteScreen(
    vm: SetupViewModel,
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
                setProvider = { vm.setProvider(it) },
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
                authState = vm.initState.collectAsState().value,
                onSuccess = { navController.navigate(PickRepo) },
                getLaunchOAuthScreenIntent = { vm.getLaunchOAuthScreenIntent() },
                vmHashCode = vm.hashCode()
            )

            is EnterUrl -> {

                if (vm.provider != null) {
                    EnterUrlWithProviderScreen(
                        onBackClick = { navController.pop() },
                        provider = vm.provider!!,
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
                authStep2State = vm.initState.collectAsState().value,
                vm = vm,
                userInfo = vm.userInfo,
                repos = vm.repos,
                storageConfig = storageConfig,
                onSuccess = onInitSuccess,
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

            is GenerateNewKeys -> GenerateNewKeysScreen(
                onBackClick = { navController.pop() },
                cloneState = vm.initState.collectAsState().value,
                provider = vm.provider,
                storageConfig = storageConfig,
                url = remoteDestination.url,
                vm = vm,
                generateSshKeys = ::generateSshKeysLib,
                onSuccess = onInitSuccess,
            )
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

