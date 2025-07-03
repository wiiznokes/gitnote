package io.github.wiiznokes.gitnote.ui.screen.init.remote

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import io.github.wiiznokes.gitnote.ui.component.AppPage
import androidx.core.net.toUri


private const val clientId = "Ov23li8EPatIAsWPt9QT"

@Composable
fun AuthorizeGitNoteScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    ) {
    AppPage(
        title = "Authorize Gitnote",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {

        val authUrl = "https://github.com/login/oauth/select_account?client_id=$clientId&scope=repo"

        val ctx = LocalContext.current

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                ctx.startActivity(intent)
            }
        ) {
            Text(text = "Authorize Gitnote")
        }


    }
}