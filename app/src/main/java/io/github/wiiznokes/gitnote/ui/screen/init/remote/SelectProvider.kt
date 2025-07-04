package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel


@Composable
fun SelectProviderScreen(
    onBackClick: () -> Unit,
    vm: InitViewModel,
    onProviderSelected: () -> Unit,
) {

    AppPage(
        title = "Select a Git Hosting Provider",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        ProviderType.entries.forEach {
            Button(
                onClick = {
                    vm.setProvider(it)
                    onProviderSelected()
                }
            ) {
                Text(text = it.name)
            }
        }

        Button(
            onClick = {
                vm.setProvider(null)
                onProviderSelected()
            }
        ) {
            Text(text = "Custom")
        }
    }
}