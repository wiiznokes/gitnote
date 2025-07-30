package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.provider.GithubProvider
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelI
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelMock

private const val TAG = "GenerateNewKeysWithProviderScreen"

private fun extractUserRepo(url: String): String? {
    val regex = Regex("""(?:git@|ssh://git@)[\w.-]+[:/](.+?)(?:\.git)?$""")
    val match = regex.find(url)
    return match?.groups?.get(1)?.value
}

@Composable
fun GenerateNewKeysScreen(
    onBackClick: () -> Unit,
    cloneState: InitState,
    provider: Provider?,
    storageConfig: StorageConfiguration,
    url: String,
    vm: SetupViewModelI,
    generateSshKeys: () -> Pair<String, String>,
    onSuccess: () -> Unit,
) {

    AppPage(
        title = stringResource(R.string.ssh_keys_title),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = cloneState.isClickable()
    ) {

        val publicKey = rememberSaveable { mutableStateOf("") }
        val privateKey = rememberSaveable { mutableStateOf("") }

        LaunchedEffect(true) {
            val (public, private) = generateSshKeys()
            Log.d(TAG, public)
            publicKey.value = public
            privateKey.value = private
        }

        SetupPage(
            title = stringResource(R.string.ssh_keys_setup_title)
        ) {

            SetupLine(
                text = "1. " + stringResource(R.string.copy_the_key)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(8.dp)
                            .horizontalScroll(rememberScrollState()),
                        text = publicKey.value,
                        maxLines = 1
                    )
                }


                val clipboardManager = LocalClipboard.current

                SetupButton(
                    text = stringResource(R.string.copy_key),
                    onClick = {
                        val data = ClipData(
                            ClipDescription(
                                "public ssh key",
                                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            ),
                            ClipData.Item(publicKey.value)
                        )

                        vm.launch {
                            clipboardManager.setClipEntry(ClipEntry(data))
                        }
                    }
                )

                SetupButton(
                    text = stringResource(R.string.regenerate_key),
                    onClick = {
                        val (public, private) = generateSshKeys()
                        publicKey.value = public
                        privateKey.value = private
                    }
                )
            }



            if (provider != null) {
                SetupLine(
                    text = "2. " + stringResource(R.string.paste_deploy_key)
                ) {
                    val uriHandler = LocalUriHandler.current

                    SetupButton(
                        text = stringResource(R.string.open_deploy_key_webpage),
                        onClick = {
                            val fullRepoName = extractUserRepo(url)
                            if (fullRepoName == null) {
                                Log.e(TAG, "can't parse full repo name: $url")
                            } else {
                                uriHandler.openUri(provider.deployKeyLink(fullRepoName))
                            }
                        },
                        link = true
                    )
                }
            } else {
                SetupLine(
                    text = "2. " + stringResource(R.string.paste_deploy_key_no_provider)
                ) {

                }
            }


            SetupLine(
                text = "3. " + stringResource(R.string.try_cloning)
            ) {

                SetupButton(
                    text = if (cloneState.isLoading()) {
                        cloneState.message()
                    } else stringResource(R.string.clone_repo),
                    onClick = {
                        vm.cloneRepo(
                            storageConfig = storageConfig,
                            remoteUrl = url,
                            cred = Cred.Ssh(
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
}

@Preview
@Composable
private fun GenerateNewKeysScreenPreview() {
    GenerateNewKeysScreen(
        onBackClick = {},
        cloneState = InitState.CloneState.Idle,
        provider = GithubProvider(),
        storageConfig = StorageConfiguration.App,
        url = "url",
        vm = SetupViewModelMock(),
        generateSshKeys = { "aaaaaaaaaaaabbbbbbbbbbbbb" to "aaaaaaaaaaaabbbbbbbbbbbbb" },
        onSuccess = {}
    )

}