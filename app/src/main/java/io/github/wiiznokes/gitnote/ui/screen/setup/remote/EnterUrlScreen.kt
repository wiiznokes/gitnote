package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.provider.GithubProvider
import io.github.wiiznokes.gitnote.provider.Provider
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.NextButton
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage

private val sshGitRegex = Regex("""^(?:git@|ssh://git@)[\w.-]+:[\w./-]+(?:\.git)?$""")

private fun isUrlCorrect(url: String): Boolean {
    return sshGitRegex.matches(url)
}

@Composable
fun EnterUrlWithProviderScreen(
    onBackClick: () -> Unit,
    provider: Provider,
    onUrl: (String) -> Unit,
) {

    AppPage(
        title = "Url",
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        SetupPage {
            SetupLine(
                text = "1. Go to the website, create a repo and copy its git clone URL. Choose the SSH method"
            ) {
                val uriHandler = LocalUriHandler.current

                SetupButton(
                    text = "Create repo",
                    onClick = { uriHandler.openUri(provider.createRepoLink) },
                    link = true

                )
            }

            val url = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }

            SetupLine(
                text = "2. Enter the Git clone URL"
            ) {
                UrlTextField(url = url)
            }

            NextButton(
                text = "Next",
                onClick = {
                    onUrl(url.value.text)
                },
                enabled = isUrlCorrect(url.value.text)
            )
        }

    }
}

@Composable
fun EnterUrlScreen(
    onBackClick: () -> Unit,
    onUrl: (String) -> Unit
) {
    AppPage(
        title = "Url",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        SetupPage {
            val url = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }

            SetupLine(
                text = "1. Enter the Git clone URL"
            ) {
                UrlTextField(url = url)
            }

            NextButton(
                text = "Next",
                onClick = {
                    onUrl(url.value.text)
                },
                enabled = isUrlCorrect(url.value.text)
            )
        }
    }
}

@Composable
private fun UrlTextField(url: MutableState<TextFieldValue>) {

    OutlinedTextField(
        modifier = Modifier
            .fillMaxSize(),
        value = url.value,
        onValueChange = {
            url.value = it
        },
        label = {
            Text(text = stringResource(R.string.clone_step_url_label))
        },
        placeholder = {
            Text(text = "git@github.com:wiiznokes/gitnote.git")
        },
        singleLine = true,
        isError = !isUrlCorrect(url.value.text),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri
        )
    )
}

@Preview
@Composable
private fun EnterUrlWithProviderScreenPreview() {

    EnterUrlWithProviderScreen(
        onBackClick = {},
        provider = GithubProvider(),
        onUrl = {}
    )
}