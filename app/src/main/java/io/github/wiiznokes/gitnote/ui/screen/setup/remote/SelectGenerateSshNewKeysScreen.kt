package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupPage


@Composable
fun SelectGenerateSshNewKeysScreen(
    onBackClick: () -> Unit,
    onGenerate: () -> Unit,
) {

    AppPage(
        title = stringResource(R.string.ssh_keys_title),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        SetupPage(
            title = stringResource(R.string.we_need_ssh_keys_to_authenticate),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    onGenerate()
                }
            ) {
                Text(text = stringResource(R.string.generate_new_keys))
            }
        }
    }
}


@Preview
@Composable
private fun SelectGenerateSshNewKeysScreenPreview() {
    SelectGenerateSshNewKeysScreen(
        onBackClick = {},
        onGenerate = {}
    )
}