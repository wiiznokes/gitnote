package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.screen.setup.NewRepoMethodScreen
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState.AuthState
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModel

private const val TAG = "AuthorizeGitNoteScreen"

@Composable
fun AuthorizeGitNoteScreen(
    onBackClick: () -> Unit,
    authState: InitState,
    onSuccess: () -> Unit,
    getLaunchOAuthScreenIntent: () -> Intent,
    vmHashCode: Int,
) {

    AppPage(
        title = "Authorize Gitnote",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = authState.isClickable()
    ) {

        LaunchedEffect(authState) {
            Log.d(TAG, "LaunchedEffect: $authState, hash=${vmHashCode}")
            if (authState is AuthState.Success) {
                onSuccess()
            }
        }

        val ctx = LocalContext.current

        Button(
            onClick = {
                val intent = getLaunchOAuthScreenIntent()
                ctx.startActivity(intent)
            },
            enabled = authState.isClickable() && authState != AuthState.Success
        ) {
             if (!authState.isLoading()) {
                Text(text = "Authorize Gitnote")
            } else {
                Text(text = authState.message())
            }
        }
    }
}

@Preview
@Composable
private fun AuthorizeGitNoteScreenPreview() {

    AuthorizeGitNoteScreen(
        onBackClick = {},
        authState = AuthState.Idle,
        onSuccess = {},
        getLaunchOAuthScreenIntent = {
            Intent()
        },
        vmHashCode = 0
    )
}