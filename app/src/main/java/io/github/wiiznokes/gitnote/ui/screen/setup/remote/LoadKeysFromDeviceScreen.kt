package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.TextFieldValue
import io.github.wiiznokes.gitnote.ui.component.AppPage


private fun isKeyCorrect(key: String): Boolean {
    return true
}

@Composable
fun LoadKeysFromDeviceScreen(
    onBackClick: () -> Unit,
    onNext: () -> Unit,

    ) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        val publicKey = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

        val privateKey = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

        val privateKeyPassword = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

        OutlinedTextField(
            value = publicKey.value,
            onValueChange = {
                publicKey.value = it
            },
            label = {
                Text(text = "Public key")
            },
            singleLine = true,
            isError = !isKeyCorrect(publicKey.value.text),
        )

        OutlinedTextField(
            value = privateKey.value,
            onValueChange = {
                privateKey.value = it
            },
            label = {
                Text(text = "Private key")
            },
            singleLine = true,
            isError = !isKeyCorrect(privateKey.value.text),
        )

        OutlinedTextField(
            value = privateKeyPassword.value,
            onValueChange = {
                privateKeyPassword.value = it
            },
            label = {
                Text(text = "Private key password")
            },
            singleLine = true,
        )

        Button(
            onClick = {
                onNext()
            },
            enabled = isKeyCorrect(publicKey.value.text) && isKeyCorrect(privateKey.value.text)
        ) {
            Text(text = "Next")
        }
    }
}