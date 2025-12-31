package io.github.wiiznokes.gitnote.helper

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object FrontmatterParser {

    private val updatedFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    private val completedRegex = Regex("completed\\?\\s*:\\s*\\w+", RegexOption.IGNORE_CASE)

    fun parseCompleted(content: String): Boolean {
        val frontmatter = extractFrontmatter(content) ?: return false
        val completedLine = frontmatter.lines().find { completedRegex.containsMatchIn(it.trim()) } ?: return false
        return completedLine.trim().endsWith("yes")
    }

    fun parseCompletedOrNull(content: String): Boolean? {
        val frontmatter = extractFrontmatter(content) ?: return null
        val completedLine = frontmatter.lines().find { completedRegex.containsMatchIn(it.trim()) } ?: return null
        return completedLine.trim().endsWith("yes")
    }

    fun toggleCompleted(content: String): String {
        val lines = content.lines()
        if (lines.isEmpty() || !lines[0].trim().startsWith("---")) return content

        val endIndex = lines.drop(1).indexOfFirst { it.trim().startsWith("---") }
        if (endIndex == -1) return content

        val frontmatterLines = lines.subList(1, endIndex + 1)
        val bodyLines = if (endIndex + 2 < lines.size) lines.subList(endIndex + 2, lines.size) else emptyList()

        val currentTime = updatedFormatter.format(Instant.now())

        var newFrontmatter = frontmatterLines.map { line ->
            when {
                completedRegex.containsMatchIn(line.trim()) -> {
                    val current = line.trim().endsWith("yes")
                    val newValue = if (current) "no" else "yes"
                    line.replace(completedRegex, "completed?: $newValue")
                }
                line.trim().startsWith("updated:") -> {
                    "updated: $currentTime"
                }
                else -> line
            }
        }

        // If no completed? field, add it
        val hasCompleted = newFrontmatter.any { completedRegex.containsMatchIn(it.trim()) }
        if (!hasCompleted) {
            // Find a good place to insert, e.g., after title or at the end
            val insertIndex = newFrontmatter.indexOfFirst { it.trim().startsWith("title:") }.takeIf { it >= 0 }?.plus(1) ?: newFrontmatter.size
            newFrontmatter = newFrontmatter.toMutableList().apply {
                add(insertIndex, "completed?: yes")
            }
        }

        return (listOf("---") + newFrontmatter + listOf("---") + bodyLines).joinToString("\n")
    }

    fun removeCompleted(content: String): String {
        val lines = content.lines()
        if (lines.isEmpty() || !lines[0].trim().startsWith("---")) return content

        val endIndex = lines.drop(1).indexOfFirst { it.trim().startsWith("---") }
        if (endIndex == -1) return content

        val frontmatterLines = lines.subList(1, endIndex + 1)
        val bodyLines = if (endIndex + 2 < lines.size) lines.subList(endIndex + 2, lines.size) else emptyList()

        val currentTime = updatedFormatter.format(Instant.now())

        val newFrontmatter = frontmatterLines.map { line ->
            when {
                completedRegex.containsMatchIn(line.trim()) -> null // remove the line
                line.trim().startsWith("updated:") -> {
                    "updated: $currentTime"
                }
                else -> line
            }
        }.filterNotNull()

        return (listOf("---") + newFrontmatter + listOf("---") + bodyLines).joinToString("\n")
    }

    fun addCompleted(content: String): String {
        val lines = content.lines()
        val currentTime = updatedFormatter.format(Instant.now())

        if (lines.isNotEmpty() && lines[0].trim().startsWith("---")) {
            // Has frontmatter, add completed? if missing
            val endIndex = lines.drop(1).indexOfFirst { it.trim().startsWith("---") }
            if (endIndex == -1) return content

            val frontmatterLines = lines.subList(1, endIndex + 1)
            val bodyLines = if (endIndex + 2 < lines.size) lines.subList(endIndex + 2, lines.size) else emptyList()

            var newFrontmatter = frontmatterLines.map { line ->
                when {
                    line.trim().startsWith("updated:") -> {
                        "updated: $currentTime"
                    }
                    else -> line
                }
            }

            // If no completed? field, add it
            val hasCompleted = newFrontmatter.any { completedRegex.containsMatchIn(it.trim()) }
            if (!hasCompleted) {
                val insertIndex = newFrontmatter.indexOfFirst { it.trim().startsWith("title:") }.takeIf { it >= 0 }?.plus(1) ?: newFrontmatter.size
                newFrontmatter = newFrontmatter.toMutableList().apply {
                    add(insertIndex, "completed?: no")
                }
            }

            return (listOf("---") + newFrontmatter + listOf("---") + bodyLines).joinToString("\n")
        } else {
            // No frontmatter, add it
            val title = "title: ${lines.firstOrNull()?.take(50) ?: "Untitled"}" // guess title from first line
            val newFrontmatter = listOf(
                title,
                "updated: $currentTime",
                "created: $currentTime",
                "completed?: no"
            )
            return (listOf("---") + newFrontmatter + listOf("---") + lines).joinToString("\n")
        }
    }

    private fun extractFrontmatter(content: String): String? {
        val lines = content.lines()
        if (lines.size < 3 || !lines[0].trim().startsWith("---")) return null
        val endIndex = lines.drop(1).indexOfFirst { it.trim().startsWith("---") }
        if (endIndex == -1) return null
        return lines.subList(1, endIndex + 1).joinToString("\n")
    }

    fun extractBody(content: String): String {
        val lines = content.lines()
        if (lines.size < 3 || !lines[0].trim().startsWith("---")) return content
        val endIndex = lines.drop(1).indexOfFirst { it.trim().startsWith("---") }
        if (endIndex == -1) return content
        return if (endIndex + 2 < lines.size) lines.subList(endIndex + 2, lines.size).joinToString("\n") else ""
    }
}