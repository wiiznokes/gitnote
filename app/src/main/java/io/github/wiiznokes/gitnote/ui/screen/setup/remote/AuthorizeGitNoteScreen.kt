package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.SetupButton
import io.github.wiiznokes.gitnote.ui.component.SetupLine
import io.github.wiiznokes.gitnote.ui.component.SetupPage
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
        title = stringResource(R.string.authorize_gitnote_title),
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

        var authButtonClicked = remember {
            false
        }

        SetupPage {

            SetupLine(
                text = ""
            ) {
                val ctx = LocalContext.current

                SetupButton(
                    onClick = {
                        authButtonClicked = true
                        val intent = getLaunchOAuthScreenIntent()
                        ctx.startActivity(intent)
                    },
                    enabled = !authState.isLoading() && authState != InitState.FetchingInfosSuccess,
                    text = if (authButtonClicked && authState.isLoading()) {
                        authState.message()
                    } else {
                        stringResource(R.string.authorize_gitnote)
                    }
                )
            }

            if (appAuthToken.isNotEmpty()) {
                SetupLine(
                    text = ""
                ) {
                    SetupButton(
                        onClick = {
                            authButtonClicked = false
                            fetchInfos(appAuthToken)
                        },
                        enabled = !authState.isLoading() && authState != InitState.FetchingInfosSuccess,
                        text = if (!authButtonClicked && authState.isLoading()) {
                            authState.message()
                        } else {
                            "Fetch repositories metadata with the previous logged account"
                        }
                    )
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