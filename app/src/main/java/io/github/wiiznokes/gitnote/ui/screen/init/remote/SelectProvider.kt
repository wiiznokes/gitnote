package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.model.Provider


@Composable
fun SelectProviderScreen(
    onBackClick: () -> Unit,
    onProviderSelected: (Provider?) -> Unit,
) {

    AppPage(
        title = "Select a Git Hosting Provider",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Provider.entries.forEach {
            Button(
                onClick = {
                    onProviderSelected(it)
                }
            ) {
                Text(text = it.name)
            }
        }

        Button(
            onClick = {
                onProviderSelected(null)
            }
        ) {
            Text(text = "Custom")
        }
    }
}