package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage


@Composable
fun SelectGenerateNewSshKeysScreen(
    onBackClick: () -> Unit,
    onGenerate: () -> Unit,
    onCustom: () -> Unit,
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

            SetupLine(text = "") {
                SetupButton(
                    onClick = onGenerate,
                    text = stringResource(R.string.generate_new_keys)
                )

                SetupButton(
                    onClick = onCustom,
                    text = stringResource(R.string.custom_ssh_keys)
                )
            }
        }
    }
}


@Preview
@Composable
private fun SelectGenerateNewSshKeysScreenPreview() {
    SelectGenerateNewSshKeysScreen(
        onBackClick = {},
        onGenerate = {},
        onCustom = {}
    )
}