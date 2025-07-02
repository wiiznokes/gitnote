package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.ui.component.AppPage

@Composable
fun PickRepoScreen(
    onBackClick: () -> Unit
) {
    AppPage(
        title = "Select or Create a Repository",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {


    }
}