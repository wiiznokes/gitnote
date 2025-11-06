package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState

private const val TAG = "AuthorizeGitNoteScreen"

@Composable
fun AuthorizeGitNoteScreen(
    onBackClick: () -> Unit,
    authState: InitState,
    appAuthToken: String,
    onSuccess: () -> Unit,
    getLaunchOAuthScreenIntent: () -> Intent,
    fetchInfos: (String) -> Unit,
    vmHashCode: Int,
) {

    AppPage(
        title = stringResource(R.string.authorize_gitnote),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        onBackClick = onBackClick,
        onBackClickEnabled = !authState.isLoading() && authState != InitState.FetchingInfosSuccess
    ) {

        LaunchedEffect(authState) {
            Log.d(TAG, "LaunchedEffect: $authState, hash=${vmHashCode}")
            if (authState is InitState.FetchingInfosSuccess) {
                onSuccess()
            }
        }

        val ctx = LocalContext.current

        Button(
            onClick = {
                val intent = getLaunchOAuthScreenIntent()
                ctx.startActivity(intent)
            },
            enabled = !authState.isLoading() && authState != InitState.FetchingInfosSuccess
        ) {
            if (!authState.isLoading()) {
                Text(text = stringResource(R.string.authorize_gitnote))
            } else {
                Text(text = authState.message())
            }
        }

        if (appAuthToken.isNotEmpty()) {
            Button(
                onClick = {
                    fetchInfos(appAuthToken)
                },
                enabled = !authState.isLoading() && authState != InitState.FetchingInfosSuccess
            ) {
                if (!authState.isLoading()) {
                    Text(text = "Fetch repositories metadata with the previous logged account")
                } else {
                    Text(text = authState.message())
                }
            }
        }
    }
}

@Preview
@Composable
private fun AuthorizeGitNoteScreenPreview() {

    AuthorizeGitNoteScreen(
        onBackClick = {},
        authState = InitState.Idle,
        onSuccess = {},
        getLaunchOAuthScreenIntent = {
            Intent()
        },
        vmHashCode = 0,
        appAuthToken = "hello",
        fetchInfos = {}
    )
}