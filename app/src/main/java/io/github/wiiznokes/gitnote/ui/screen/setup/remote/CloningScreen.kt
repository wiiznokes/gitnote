package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState

private const val TAG = "CloningScreen"


@Composable
fun CloningScreen(
    cloneState: InitState,
    onCancel: () -> Unit,
    onShowLogs: () -> Unit,
) {
    AppPage(
        title = "Clone",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClickEnabled = !cloneState.isLoading()
    ) {
        Text(text = cloneState.message())

        Spacer(Modifier.height(20.dp))

        if (cloneState is InitState.Error) {
            Button(
                onClick = onShowLogs
            ) {
                Text(stringResource(R.string.show_logs))
            }
        }

        Button(
            onClick = onCancel,
            enabled = cloneState !is InitState.CalculatingTimestamps && cloneState !is InitState.GeneratingDatabase
        ) {
            Text(
                if (cloneState is InitState.Error) {
                    stringResource(R.string.go_back)

                } else stringResource(R.string.cancel)
            )
        }
    }
}

@Preview
@Composable
private fun PickRepoScreenPreview() {

    CloningScreen(
        cloneState = InitState.Error("Can't clone repo"),
        onCancel = {},
        onShowLogs = {}
    )
}