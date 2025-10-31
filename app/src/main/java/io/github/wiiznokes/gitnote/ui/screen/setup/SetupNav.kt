package io.github.wiiznokes.gitnote.ui.screen.setup

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.wiiznokes.gitnote.ui.destination.NewRepoMethod
import io.github.wiiznokes.gitnote.ui.destination.SetupDestination
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.screen.setup.remote.RemoteScreen
import io.github.wiiznokes.gitnote.ui.utils.crossFade
import io.github.wiiznokes.gitnote.ui.utils.slide
import io.github.wiiznokes.gitnote.ui.viewmodel.FileExplorerViewModel
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


    val useUrlForRootFolder = rememberSaveable { mutableStateOf(false) }


    AnimatedNavHost(
        controller = navController,
        transitionSpec = InitNavTransitionSpec
    ) { setupDestination ->
        when (setupDestination) {

            SetupDestination.Main -> NewRepoMethodScreen(
                createLocalRepo = vm::createLocalRepo,
                openRepo = vm::openRepo,
                makeToast = vm.uiHelper::makeToast,
                repoPath = vm.prefs.repoPathSafely(),
                navigate = navController::navigate,
                onSetupSuccess = onSetupSuccess,
            )

            is SetupDestination.FileExplorer -> {

                val path = setupDestination.path?.let {
                    it.ifEmpty {
                        null
                    }
                }

                val fileExplorerVm: FileExplorerViewModel = viewModel(
                    factory = viewModelFactory {
                        FileExplorerViewModel(
                            path = path
                        )
                    },
                    key = path
                )


                FileExplorerScreen(
                    currentDir = fileExplorerVm.currentDir,
                    onDirectoryClick = {
                        navController.navigate(
                            SetupDestination.FileExplorer(
                                path = it,
                                newRepoMethod = setupDestination.newRepoMethod
                            )
                        )
                    },
                    onFinish = { path, useUrlForRootFolder ->
                        val storageConfig = StorageConfiguration.Device(path, useUrlForRootFolder = useUrlForRootFolder)

                        when (setupDestination.newRepoMethod) {
                            NewRepoMethod.Create -> vm.createLocalRepo(
                                storageConfig,
                                onSetupSuccess
                            )

                            NewRepoMethod.Open -> vm.openRepo(storageConfig, onSetupSuccess)
                            NewRepoMethod.Clone -> {
                                if (useUrlForRootFolder || vm.checkPathForClone(storageConfig.repoPath()).isSuccess) {
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
                    title = setupDestination.newRepoMethod.getExplorerTitle(useUrlForRootFolder.value),
                    createDir = fileExplorerVm::createDir,
                    folders = fileExplorerVm.folders,
                    newRepoMethod = setupDestination.newRepoMethod,
                    useUrlForRootFolder = useUrlForRootFolder,
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