package io.github.wiiznokes.gitnote.ui.screen.init.remote

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.viewmodel.AuthState
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel

private const val TAG = "AuthorizeGitNoteScreen"

@Composable
fun AuthorizeGitNoteScreen(
    onBackClick: () -> Unit,
    vm: InitViewModel,
    onSuccess: () -> Unit,
    ) {
    AppPage(
        title = "Authorize Gitnote",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
    ) {
        val ctx = LocalContext.current

        val authState = vm.authState.collectAsState().value
        LaunchedEffect(authState) {
            Log.d(TAG, "LaunchedEffect: $authState, hash=${vm.hashCode()}")
            if (authState is AuthState.Success) {
                onSuccess()
            }
        }

        Button(
            onClick = {
                val intent = vm.getLaunchOAuthScreenIntent()
                ctx.startActivity(intent)
            }
        ) {
            Text(text = "Authorize Gitnote")
        }

    }
}