package io.github.wiiznokes.gitnote.ui.model

import android.os.Parcelable
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class Cred : Parcelable {
    data class UserPassPlainText(
        val username: String,
        val password: String
    ) : Cred() {
        override fun toString(): String {
            return "UserPassPlainText(username=$username, password=${"*".repeat(password.length)})"
        }
    }

    data class Ssh(
        val username: String = "git",
        val publicKey: String,
        val privateKey: String,
    ) : Cred() {
        override fun toString(): String {
            return "Ssh(username=$username, publicKey=$publicKey, privateKeyLen=${privateKey.length})"
        }
    }
}

enum class CredType {
    None,
    UserPassPlainText,
    Ssh,
}


@Parcelize
sealed class StorageConfiguration : Parcelable {
    data object App : StorageConfiguration()
    class Device(var path: String, val useUrlForRootFolder: Boolean = false) : StorageConfiguration()

    fun repoPath(): String {
        return when (this) {
            App -> AppPreferences.appStorageRepoPath
            is Device -> this.path
        }
    }

    fun applyUrlName(url: String) {
        if (this is Device && useUrlForRootFolder) {
            val name = url
                .substringAfterLast('/')
                .substringBeforeLast(".git")

            path = "$path/$name"
        }
    }

    fun prepareStorageRepoPath() {
        val folder = NodeFs.Folder.fromPath(repoPath())
        folder.delete()
        folder.create()
    }
}
