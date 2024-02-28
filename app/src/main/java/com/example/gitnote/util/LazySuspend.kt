package com.example.gitnote.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class SuspendingLazy<T>(
    initializer: suspend () -> T

) {
    private var initializer: (suspend () -> T)? = initializer
    private var mutex: Mutex? = Mutex()
    private var _value: T? = null

    @Suppress("UNCHECKED_CAST")
    suspend fun value(): T {
        val m = mutex ?: return _value as T
        m.withLock {
            val i = initializer ?: return _value as T
            val v = i()
            _value = v
            initializer = null
            mutex = null
            return v
        }
    }
}


fun <T> toResult(fn: () -> T): Result<T> {
    return try {
        success(fn())
    } catch (e: Exception) {
        failure(e)
    }
}

fun <T> (() -> T).intoResult(): Result<T> {
    return try {
        success(this())
    } catch (e: Exception) {
        failure(e)
    }
}

fun <T> MutableList<T>.remove(predicate: (T) -> Boolean): Result<Int> {

    val index = this.indexOfFirst(predicate)

    if (index == -1) {
        return failure(Exception("No such element"))
    }

    this.removeAt(index = index)
    return success(index)
}

fun <T> Iterable<T>.contains(predicate: (T) -> Boolean): Boolean =
    firstOrNull(predicate) != null