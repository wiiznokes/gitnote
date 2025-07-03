package io.github.wiiznokes.gitnote.ui.screen.init.remote

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.manager.generateSshKeysLib
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.CloneState
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import kotlinx.coroutines.launch

private const val TAG = "GenerateNewKeysWithProviderScreen"

@Composable
fun GenerateNewKeysWithProviderScreen(
    onBackClick: () -> Unit,
    vm: InitViewModel,
    storageConfig: StorageConfiguration,
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

        SelectionContainer {
            Text(publicKey.value)
        }

        val clipboardManager = LocalClipboard.current

        Button(
            onClick = {
                val data = ClipData(
                    ClipDescription(
                        "public ssh key",
                        arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    ),
                    ClipData.Item(publicKey.value)
                )

                vm.viewModelScope.launch {
                    clipboardManager.setClipEntry(ClipEntry(data))
                }
            }
        ) {
            Text("Copy key")
        }

        Button(
            onClick = {
                val (public, private) = generateSshKeysLib()
                publicKey.value = public
                privateKey.value = private
            }
        ) {
            Text("Regenerate key")
        }


        val uriHandler = LocalUriHandler.current

        Button(
            onClick = {
                // todo: change the link
                uriHandler.openUri(url)
            }
        ) {
            Text("Open deploy key webpage")
        }


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