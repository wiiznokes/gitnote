package io.github.wiiznokes.gitnote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wiiznokes.gitnote.manager.GitManager
import io.github.wiiznokes.gitnote.ui.model.GitCreed
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TestGitManager {

    private lateinit var gitManager: GitManager
    private lateinit var context: Context

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()


        gitManager = GitManager()
    }

    @After
    fun close() {
        gitManager.closeRepo()
        gitManager.shutdown()
    }


    @Test
    fun clone() {

        val repoPath = context.filesDir.path + "/repo"
        val creed = GitCreed(
            userName = "",
            password = ""
        )

        runBlocking {
            gitManager.cloneRepo(
                repoPath = repoPath,
                repoUrl = "https://github.com/wiiznokes/repo_test.git",
                cred = creed,
                progressCallback = {}
            ).getOrThrow()

            //assert(gitManager.lastCommit().unwrap() == "ec6e09f227d5ded1724fff7e195b42541bb69354")

            File("$repoPath/file1.txt").createNewFile()

            gitManager.commitAll(creed.userName).getOrThrow()

            gitManager.push(creed).getOrThrow()
        }


    }
}