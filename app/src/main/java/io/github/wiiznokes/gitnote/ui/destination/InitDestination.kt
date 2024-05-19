package io.github.wiiznokes.gitnote.ui.destination

import android.os.Parcelable
import io.github.wiiznokes.gitnote.data.NewRepoState
import kotlinx.parcelize.Parcelize


sealed interface InitDestination : Parcelable {

    @Parcelize
    data object Main : InitDestination

    @Parcelize
    data class FileExplorer(
        val title: String,
        val path: String?,
        val newRepoSource: NewRepoSource,
    ) : InitDestination

    @Parcelize
    data class Remote(val repoState: NewRepoState) : InitDestination

}

enum class NewRepoSource {
    Create,
    Open,
    Clone;


    fun getExplorerTitle(): String {
        return when (this) {
            Create -> "Create repository in this folder"
            Open -> "Open this repository"
            Clone -> "Clone repository in this folder"
        }
    }
}
