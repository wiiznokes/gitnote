package io.github.wiiznokes.gitnote.ui.model

import android.os.Parcelable
import io.github.wiiznokes.gitnote.data.AppPreferences
import kotlinx.parcelize.Parcelize


interface ProviderLink {
    val mainPage: String?
    val createRepo: String?
    val createToken: String?
    val listRepo: String?
}

enum class Provider : ProviderLink {
    GitHub {
        override val mainPage: String = "https://github.com/"
        override val createRepo: String = "https://github.com/new"
        override val createToken: String = "https://github.com/settings/tokens"
        override val listRepo: String? = null
    },
//    GitLab {
//        override val mainPage: String = "https://gitlab.com/"
//        override val createRepo: String = "https://gitlab.com/projects/new#blank_project"
//        override val createToken: String =
//            "https://gitlab.com/-/user_settings/personal_access_tokens"
//        override val listRepo: String = "https://gitlab.com/dashboard/projects"
//    },
}

@Parcelize
sealed class Cred: Parcelable {
    data class UserPassPlainText(
        val username: String,
        val password: String
    ) : Cred()

    data class Ssh(
        val username: String,
        val publicKey: String,
        val privateKey: String,
    ) : Cred()


    override fun toString(): String {
        return when  (this) {
            is Ssh -> "UserPassPlainText(username=$username, publicKey=$publicKey, privateKeyLen=${privateKey.length})"
            is UserPassPlainText -> "UserPassPlainText(username=$username, password=${"*".repeat(password.length)})"
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
    class Device(val path: String) : StorageConfiguration()

    fun repoPath(): String {
        return when (this) {
            App -> AppPreferences.appStorageRepoPath
            is Device -> this.path
        }
    }
}
