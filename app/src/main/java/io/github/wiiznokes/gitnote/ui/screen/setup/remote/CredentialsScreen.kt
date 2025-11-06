package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.provider.GithubProvider
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelI
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelMock


@Composable
fun CredentialsScreen(
    onBackClick: () -> Unit,
    vm: SetupViewModelI,
    storageConfig: StorageConfiguration,
    provider: Provider?,
    url: String,
    onClone: () -> Unit,
    onSuccess: () -> Unit,
) {

    AppPage(
        title = "Credentials",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {


        SetupPage {

            if (provider is GithubProvider) {

                SetupLine(
                    text = "Create a new access token to allow this app to access your repository. Be sure to grant the “Contents” permission with read and write access."
                ) {
                    val uriHandler = LocalUriHandler.current

                    SetupButton(
                        text = "1. " + "Create a token",
                        onClick = { uriHandler.openUri("https://github.com/settings/personal-access-tokens/new") },
                        link = true
                    )
                }
            }

            val username = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }
            val password = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }


            val usernameFocusRequester = remember { FocusRequester() }
            val passwordFocusRequester = remember { FocusRequester() }

            LaunchedEffect(null) {
                usernameFocusRequester.requestFocus()
            }

            SetupLine(
                text = "Enter username"
            ) {

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(usernameFocusRequester),
                    value = username.value,
                    onValueChange = {
                        username.value = it
                    },
                    label = {
                        Text(text = "Username")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Unspecified
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            passwordFocusRequester.requestFocus()
                        }
                    )
                )
            }

            SetupLine(
                text = "Enter password"
            ) {

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(passwordFocusRequester),
                    value = password.value,
                    onValueChange = {
                        password.value = it
                    },
                    label = {
                        Text(text = "Password")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    )
                )


            }

            SetupLine(
                text = stringResource(R.string.try_cloning)
            ) {

                SetupButton(
                    text = stringResource(R.string.clone_repo),
                    onClick = {
                        vm.cloneRepo(
                            storageConfig = storageConfig,
                            remoteUrl = url,
                            cred = Cred.UserPassPlainText(
                                username = username.value.text,
                                password = password.value.text,
                            ),
                            onSuccess = onSuccess
                        )

                        onClone()
                    },
                    enabled = username.value.text.isNotEmpty() && password.value.text.isNotEmpty()
                )
            }
        }

    }
}


@Preview
@Composable
private fun CredentialsScreenPreview() {
    CredentialsScreen(
        onBackClick = {},
        storageConfig = StorageConfiguration.App,
        url = "url",
        provider = GithubProvider(),
        vm = SetupViewModelMock(),
        onSuccess = {},
        onClone = {}
    )

}