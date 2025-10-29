package io.github.wiiznokes.gitnote.data.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Fts4
import io.github.wiiznokes.gitnote.BuildConfig
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.data.removeFirstAndLastSlash
import io.github.wiiznokes.gitnote.data.requireNotEndOrStartWithSlash
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.Instant


private const val TAG = "DatabaseSchema"


@Entity(
    tableName = "NoteFolders",
    primaryKeys = ["relativePath"],
)
data class NoteFolder(
    val relativePath: String,
    val id: Int,
) {

    companion object {
        fun new(
            relativePath: String,
            id: Int = RepoDatabase.generateUid()
        ): NoteFolder {
            return NoteFolder(
                relativePath = removeFirstAndLastSlash(relativePath),
                id = id
            )
        }
    }

    init {
        if (BuildConfig.DEBUG) {
            requireNotEndOrStartWithSlash(relativePath)
            requireNotEndOrStartWithSlash(fullName())
        }
    }

    fun fullName(): String {
        return relativePath.substringAfterLast("/")
    }

    fun parentPath(): String? {
        if (relativePath == "") return null
        return relativePath.substringBeforeLast("/", missingDelimiterValue = "")
    }

    fun toFolderFs(rootPath: String): NodeFs.Folder {
        return NodeFs.Folder.fromPath(rootPath, relativePath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return id == (other as NoteFolder).id
    }

    override fun hashCode(): Int = id
}

@Entity(
    tableName = "Notes",
    primaryKeys = ["relativePath"],
)
@Parcelize
data class Note(
    val relativePath: String,
    val content: String,
    val lastModifiedTimeMillis: Long,
    val id: Int
) : Parcelable, Serializable {

    companion object {
        fun new(
            relativePath: String,
            content: String = "",
            lastModifiedTimeMillis: Long = Instant.now().toEpochMilli(),
            id: Int = RepoDatabase.generateUid()
        ): Note {
            return Note(
                relativePath = removeFirstAndLastSlash(relativePath),
                content = content,
                lastModifiedTimeMillis = lastModifiedTimeMillis,
                id = id,
            )
        }
    }

    fun fullName(): String {
        return relativePath.substringAfterLast("/")
    }

    fun parentPath(): String {
        return relativePath.substringBeforeLast("/", missingDelimiterValue = "")
    }

    fun fileExtension(): FileExtension {
        return relativePath.substringAfterLast(".", missingDelimiterValue = "")
            .let { FileExtension.match(it) }
    }

    fun nameWithoutExtension(): String {
        val fullName = fullName()
        return fullName.substring(
            startIndex = 0,
            endIndex = fullName.length - (fileExtension().text.length + 1)
        )
    }

    init {
        if (BuildConfig.DEBUG) {
            require(relativePath.isNotEmpty())
            requireNotEndOrStartWithSlash(relativePath)
            requireNotEndOrStartWithSlash(parentPath())
            requireNotEndOrStartWithSlash(fullName())
            requireNotEndOrStartWithSlash(nameWithoutExtension())
        }
    }

    fun toFileFs(rootPath: String): NodeFs.File {
        return NodeFs.File.fromPath(rootPath, relativePath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return id == (other as Note).id
    }

    override fun hashCode(): Int = id
}

@Fts4(contentEntity = Note::class)
@Entity(tableName = "NotesFts")
data class NoteFts(
    val relativePath: String,
    val content: String
)