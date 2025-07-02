package io.github.wiiznokes.gitnote.ui.destination

import android.os.Parcelable
import io.github.wiiznokes.gitnote.data.NewRepoState
import io.github.wiiznokes.gitnote.ui.model.Provider
import kotlinx.parcelize.Parcelize

sealed interface RemoteDestination : Parcelable {

    @Parcelize
    data class SelectProvider(val repoState: NewRepoState) : RemoteDestination


    @Parcelize
    data class SelectSetupAutomatically(val repoState: NewRepoState, val provider: Provider) :
        RemoteDestination


    @Parcelize
    data class AuthorizeGitNote(val repoState: NewRepoState, val provider: Provider) :
        RemoteDestination


    @Parcelize
    data class EnterUrl(val repoState: NewRepoState, val provider: Provider?) :
        RemoteDestination


    @Parcelize
    data class PickRepo(val repoState: NewRepoState, val provider: Provider) : RemoteDestination

    @Parcelize
    data class SelectGenerateNewKeys(
        val repoState: NewRepoState,
        val provider: Provider?,
        val url: String
    ) : RemoteDestination


    @Parcelize
    data class GenerateNewKeys(
        val repoState: NewRepoState, val provider: Provider?, val url: String,
    ) : RemoteDestination

    @Parcelize
    data class LoadKeysFromDevice(
        val repoState: NewRepoState,
        val provider: Provider?,
        val url: String
    ) : RemoteDestination

}
