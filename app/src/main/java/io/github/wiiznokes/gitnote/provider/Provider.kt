package io.github.wiiznokes.gitnote.provider


enum class ProviderType {
    GitHub,
}

data class RepoInfo(
    val name: String,
    val owner: String,
    val url: String,
    val lastModifiedTimeMillis: Long,
) {
    val fullRepoName = "$owner/$name"
}

data class UserInfo(
    val username: String,
    val name: String,
    val email: String,
)

interface Provider {

    fun getLaunchOAuthScreenUrl(): String

    fun exchangeCodeForAccessToken(code: String): String

    fun fetchUserRepos(token: String): List<RepoInfo>

    fun createNewRepo(token: String, repoName: String)

    fun getUserInfo(token: String): UserInfo

    fun addDeployKeyToRepo(token: String, publicKey: String, fullRepoName: String)

    fun sshCloneUrlFromRepoName(repoName: String, username: String = "git"): String
}