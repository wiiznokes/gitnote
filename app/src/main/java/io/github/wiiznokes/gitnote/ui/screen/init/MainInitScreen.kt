package io.github.wiiznokes.gitnote.ui.screen.init

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import io.github.wiiznokes.gitnote.data.NewRepoState
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.destination.InitDestination
import io.github.wiiznokes.gitnote.ui.destination.NewRepoSource
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory
import kotlinx.coroutines.launch


private sealed class StorageChooser {
    data object UnExpanded: StorageChooser()
    data class Expanded(val source: NewRepoSource): StorageChooser()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController<InitDestination>,
    onInitSuccess: () -> Unit
) {
    val vm = viewModel<InitViewModel>(
        factory = viewModelFactory { InitViewModel() }
    )

    val showStorageChooser: MutableState<StorageChooser> = remember { mutableStateOf(StorageChooser.UnExpanded) }


    AppPage(
        title = "Choose method",
        verticalArrangement = Arrangement.spacedBy(80.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = {
                showStorageChooser.value = StorageChooser.Expanded(NewRepoSource.Create)
            }
        ) {
            Text(
                text = "Create local repository"
            )
        }


        Button(
            onClick = {
                showStorageChooser.value = StorageChooser.Expanded(NewRepoSource.Open)
            }
        ) {
            Text(
                text = "Open local repository"
            )
        }


        Button(
            onClick = {
                showStorageChooser.value = StorageChooser.Expanded(NewRepoSource.Clone)
            }
        ) {
            Text(
                text = "Clone remote repository"
            )
        }
    }


    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
     val  storageChooser = showStorageChooser.value

    if (storageChooser is StorageChooser.Expanded) {
        ModalBottomSheet(
            onDismissRequest = {
                showStorageChooser.value = StorageChooser.UnExpanded
            },
            sheetState = sheetState
        ) {
            
            fun closeSheet() {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showStorageChooser.value = StorageChooser.UnExpanded
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    closeSheet()
                    vm.initRepoWithSource(
                        repoState = NewRepoState.AppStorage,
                        newRepoSource = storageChooser.source,
                        navController = navController,
                        onSuccess = onInitSuccess
                    )
                },
                enabled = storageChooser.source != NewRepoSource.Open
            ) {
                Text(text = "Use app storage")
            }


            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.CenterHorizontally),
            ) {

                Text(
                    text = "Require full storage permission. You will be able to see the repository with a file explorer.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))


                val storagePermissionHelper = remember {
                    StoragePermissionHelper()
                }
                val (contract, permissionName) = storagePermissionHelper.permissionContract()

                val permissionLauncher = rememberLauncherForActivityResult(contract = contract) {
                    if (it) {
                        navController.navigate(
                            InitDestination.FileExplorer(
                                title = storageChooser.source.getExplorerTitle(),
                                path = vm.prefs.repoPathSafely(),
                                newRepoSource = storageChooser.source,
                            )
                        )
                    } else {
                        vm.uiHelper.makeToast("Need permission to access the storage.")
                    }
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        closeSheet()

                        if (!StoragePermissionHelper.isPermissionGranted()) {
                            permissionLauncher.launch(permissionName)
                        } else {
                            navController.navigate(
                                InitDestination.FileExplorer(
                                    title = storageChooser.source.getExplorerTitle(),
                                    path = vm.prefs.repoPathSafely(),
                                    newRepoSource = storageChooser.source,
                                )
                            )
                        }

                    }
                ) {
                    Text(text = "Use device storage")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

    }
}
