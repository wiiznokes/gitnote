package io.github.wiiznokes.gitnote.provider

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class GithubProvider : Provider {
    private val clientId = "Ov23li8EPatIAsWPt9QT"

    // storing this secret in the repo is "ok"
    // the only risk is github app reputation and quotas
    // it would require a server to not store it here
    private val clientSecret = "12f3f4742855deaafb45e798bcc635608b9d6fe6"

    override fun getLaunchOAuthScreenUrl(): String {
        return "https://github.com/login/oauth/authorize?client_id=$clientId&scope=repo"
    }

    override fun exchangeCodeForAccessToken(code: String): String {
        val url = URL("https://github.com/login/oauth/access_token")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val body = "client_id=$clientId&client_secret=$clientSecret&code=$code"
        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val accessToken = json.getString("access_token")

        return accessToken
    }

    override fun fetchUserRepos(token: String): List<RepoInfo> {
        val url = URL("https://api.github.com/user/repos?page=1&per_page=100")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val response = connection.inputStream.bufferedReader().use { it.readText() }


        val repos = mutableListOf<RepoInfo>()


        val jsonArray = JSONArray(response)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        for (i in 0 until jsonArray.length()) {
            val repo = jsonArray.getJSONObject(i)
            val name = repo.getString("name")
            val owner = repo.getJSONObject("owner").getString("login")
            val url = repo.getString("ssh_url")
            val updatedAt = repo.getString("updated_at")
            val timeMillis = dateFormat.parse(updatedAt)?.time ?: 0L

            repos.add(
                RepoInfo(
                    owner = owner,
                    name = name,
                    url = url,
                    lastModifiedTimeMillis = timeMillis
                )
            )
        }


        repos.sortWith(compareByDescending { it.lastModifiedTimeMillis })

        return repos
    }

    override fun createNewRepo(token: String, repoName: String) {

        val url = URL("https://api.github.com/user/repos")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject().apply {
            put("name", repoName)
            put("description", "")
            put("private", true)
        }

        connection.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            Exception("Failed to create repo: HTTP $responseCode $error")
        }
    }

    override fun getUserInfo(token: String): UserInfo {
        val url = URL("https://api.github.com/user")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("Failed to fetch user info: HTTP $responseCode $error")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        return UserInfo(
            username = json.getString("login"),
            name = json.optString("name", ""),
            email = json.optString("email", ""),
        )
    }

    override fun addDeployKeyToRepo(
        token: String,
        publicKey: String,
        fullRepoName: String
    ) {
        val url = URL("https://api.github.com/repos/$fullRepoName/keys")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "token $token")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject().apply {
            put("title", "GitNote")
            put("key", publicKey)
            put("read_only", false)
        }

        connection.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("Failed to add deploy key: HTTP $responseCode $error")
        }
    }

    override fun sshCloneUrlFromRepoName(repoName: String, username: String): String {
        return "$username@github.com:$repoName.git"
    }

}