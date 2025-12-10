package io.github.wiiznokes.gitnote.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface RemoteDestination : Parcelable {

    @Parcelize
    data object SelectProvider : RemoteDestination


    @Parcelize
    data object SelectSetupAutomatically : RemoteDestination


    @Parcelize
    data object AuthorizeGitNote : RemoteDestination


    @Parcelize
    data object EnterUrl : RemoteDestination


    @Parcelize
    data object PickRepo : RemoteDestination

    @Parcelize
    data class SelectGenerateNewSshKeys(
        val url: String
    ) : RemoteDestination


    @Parcelize
    data class GenerateNewKeys(
        val url: String,
    ) : RemoteDestination

    @Parcelize
    data class Credentials(
        val url: String,
    ) : RemoteDestination

    @Parcelize
    data object Cloning : RemoteDestination

    @Parcelize
    data class LoadKeysFromDevice(
        val url: String
    ) : RemoteDestination

    @Parcelize
    data object Logs: RemoteDestination
}
