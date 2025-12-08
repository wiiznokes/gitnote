package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelI
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelMock


private fun isKeyCorrect(key: String): Boolean {
    return true
}


@Composable
fun LoadKeysFromDeviceScreen(
    onBackClick: () -> Unit,
    cloneState: InitState,
    storageConfig: StorageConfiguration,
    url: String,
    vm: SetupViewModelI,
    onClone: () -> Unit,
    onSuccess: () -> Unit,

    ) {
    AppPage(
        title = stringResource(R.string.custom_ssh_keys),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = !cloneState.isLoading()
    ) {

        SetupPage {
            val publicKey = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }

            val privateKey = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }

            val privateKeyPassword = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }

            SetupLine(
                text = stringResource(R.string.public_key),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = publicKey.value,
                    onValueChange = {
                        publicKey.value = it
                    },
                    label = {
                        Text(text = stringResource(R.string.public_key))
                    },
                    isError = !isKeyCorrect(publicKey.value.text),
                )

                LoadFileButton(
                    onTextLoaded = {
                        publicKey.value = publicKey.value.copy(text = it)
                    }
                )
            }

            SetupLine(
                text = stringResource(R.string.private_key),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = privateKey.value,
                    onValueChange = {
                        privateKey.value = it
                    },
                    label = {
                        Text(text = stringResource(R.string.private_key))
                    },
                    isError = !isKeyCorrect(privateKey.value.text),
                )

                LoadFileButton(
                    onTextLoaded = {
                        privateKey.value = privateKey.value.copy(text = it)
                    }
                )
            }

            SetupLine(
                text = stringResource(R.string.private_key_password)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = privateKeyPassword.value,
                    onValueChange = {
                        privateKeyPassword.value = it
                    },
                    label = {
                        Text(text = stringResource(R.string.private_key_password))
                    },
                )
            }

            SetupLine(
                text = stringResource(R.string.try_cloning)
            ) {
                SetupButton(
                    text = stringResource(R.string.clone_repo),
                    enabled = isKeyCorrect(publicKey.value.text) && isKeyCorrect(privateKey.value.text),
                    onClick = {
                        vm.cloneRepo(
                            storageConfig = storageConfig,
                            remoteUrl = url,
                            cred = Cred.Ssh(
                                publicKey = publicKey.value.text,
                                privateKey = privateKey.value.text,
                                passphrase = privateKeyPassword.value.text.ifEmpty { null }
                            ),
                            onSuccess = onSuccess
                        )

                        onClone()
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadFileButton(
    onTextLoaded: (String) -> Unit
) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                .use { it?.readText().orEmpty() }

            onTextLoaded(content)
        }
    }

    Button(
        onClick = {
            filePickerLauncher.launch(arrayOf("text/*", "application/*"))
        }
    ) {
        Text(
            text = stringResource(R.string.load_from_file)
        )
    }
}

@Preview
@Composable
private fun LoadKeysFromDeviceScreenPreview() {
    LoadKeysFromDeviceScreen(
        onBackClick = {},
        cloneState = InitState.Idle,
        vm = SetupViewModelMock(),
        storageConfig = StorageConfiguration.App,
        onSuccess = {},
        onClone = {},
        url = "url",
    )
}