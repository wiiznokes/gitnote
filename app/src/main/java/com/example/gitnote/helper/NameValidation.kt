package com.example.gitnote.helper


class NameValidation {

    companion object {

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