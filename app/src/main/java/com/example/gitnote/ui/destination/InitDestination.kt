package com.example.gitnote.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


sealed interface InitDestination : Parcelable {


    @Parcelize
    data object LocalStoragePermission : InitDestination

    @Parcelize
    data object Main : InitDestination

    @Parcelize
    data class FileExplorer(
        val title: String,
        val path: String?,
        val newRepoSource: NewRepoSource,
    ) : InitDestination

    @Parcelize
    data class Remote(val repoPath: String) : InitDestination

}

enum class NewRepoSource {
    Create,
    Open,
    Clone,
}