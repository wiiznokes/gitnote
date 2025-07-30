package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.ui.component.AppPage

@Composable
fun SelectSetupAutomaticallyScreen(
    onBackClick: () -> Unit,
    onAutomatically: () -> Unit,
    onManually: () -> Unit,

    ) {
    AppPage(
        title = "How do you want to do this?",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Button(
            onClick = {
                onAutomatically()
            }
        ) {
            Text(text = "Setup Automatically")
        }

        Button(
            onClick = {
                onManually()
            }
        ) {
            Text(text = "Let me do it manually")
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