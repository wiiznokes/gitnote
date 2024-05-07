package io.github.wiiznokes.gitnote.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


interface ProviderLink {
    val mainPage: String?
    val createRepo: String?
    val createToken: String?
    val checkOutRepo: String?
}

enum class Provider : ProviderLink {
    GitHub {
        override val mainPage: String = "https://github.com/"
        override val createRepo: String = "https://github.com/new"
        override val createToken: String = "https://github.com/settings/tokens"
        override val checkOutRepo: String? = null
    },
    Other {
        override val mainPage: String? = null
        override val createRepo: String? = null
        override val createToken: String? = null
        override val checkOutRepo: String? = null
    },
}

@Parcelize
data class GitCreed(
    val userName: String,
    val password: String,
) : Parcelable {

    override fun toString(): String =
        "GitCreed(userName=${userName}, password=${"*".repeat(password.length)})"

}

