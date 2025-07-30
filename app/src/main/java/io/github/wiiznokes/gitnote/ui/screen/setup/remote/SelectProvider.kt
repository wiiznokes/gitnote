package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage


@Composable
fun SelectProviderScreen(
    onBackClick: () -> Unit,
    setProvider: (ProviderType?) -> Unit,
    onProviderSelected: () -> Unit,
) {

    AppPage(
        title = stringResource(R.string.git_hosting_provider),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {
        SetupPage(
            stringResource(R.string.select_a_git_hosting_provider),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            ProviderType.entries.forEach {
                Button(
                    onClick = {
                        setProvider(it)
                        onProviderSelected()
                    }
                ) {
                    Text(text = it.name)
                }
            }

            Button(
                onClick = {
                    setProvider(null)
                    onProviderSelected()
                }
            ) {
                Text(text = stringResource(R.string.custom))
            }
        }
    }
}

@Preview
@Composable
private fun SelectProviderScreenPreview() {
    SelectProviderScreen(
        onBackClick = {},
        setProvider = {},
        onProviderSelected = {}
    )
}