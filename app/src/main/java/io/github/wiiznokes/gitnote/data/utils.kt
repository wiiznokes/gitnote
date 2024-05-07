package io.github.wiiznokes.gitnote.data

import android.util.Log


private const val TAG = "DataUtils"


fun removeFirstAndLastSlash(input: String): String {
    var result = input

    if (result.isEmpty())
        return result

    if (result.length == 1) {
        if (result.first() == '/')
            return ""
        else
            return result
    }

    if (result.first() == '/') {
        result = result.substring(1)
    }

    if (result.last() == '/') {
        result = result.substring(0, result.length - 1)
    }

    return result
}

fun requireNotEndOrStartWithSlash(str: String) {
    val requirement = !str.startsWith("/") and !str.endsWith("/")
    if (!requirement) {
        Log.d(TAG, "error: requirement not satisfied for $str")
    }
    require(requirement)
}