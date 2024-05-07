package io.github.wiiznokes.gitnote.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


// todo: add more or find another solution
// https://www.reddit.com/r/Kotlin/comments/195k46c/lib_for_filtering_ascii_files/


@Parcelize
sealed class FileExtension(val text: String) : Parcelable {


    class Md : FileExtension("md")
    class Txt : FileExtension("txt")
    class Other(private val customText: String) : FileExtension(customText)

    companion object {

        val entries = arrayListOf(Md(), Txt())
        fun match(extension: String): FileExtension {
            entries.forEach {
                if (it.text.equals(extension, ignoreCase = true)) {
                    return it
                }
            }
            return Other(extension)
        }
    }

    override fun toString(): String {
        return text
    }
}
