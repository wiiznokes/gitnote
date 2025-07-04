package io.github.wiiznokes.gitnote.ui.destination

import android.os.Parcelable
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.parcelize.Parcelize


sealed interface SetupDestination : Parcelable {

    @Parcelize
    data object Main : SetupDestination

    @Parcelize
    data class FileExplorer(
        val title: String,
        val path: String?,
        val newRepoMethod: NewRepoMethod,
    ) : SetupDestination

    @Parcelize
    data class Remote(val storageConfig: StorageConfiguration) : SetupDestination

}

enum class NewRepoMethod {
    Create,
    Open,
    Clone;


    fun getExplorerTitle(): String {
        val context = MyApp.appModule.context

        return when (this) {
            Create -> context.getString(R.string.create_repo_explorer)
            Open -> context.getString(R.string.open_repo_explorer)
            Clone -> context.getString(R.string.clone_repo_explorer)
        }
    }
}


