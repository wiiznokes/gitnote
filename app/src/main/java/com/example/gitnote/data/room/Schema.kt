package com.example.gitnote.data.room

import android.os.Parcelable
import androidx.room.Entity
import com.example.gitnote.BuildConfig
import com.example.gitnote.data.removeFirstAndLastSlash
import com.example.gitnote.data.requireNotEndOrStartWithSlash
import com.example.gitnote.ui.model.FileExtension
import kotlinx.parcelize.Parcelize
import java.time.Instant


private const val TAG = "DatabaseSchema"


@Entity(
    tableName = "NoteFolders",
    primaryKeys = ["relativePath"],
)
data class NoteFolder(
    val relativePath: String,
    val id: Int = RepoDatabase.generateUid()
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
}

@Entity(
    tableName = "Notes",
    primaryKeys = ["relativePath"],
)
@Parcelize
data class Note(
    val relativePath: String,
    val content: String,
    val lastModifiedTimeMillis: Long = Instant.now().toEpochMilli(),
    val id: Int = RepoDatabase.generateUid()
) : Parcelable {

    override fun toString(): String =
        "Note(relativePath=$relativePath, id=$id)"

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
}