package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.model.Provider

private fun isUrlCorrect(url: String): Boolean {
    return url.contains(" ") || url.isEmpty()
}

@Composable
fun EnterUrlWithProviderScreen(
    onBackClick: () -> Unit,
    provider: Provider,
    onUrl: (String) -> Unit
) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {


        Button(
            onClick = {

            }
        ) {
            Text("open create repo page")
        }

        val url = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

        OutlinedTextField(
            value = url.value,
            onValueChange = {
                url.value = it
            },
            label = {
                Text(text = stringResource(R.string.clone_step_url_label))
            },
            singleLine = true,
            isError = !isUrlCorrect(url.value.text),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri
            )
        )


        Button(
            onClick = {
                onUrl(url.value.text)
            },
            enabled = isUrlCorrect(url.value.text)
        ) {
            Text(text = "Next")
        }

    }
}

@Composable
fun EnterUrlScreen(
    onBackClick: () -> Unit,
    onUrl: (String) -> Unit
) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        val url = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

        OutlinedTextField(
            value = url.value,
            onValueChange = {
                url.value = it
            },
            label = {
                Text(text = stringResource(R.string.clone_step_url_label))
            },
            singleLine = true,
            isError = !isUrlCorrect(url.value.text),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri
            )
        )


        Button(
            onClick = {
                onUrl(url.value.text)
            },
            enabled = isUrlCorrect(url.value.text)
        ) {
            Text(text = "Next")
        }

    }
}