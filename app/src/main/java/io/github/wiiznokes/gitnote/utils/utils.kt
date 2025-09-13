package io.github.wiiznokes.gitnote.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
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
        e.printStackTrace()
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

fun String.endsWith(
    suffix: String,
    ignoreCase: Boolean = false,
    startIndex: Int = this.length
): Boolean {
    if (startIndex < 0 || startIndex > this.length) return false
    if (startIndex < suffix.length) return false

    return regionMatches(
        startIndex - suffix.length,
        suffix,
        0,
        suffix.length,
        ignoreCase = ignoreCase
    )
}

/**
 * This is similar to the map function, but the flow that was
 * mapped is also included in the result:
 *  flow1.mapAndCombine(f(flow1) -> flow3)
 *  will return: flow(1, 3)
 */
inline fun <T, R> Flow<T>.mapAndCombine(crossinline transform: suspend (value: T) -> R): Flow<Pair<T, R>> =
    transform { value ->
        return@transform emit(Pair(value, transform(value)))
    }
