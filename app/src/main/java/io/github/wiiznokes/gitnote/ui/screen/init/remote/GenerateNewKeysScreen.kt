package io.github.wiiznokes.gitnote.ui.screen.init.remote

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.Provider
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.CloneState
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel

private const val TAG = "GenerateNewKeysWithProviderScreen"

@Composable
fun GenerateNewKeysWithProviderScreen(
    onBackClick: () -> Unit,
    vm: InitViewModel,
    storageConfig: StorageConfiguration,
    provider: Provider,
    url: String,
    onSuccess: () -> Unit
) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        val publicKey = rememberSaveable { mutableStateOf("") }
        var privateKey = rememberSaveable { mutableStateOf("") }

        LaunchedEffect(true) {
            val (public, private) = generateSshKeysLib()
            Log.d(TAG, public)
            publicKey.value = public
            privateKey.value = private
        }


        Text(publicKey.value)
        Button(onClick = {}) { Text("Copy key") }
        Button(
            onClick = {
                val (public, private) = generateSshKeysLib()
                publicKey.value = public
                privateKey.value = private
            }
        ) {
            Text("Regenerate key")
        }


        Button(onClick = {}) { Text("Open deploy key webpage") }


        val cloneState = vm.cloneState.collectAsState().value


        Button(
            onClick = {
                vm.cloneRepo(
                    storageConfig = storageConfig,
                    repoUrl = url,
                    cred = Cred.Ssh(
                        username = "git",
                        publicKey = publicKey.value,
                        privateKey = privateKey.value,
                    ),
                    onSuccess = onSuccess
                )

            },
            enabled = cloneState.isClickable()
        ) {
            Text("Clone repo")
        }


        if (cloneState is CloneState.Cloning) {
            Text(text = "${cloneState.percent} %")
        }
    }
}

@Composable
fun GenerateNewKeysScreen(
    onBackClick: () -> Unit,

    ) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Text("key")
        Button(onClick = {}) { Text("Copy key") }
        Button(onClick = {}) { Text("Regenerate key") }


        Button(onClick = {}) { Text("Clone repo") }
    }
}