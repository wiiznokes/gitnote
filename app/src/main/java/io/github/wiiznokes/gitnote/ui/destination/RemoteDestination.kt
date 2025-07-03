package io.github.wiiznokes.gitnote.ui.destination

import android.os.Parcelable
import io.github.wiiznokes.gitnote.ui.model.Provider
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.parcelize.Parcelize

sealed interface RemoteDestination : Parcelable {

    @Parcelize
    data object SelectProvider : RemoteDestination


    @Parcelize
    data class SelectSetupAutomatically( val provider: Provider) :
        RemoteDestination


    @Parcelize
    data class AuthorizeGitNote( val provider: Provider) :
        RemoteDestination


    @Parcelize
    data class EnterUrl( val provider: Provider?) :
        RemoteDestination


    @Parcelize
    data class PickRepo( val provider: Provider) : RemoteDestination

    @Parcelize
    data class SelectGenerateNewKeys(
        val provider: Provider?,
        val url: String
    ) : RemoteDestination


    @Parcelize
    data class GenerateNewKeys( val provider: Provider?, val url: String,
    ) : RemoteDestination

    @Parcelize
    data class LoadKeysFromDevice(
        val provider: Provider?,
        val url: String
    ) : RemoteDestination

}
