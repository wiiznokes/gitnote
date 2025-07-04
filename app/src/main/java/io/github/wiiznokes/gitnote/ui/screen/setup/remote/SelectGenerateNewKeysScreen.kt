package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.ui.component.AppPage





@Composable
fun SelectGenerateNewKeysScreen(
    onBackClick: () -> Unit,
    onGenerate: () -> Unit,
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
    }
}