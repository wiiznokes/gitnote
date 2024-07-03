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
    GitLab {
        override val mainPage: String = "https://gitlab.com/"
        override val createRepo: String = "https://gitlab.com/projects/new#blank_project"
        override val createToken: String = "https://gitlab.com/-/user_settings/personal_access_tokens"
        override val listRepo: String = "https://gitlab.com/dashboard/projects"
    },
}

@Parcelize
data class GitCreed(
    val userName: String,
    val password: String,
) : Parcelable {

    companion object {
        fun usernameOrDefault(creed: GitCreed?): String =
            creed?.userName ?: AppPreferences.DEFAULT_USERNAME
    }

    override fun toString(): String =
        "GitCreed(userName=${userName}, password=${"*".repeat(password.length)})"

}

