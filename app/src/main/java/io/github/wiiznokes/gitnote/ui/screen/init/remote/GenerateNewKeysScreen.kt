package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.model.Provider

@Composable
fun GenerateNewKeysWithProviderScreen(
    onBackClick: () -> Unit,
    provider: Provider,
) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Text("key")
        Button(onClick = {}) { Text("Copy key") }
        Button(onClick = {}) { Text("Regenerate key") }


        Button(onClick = {}) { Text("Open deploy key webpage") }

        Button(onClick = {}) { Text("Clone repo") }
    }
}

@Composable
fun GenerateNewKeysScreen(
    onBackClick: () -> Unit,

    ) {
    AppPage(
        title = "",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Text("key")
        Button(onClick = {}) { Text("Copy key") }
        Button(onClick = {}) { Text("Regenerate key") }


        Button(onClick = {}) { Text("Clone repo") }
    }
}