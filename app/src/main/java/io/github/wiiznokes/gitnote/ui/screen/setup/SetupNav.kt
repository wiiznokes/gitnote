package io.github.wiiznokes.gitnote.ui.screen.setup

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.popUpTo
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.ui.destination.SetupDestination
import io.github.wiiznokes.gitnote.ui.destination.NewRepoMethod
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.screen.setup.remote.RemoteScreen
import io.github.wiiznokes.gitnote.ui.util.crossFade
import io.github.wiiznokes.gitnote.ui.util.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharedFlow

private const val TAG = "SetupNav"

@Composable
fun SetupNav(
    startDestination: SetupDestination,
    onSetupSuccess: () -> Unit,
    authFlow: SharedFlow<String>
) {

    val vm: SetupViewModel = viewModel(
        factory = viewModelFactory {
            SetupViewModel(
                authFlow = authFlow
            )
        },

    )

    val navController =
        rememberNavController(startDestination = startDestination)

    NavBackHandler(navController)


    AnimatedNavHost(
        controller = navController,
        transitionSpec = InitNavTransitionSpec
    ) { setupDestination ->
        when (setupDestination) {

            SetupDestination.Main -> NewRepoMethodScreen(
                vm = vm,
                navController = navController,
                onSetupSuccess = onSetupSuccess,
            )

            is SetupDestination.FileExplorer -> {


                FileExplorerScreen(
                    path = setupDestination.path?.let {
                        it.ifEmpty {
                            null
                        }
                    },
                    onDirectoryClick = {
                        navController.navigate(
                            SetupDestination.FileExplorer(
                                title = setupDestination.title,
                                path = it,
                                newRepoMethod = setupDestination.newRepoMethod
                            )
                        )
                    },
                    onFinish = { path ->
                        val storageConfig = StorageConfiguration.Device(path)

                        when (setupDestination.newRepoMethod) {
                            NewRepoMethod.Create -> vm.createLocalRepo(storageConfig, onSetupSuccess)
                            NewRepoMethod.Open -> vm.openRepo(storageConfig, onSetupSuccess)
                            NewRepoMethod.Clone -> {
                                vm.checkPathForClone(storageConfig.repoPath()).onSuccess {
                                    navController.navigate(
                                        SetupDestination.Remote(storageConfig)
                                    )
                                }
                            }
                        }
                    },
                    onBackClick = {
                        navController.popUpTo(inclusive = false) {
                            it !is SetupDestination.FileExplorer
                        }
                    },
                    title = setupDestination.title
                )
            }

            is SetupDestination.Remote -> RemoteScreen(
                vm = vm,
                storageConfig = setupDestination.storageConfig,
                onInitSuccess = onSetupSuccess,
                onBackClick = {
                    navController.pop()
                }
            )
        }
    }
}

private object InitNavTransitionSpec : NavTransitionSpec<SetupDestination> {


    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: SetupDestination,
        to: SetupDestination
    ): ContentTransform {

        return when (from) {
            is SetupDestination.FileExplorer -> {
                when (to) {
                    is SetupDestination.FileExplorer -> {
                        //val toParent = (from.path?.length ?: 0) > (to.path?.length ?: 0)
                        crossFade()
                    }

                    SetupDestination.Main -> slide(backWard = true)
                    is SetupDestination.Remote -> slide()
                }
            }

            SetupDestination.Main -> {
                when (to) {
                    is SetupDestination.FileExplorer -> slide()
                    SetupDestination.Main -> crossFade()
                    is SetupDestination.Remote -> slide()
                }
            }

            is SetupDestination.Remote -> {
                when (to) {
                    is SetupDestination.FileExplorer -> slide(backWard = true)
                    SetupDestination.Main -> slide(backWard = true)
                    is SetupDestination.Remote -> crossFade()
                }
            }
        }
    }
}