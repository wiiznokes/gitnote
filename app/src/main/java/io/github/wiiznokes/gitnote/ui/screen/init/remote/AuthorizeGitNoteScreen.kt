package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.github.wiiznokes.gitnote.ui.component.AppPage



@Composable
fun AuthorizeGitNoteScreen(
    onBackClick: () -> Unit,
    onSucess: () -> Unit,
    ) {
    AppPage(
        title = "Authorize Gitnote",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        Button(
            onClick = {
                onSucess()
            }
        ) {
            Text(text = "Authorize Gitnote")
        }


    }
}