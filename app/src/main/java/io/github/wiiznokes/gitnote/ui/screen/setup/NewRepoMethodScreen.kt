package io.github.wiiznokes.gitnote.ui.screen.setup

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.destination.NewRepoMethod
import io.github.wiiznokes.gitnote.ui.destination.SetupDestination
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.coroutines.launch


private const val TAG = "NewRepoMethodScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRepoMethodScreen(
    createLocalRepo: (StorageConfiguration, () -> Unit) -> Unit,
    openRepo: (StorageConfiguration, () -> Unit) -> Unit,
    makeToast: (String) -> Unit,
    repoPath: String,
    navigate: (SetupDestination) -> Unit,
    onSetupSuccess: () -> Unit
) {

    val storageChooserExpanded: MutableState<Boolean> =
        remember { mutableStateOf(false) }

    val newRepoMethod: MutableState<NewRepoMethod?> =
        remember { mutableStateOf(null) }

    val storagePermissionHelper = remember {
        StoragePermissionHelper()
    }
    val (contract, permissionName) = storagePermissionHelper.permissionContract()

    val permissionLauncher = rememberLauncherForActivityResult(contract = contract) {
        if (it) {
            val newRepoMethod = newRepoMethod.value!!
            navigate(
                SetupDestination.FileExplorer(
                    path = repoPath,
                    newRepoMethod = newRepoMethod,
                )
            )
        } else {
            makeToast(MyApp.appModule.context.getString(R.string.error_need_storage_permission))
        }
    }

    AppPage(
        title = stringResource(R.string.app_page_choose_method),
        verticalArrangement = Arrangement.spacedBy(80.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = {
                storageChooserExpanded.value = true
                newRepoMethod.value = NewRepoMethod.Create
            }
        ) {
            Text(
                text = stringResource(R.string.create_local_repo)
            )
        }


        Button(
            onClick = {
                newRepoMethod.value = NewRepoMethod.Open
                val newRepoMethod = newRepoMethod.value!!

                if (!StoragePermissionHelper.isPermissionGranted()) {
                    permissionLauncher.launch(permissionName)
                } else {
                    navigate(
                        SetupDestination.FileExplorer(
                            path = repoPath,
                            newRepoMethod = newRepoMethod,
                        )
                    )
                }
            }
        ) {
            Text(
                text = stringResource(R.string.open_repo)
            )
        }


        Button(
            onClick = {
                storageChooserExpanded.value = true
                newRepoMethod.value = NewRepoMethod.Clone
            }
        ) {
            Text(
                text = stringResource(R.string.clone_remote_repo)
            )
        }
    }


    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()


    if (storageChooserExpanded.value) {
        ModalBottomSheet(
            onDismissRequest = {
                storageChooserExpanded.value = false
            },
            sheetState = sheetState
        ) {

            fun closeSheet() {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        storageChooserExpanded.value = false
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    closeSheet()

                    val storageConfig = StorageConfiguration.App
                    when (newRepoMethod.value!!) {
                        NewRepoMethod.Create -> createLocalRepo(storageConfig, onSetupSuccess)
                        NewRepoMethod.Open -> openRepo(storageConfig, onSetupSuccess)
                        NewRepoMethod.Clone -> navigate(SetupDestination.Remote(storageConfig))
                    }
                }
            ) {
                Text(text = stringResource(R.string.use_app_storage))
            }


            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .align(Alignment.CenterHorizontally),
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        closeSheet()

                        if (!StoragePermissionHelper.isPermissionGranted()) {
                            permissionLauncher.launch(permissionName)
                        } else {
                            val newRepoMethod = newRepoMethod.value!!
                            navigate(
                                SetupDestination.FileExplorer(
                                    path = repoPath,
                                    newRepoMethod = newRepoMethod,
                                )
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.use_device_storage))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.use_device_storage_info),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

    }
}

@Preview
@Composable
private fun NewRepoMethodScreenPreview() {

    NewRepoMethodScreen(
        createLocalRepo = { _, _ -> },
        openRepo = { _, _ -> },
        makeToast = {},
        repoPath = "test",
        navigate = {},
        onSetupSuccess = {}
    )
}