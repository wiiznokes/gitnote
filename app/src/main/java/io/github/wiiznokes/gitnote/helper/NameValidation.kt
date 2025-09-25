package io.github.wiiznokes.gitnote.helper


class NameValidation {

    companion object {

        fun removeEndingWhiteSpace(name: String): String {
            return name.trimEnd()
        }

        /**
         * Best effort
         */
        fun check(
            name: String,
        ): Boolean {

            if (name.isBlank()) {
                return false
            }

            name.forEach {
                if (ILLEGAL_CHARACTERS.contains(it)) return false
            }

            return true
        }


        private val ILLEGAL_CHARACTERS = charArrayOf(
            '/',
            '\n',
            '\r',
            '\t',
            '\u0000',
            '\u000c',
            '`',
            '?',
            '*',
            '\\',
            '<',
            '>',
            '|',
            '\"',
            ':'
        )
    }
}