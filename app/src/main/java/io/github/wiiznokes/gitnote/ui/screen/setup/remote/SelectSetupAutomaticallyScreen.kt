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
fun SelectSetupAutomaticallyScreen(
    onBackClick: () -> Unit,
    onAutomatically: () -> Unit,
    onManually: () -> Unit,

    ) {
    AppPage(
        title = stringResource(R.string.setup_selection),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        SetupPage(
            stringResource(R.string.how_do_you_want_to_do_this),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = {
                    onAutomatically()
                }
            ) {
                Text(text = stringResource(R.string.setup_automatically))
            }

            Button(
                onClick = {
                    onManually()
                }
            ) {
                Text(text = stringResource(R.string.setup_manually))
            }
        }
    }
}


@Preview
@Composable
private fun SelectSetupAutomaticallyScreenPreview() {
    SelectSetupAutomaticallyScreen(
        onBackClick = {},
        onAutomatically = {},
        onManually = {}
    )
}