package io.github.wiiznokes.gitnote

import io.github.wiiznokes.gitnote.provider.GithubProvider
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.test.Ignore
import kotlin.test.Test

class GithubProviderTest {

    @Test
    @Ignore("Need PAT")
    fun testing() {

        val provider = GithubProvider()

        val token = ""
        val repos = provider.fetchUserRepos(token, "wiiznokes")

        println("size=${repos.size}")

        println("repos=$repos")

        Path("repos.txt").writeText(repos.toString())
    }

}