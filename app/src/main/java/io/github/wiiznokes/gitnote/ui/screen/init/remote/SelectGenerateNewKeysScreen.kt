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





@Composable
fun SelectGenerateNewKeysScreen(
    onBackClick: () -> Unit,
    onGenerate: () -> Unit,
//    onCustom: () -> Unit,
) {

    AppPage(
        title = "We need SSH keys to authenticate",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Button(
            onClick = {
                onGenerate()
            },
        ) {
            Text(text = "Generate new keys")
        }

//        Button(
//            onClick = {
//                onCustom()
//            },
//        ) {
//            Text(text = "Provide Custom SSH keys")
//        }
    }
}