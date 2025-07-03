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
import io.github.wiiznokes.gitnote.ui.model.Provider
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

//    val navController: NavController<RemoteDestination> =
//        rememberNavController(startDestination = AuthorizeGitNote(provider = Provider.GitHub))

    val navController: NavController<RemoteDestination> =
        rememberNavController(startDestination = PickRepo(provider = Provider.GitHub))

    NavBackHandler(navController)


    AnimatedNavHost(
        controller = navController,
        transitionSpec = RemoteNavTransitionSpec
    ) { remoteDestination ->

        when (remoteDestination) {
            is SelectProvider -> SelectProviderScreen(
                onBackClick = onBackClick,
                onProviderSelected = { provider ->
                    if (provider != null) {
                        navController.navigate(
                            SelectSetupAutomatically(
                                provider = provider
                            )
                        )
                    } else {
                        navController.navigate(
                            EnterUrl(
                                provider = null
                            )
                        )
                    }
                }
            )

            is SelectSetupAutomatically -> SelectSetupAutomaticallyScreen(
                onBackClick = { navController.pop() },
                onAutomatically = {
                    navController.navigate(
                        AuthorizeGitNote(
                            provider = remoteDestination.provider,
                        )
                    )
                },
                onManually = {
                    navController.navigate(
                        EnterUrl(
                            provider = remoteDestination.provider,
                        )
                    )
                }
            )

            is AuthorizeGitNote -> AuthorizeGitNoteScreen(
                onBackClick = { navController.pop() },
                vm = vm,
                onSuccess = {
                    navController.navigate(
                        PickRepo(
                            provider = remoteDestination.provider,
                        )
                    )
                }

            )
            is EnterUrl -> {

                if (remoteDestination.provider != null) {
                    EnterUrlWithProviderScreen(
                        onBackClick = { navController.pop() },
                        provider = remoteDestination.provider,
                        onUrl = { url ->
                            navController.navigate(
                                SelectGenerateNewKeys(
                                    provider = remoteDestination.provider,
                                    url = url
                                )
                            )
                        }
                    )
                } else {
                    EnterUrlScreen(
                        onBackClick = { navController.pop() },
                        onUrl = { url ->
                            navController.navigate(
                                SelectGenerateNewKeys(
                                    provider = remoteDestination.provider,
                                    url = url
                                )
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
                            provider = remoteDestination.provider,
                            url = remoteDestination.url
                        )
                    )
                },
                onCustom = {
                    navController.navigate(
                        LoadKeysFromDevice(
                            provider = remoteDestination.provider,
                            url = remoteDestination.url
                        )
                    )
                }
            )

            is GenerateNewKeys -> {

                if (remoteDestination.provider != null) {
                    GenerateNewKeysWithProviderScreen(
                        onBackClick = { navController.pop() },
                        onSuccess = onInitSuccess,
                        vm = vm,
                        provider = remoteDestination.provider,
                        url = remoteDestination.url,
                        storageConfig = storageConfig,
                    )
                } else {
                    GenerateNewKeysScreen(
                        onBackClick = { navController.pop() },
                    )
                }

            }
            is LoadKeysFromDevice -> LoadKeysFromDeviceScreen(
                onBackClick = { navController.pop() },
                onNext = { }
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

