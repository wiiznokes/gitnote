package io.github.wiiznokes.gitnote.data.platform

import android.os.Environment
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.removeFirstAndLastSlash
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.utils.toResult
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.getLastModifiedTime
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

sealed class NodeFs(
    open val path: String,
    open val fullName: String,
    protected val pathFs: Path = Paths.get(path)
) {

    fun fileSize(): Long {
        return pathFs.fileSize()
    }

    fun lastModifiedTime(): FileTime {
        return pathFs.getLastModifiedTime()
    }

    fun isHidden(): Boolean {
        return pathFs.isHidden()
    }

    fun isSym(): Boolean {
        return pathFs.isSymbolicLink()
    }

    fun parent(): Folder? {
        return pathFs.parent?.toFolderFs()
    }

    fun exist(): Boolean {
        return pathFs.exists()
    }

    abstract fun delete(): Result<Unit>

    abstract fun create(): Result<Unit>


    class File(
        override val path: String,
        override val fullName: String,
        val extension: FileExtension,
    ) : NodeFs(path, fullName) {

        companion object {
            fun fromPath(path: String): File = Paths.get(path).toFileFs()
            fun fromPath(prefix: String, suffix: String): File {
                return Paths.get(prefix).resolve(removeFirstAndLastSlash(suffix)).toFileFs()
            }
        }

        fun nameWithoutExtension() =
            fullName.take(fullName.lastIndex - extension.text.length)

        override fun delete(): Result<Unit> {

            return toResult { pathFs.deleteExisting() }
        }

        override fun create(): Result<Unit> {
            return toResult {
                pathFs.createParentDirectories()
                pathFs.createFile()
            }
        }


        fun write(text: String): Result<Unit> {
            return toResult { pathFs.writeText(text) }
        }

        fun readText(): String {
            return pathFs.readText()
        }

    }


    class Folder(
        override val path: String,
        override val fullName: String,
    ) : NodeFs(path, fullName) {

        companion object {
            private const val TAG = "FolderFs"

            fun fromPath(path: String): Folder = Paths.get(path).toFolderFs()
            fun fromPath(prefix: String, suffix: String): Folder {
                return Paths.get(prefix).resolve(removeFirstAndLastSlash(suffix)).toFolderFs()
            }

        }

        suspend fun <T> filterMapNodeFs(fn: suspend (NodeFs) -> T?): List<T> {
            val output = mutableListOf<T>()
            try {
                pathFs.forEachDirectoryEntry { path ->
                    fn(path.toNodeFs())?.let {
                        output.add(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return output
        }

        /**
         * Returns success if the folder is
         * and existing empty directory
         */
        fun isEmptyDirectory(): Result<Unit> {

            try {
                if (!pathFs.isDirectory()) {
                    return failure(Exception(MyApp.appModule.context.getString(R.string.error_path_not_directory)))
                }

                if (pathFs.listDirectoryEntries().isNotEmpty()) {
                    return failure(Exception(MyApp.appModule.context.getString(R.string.error_path_not_empty)))
                }

            } catch (e: Exception) {
                return failure(e)
            }
            return success(Unit)
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
                pathFs.createDirectories()
            }
        }

    }
}


private fun File.toFolderFs(): NodeFs.Folder {
    return NodeFs.Folder(
        fullName = this.name,
        path = this.path
    )
}

private fun Path.toFolderFs(): NodeFs.Folder {
    return NodeFs.Folder(
        fullName = this.name,
        path = this.pathString
    )
}


private fun Path.toFileFs(): NodeFs.File {
    val extension = this.extension.run {
        FileExtension.match(this)
    }

    return NodeFs.File(
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