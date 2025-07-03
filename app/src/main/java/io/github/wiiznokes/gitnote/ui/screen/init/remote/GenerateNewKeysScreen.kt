package io.github.wiiznokes.gitnote.ui.screen.init.remote

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import kotlinx.coroutines.launch

private const val TAG = "GenerateNewKeysWithProviderScreen"

private fun extractUserRepo(url: String): String? {
    val regex = Regex("""(?:git@|ssh://git@)[\w.-]+[:/](.+?)(?:\.git)?$""")
    val match = regex.find(url)
    return match?.groups?.get(1)?.value
}

@Composable
fun GenerateNewKeysScreen(
    onBackClick: () -> Unit,
    vm: InitViewModel,
    storageConfig: StorageConfiguration,
    url: String,
    onSuccess: () -> Unit
) {


    val cloneState = vm.initState.collectAsState().value

    val provider = vm.provider

    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = cloneState.isClickable()
    ) {

        val publicKey = rememberSaveable { mutableStateOf("") }
        var privateKey = rememberSaveable { mutableStateOf("") }

        LaunchedEffect(true) {
            val (public, private) = generateSshKeysLib()
            Log.d(TAG, public)
            publicKey.value = public
            privateKey.value = private
        }

        SetupPage(
            title = "In order to access this repository, this public key must be copied as a deploy key"
        ) {

            SetupLine(
                text = "1. Copy the key"
            ) {
                Text(
                    text = publicKey.value
                )


                val clipboardManager = LocalClipboard.current

                SetupButton(
                    text = "Copy Key",
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
                )

                SetupButton(
                    text = "Regenerate Key",
                    onClick = {
                        val (public, private) = generateSshKeysLib()
                        publicKey.value = public
                        privateKey.value = private
                    }
                )
            }
        }


        if (provider != null) {
            SetupLine(
                text = "2. Open webpage, and paste the deploy key. Make sure it is given Write Access."
            ) {
                val uriHandler = LocalUriHandler.current

                SetupButton(
                    text = "Open deploy key webpage",
                    onClick = {
                        val fullRepoName = extractUserRepo(url)
                        if (fullRepoName == null) {
                            Log.e(TAG, "can't parse full repo name: $url")
                        } else {
                            uriHandler.openUri(provider.deployKeyLink(fullRepoName))
                        }
                    }
                )
            }
        } else {
            SetupLine(
                text = "2. Add the public key to the git provider."
            ) {

            }
        }


        SetupLine(
            text = "3. Try Cloning"
        ) {

            SetupButton(
                text = if (cloneState.isLoading()) {
                    cloneState.message()
                } else "Clone Repo",
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
            )
        }
    }
}