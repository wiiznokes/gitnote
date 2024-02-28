package com.example.gitnote

import com.example.gitnote.helper.NameValidation
import org.junit.Test

class PlatformTest {


    @Test
    fun name() {
        assert(NameValidation.check("a"))

        assert(!NameValidation.check(" "))
        assert(!NameValidation.check(""))
        assert(!NameValidation.check("/"))
        assert(!NameValidation.check("\\"))

        assert(!NameValidation.check("a/a"))
        assert(!NameValidation.check("a\\a"))
        assert(!NameValidation.check("a\\a\\a"))
        assert(!NameValidation.check("a\\a\\a//\\b\\b/a/"))
        assert(!NameValidation.check("a\\a\\a//\\b\\b/a"))

        assert(!NameValidation.check("aaf\nef"))
    }
}