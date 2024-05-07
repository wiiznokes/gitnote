package io.github.wiiznokes.gitnote.ui.screen.init

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.wiiznokes.gitnote.helper.StoragePermissionHelper
import io.github.wiiznokes.gitnote.ui.component.CenteredButton


@Composable
fun LocalStoragePermissionScreen(
    onSuccess: () -> Unit
) {

    val storagePermissionHelper = remember {
        StoragePermissionHelper()
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()

        ) {

            val (contract, permissionName) = storagePermissionHelper.permissionContract()

            val permissionLauncher = rememberLauncherForActivityResult(contract = contract) {
                if (it) {
                    onSuccess()
                }
            }
            CenteredButton(
                onClick = {
                    permissionLauncher.launch(permissionName)
                },
                text = "request storage permission"
            )
        }
    }
}
