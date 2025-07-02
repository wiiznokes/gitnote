package io.github.wiiznokes.gitnote.ui.screen.init

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
import io.github.wiiznokes.gitnote.data.NewRepoState
import io.github.wiiznokes.gitnote.ui.destination.InitDestination
import io.github.wiiznokes.gitnote.ui.destination.NewRepoSource
import io.github.wiiznokes.gitnote.ui.screen.init.remote.RemoteScreen
import io.github.wiiznokes.gitnote.ui.util.crossFade
import io.github.wiiznokes.gitnote.ui.util.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory

private const val TAG = "InitScreen"

@Composable
fun InitScreen(
    startDestination: InitDestination,
    onInitSuccess: () -> Unit,
) {
    val vm = viewModel<InitViewModel>(
        factory = viewModelFactory { InitViewModel() }
    )

    val navController =
        rememberNavController(startDestination = startDestination)

    NavBackHandler(navController)


    AnimatedNavHost(
        controller = navController,
        transitionSpec = InitNavTransitionSpec
    ) { initDestination ->
        when (initDestination) {

            InitDestination.Main -> MainScreen(
                navController = navController,
                onInitSuccess = onInitSuccess,
            )


            is InitDestination.FileExplorer -> {


                FileExplorerScreen(
                    path = initDestination.path?.let {
                        it.ifEmpty {
                            null
                        }
                    },
                    onDirectoryClick = {
                        navController.navigate(
                            InitDestination.FileExplorer(
                                title = initDestination.title,
                                path = it,
                                newRepoSource = initDestination.newRepoSource
                            )
                        )
                    },
                    onFinish = { path ->
                        val repoState = NewRepoState.DeviceStorage(path)

                        when (initDestination.newRepoSource) {
                            NewRepoSource.Create -> vm.createRepo(repoState, onInitSuccess)
                            NewRepoSource.Open -> vm.openRepo(repoState, onInitSuccess)
                            NewRepoSource.Clone -> {
                                vm.checkPathForClone(repoState.repoPath()).onSuccess {
                                    navController.navigate(
                                        InitDestination.Remote(repoState)
                                    )
                                }
                            }
                        }
                    },
                    onBackClick = {
                        navController.popUpTo(inclusive = false) {
                            it !is InitDestination.FileExplorer
                        }
                    },
                    title = initDestination.title
                )
            }

            is InitDestination.Remote -> RemoteScreen(
                vm = vm,
                repoState = initDestination.repoState,
                onInitSuccess = onInitSuccess,
                onBackClick = {
                    navController.pop()
                }
            )
        }
    }
}

private object InitNavTransitionSpec : NavTransitionSpec<InitDestination> {


    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: InitDestination,
        to: InitDestination
    ): ContentTransform {

        return when (from) {
            is InitDestination.FileExplorer -> {
                when (to) {
                    is InitDestination.FileExplorer -> {
                        //val toParent = (from.path?.length ?: 0) > (to.path?.length ?: 0)
                        crossFade()
                    }

                    InitDestination.Main -> slide(backWard = true)
                    is InitDestination.Remote -> slide()
                }
            }

            InitDestination.Main -> {
                when (to) {
                    is InitDestination.FileExplorer -> slide()
                    InitDestination.Main -> crossFade()
                    is InitDestination.Remote -> slide()
                }
            }

            is InitDestination.Remote -> {
                when (to) {
                    is InitDestination.FileExplorer -> slide(backWard = true)
                    InitDestination.Main -> slide(backWard = true)
                    is InitDestination.Remote -> crossFade()
                }
            }
        }
    }
}