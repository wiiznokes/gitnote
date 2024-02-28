package com.example.gitnote.data.platform

import android.os.Environment
import com.example.gitnote.data.removeFirstAndLastSlash
import com.example.gitnote.ui.model.FileExtension
import com.example.gitnote.util.toResult
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText


private const val TAG = "FileSystem"

object FileSystem {
    val defaultDir = Environment.getExternalStorageDirectory().toFolderFs()
}

sealed class NodeFs {

    abstract val path: String
    abstract val fullName: String

    abstract fun delete(): Result<Unit>

    abstract fun create(): Result<Unit>

    abstract fun exist(): Boolean
}


class FileFs(
    override val path: String,
    override val fullName: String,
    val extension: FileExtension
) : NodeFs() {

    private val pathFs: Path = Paths.get(path)

    fun nameWithoutExtension() = fullName.substring(0, fullName.lastIndex - extension.text.length)

    companion object {
        fun fromPath(path: String): FileFs = Paths.get(path).toFileFs()
        fun fromPath(prefix: String, suffix: String): FileFs {
            return Paths.get(prefix).resolve(removeFirstAndLastSlash(suffix)).toFileFs()
        }


    }


    override fun delete(): Result<Unit> {

        return toResult { pathFs.deleteExisting() }
    }

    override fun create(): Result<Unit> {
        return toResult { pathFs.createFile() }
    }

    override fun exist(): Boolean {
        return pathFs.exists()
    }

    fun write(text: String): Result<Unit> {
        return toResult { pathFs.writeText(text) }
    }

    fun containerFolder(): FolderFs? {
        return pathFs.parent?.toFolderFs()
    }

    fun readText(): String {
        return pathFs.readText()
    }


}


class FolderFs(
    override val path: String,
    override val fullName: String,
) : NodeFs() {

    companion object {
        private const val TAG = "FolderFs"

        fun fromPath(path: String): FolderFs = Paths.get(path).toFolderFs()
        fun fromPath(prefix: String, suffix: String): FolderFs {
            return Paths.get(prefix).resolve(removeFirstAndLastSlash(suffix)).toFolderFs()
        }

    }

    private val pathFs: Path = Paths.get(path)

    fun listFolder(glob: String = "*"): List<FolderFs> {
        val folders = mutableListOf<FolderFs>()

        val entries = try {
            pathFs.listDirectoryEntries(glob)
        } catch (e: Exception) {
            e.printStackTrace()
            return folders
        }
        entries.filter { it.isDirectory() }.forEach {
            folders.add(it.toFolderFs())
        }

        return folders
    }

    override fun exist(): Boolean {
        return pathFs.exists()
    }

    /// return success if the folder is
    /// and existing empty directory
    fun isEmptyDirectory(): Result<Unit> {

        try {
            if (!pathFs.isDirectory()) {
                return failure(Exception("The path is not a directory"))
            }

            if (pathFs.listDirectoryEntries().isNotEmpty()) {
                return failure(Exception("The path is not empty"))
            }

        } catch (e: Exception) {
            return failure(e)
        }
        return success(Unit)
    }

    fun listFile(glob: String = "*"): List<FileFs> {
        val files = mutableListOf<FileFs>()

        val entries = try {
            pathFs.listDirectoryEntries(glob)
        } catch (e: Exception) {
            e.printStackTrace()
            return files
        }
        entries.filter { !it.isDirectory() }.forEach {
            files.add(it.toFileFs())
        }


        return files
    }

    suspend fun forEachNodeFs(fn: suspend (NodeFs) -> Unit) {
        try {
            pathFs.forEachDirectoryEntry { path ->
                fn(path.toNodeFs())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isHidden(): Boolean {
        return pathFs.isHidden()
    }

    fun isSym(): Boolean {
        return pathFs.isSymbolicLink()
    }

    fun createFolder(name: String): Result<Unit> {
        return toResult {
            pathFs.resolve(name).createDirectory()
        }
    }

    fun createFile(name: String): Result<Unit> {
        return toResult {
            pathFs.resolve(name).createFile()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun delete(): Result<Unit> {
        return toResult {
            pathFs.deleteRecursively()
        }
    }

    override fun create(): Result<Unit> {
        return toResult {
            pathFs.createDirectory()
        }
    }

    fun parent(): FolderFs? {
        return pathFs.parent?.toFolderFs()
    }


}


private fun File.toFolderFs(): FolderFs {
    return FolderFs(
        fullName = this.name,
        path = this.path
    )
}

private fun Path.toFolderFs(): FolderFs {
    return FolderFs(
        fullName = this.name,
        path = this.pathString
    )
}


private fun Path.toFileFs(): FileFs {
    val extension = this.extension.run {
        FileExtension.match(this)
    }

    return FileFs(
        fullName = this.name,
        path = this.pathString,
        extension = extension
    )
}


private fun Path.toNodeFs(): NodeFs {
    return if (this.isDirectory()) {
        this.toFolderFs()
    } else {
        this.toFileFs()
    }
}

fun NodeFs.toFile(): File {
    return File(path)
}