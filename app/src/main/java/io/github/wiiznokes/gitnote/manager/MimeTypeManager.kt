package io.github.wiiznokes.gitnote.manager

import io.github.wiiznokes.gitnote.manager.ExtensionType.Markdown
import io.github.wiiznokes.gitnote.manager.ExtensionType.Text


enum class ExtensionType {
    Text,
    Markdown;
}

fun extensionType(extension: String): ExtensionType? = extensionTypeLib(extension)?.let {
    extensionTypeFromNumber(it)
}

private fun extensionTypeFromNumber(num: Int): ExtensionType =
    when (num) {
        1 -> Text
        2 -> Markdown
        else -> throw Exception("Invalid number for ExtensionType: ^$num")
    }

private external fun extensionTypeLib(extension: String): Int?

external fun isExtensionSupported(extension: String): Boolean