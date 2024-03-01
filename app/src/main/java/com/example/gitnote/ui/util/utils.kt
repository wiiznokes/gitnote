package com.example.gitnote.ui.util

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

@Composable
fun Modifier.conditional(
    condition: Boolean,
    modifier: @Composable Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

fun crossFade() = fadeIn(tween()) togetherWith fadeOut(tween())

fun slide(backWard: Boolean = false) = slideInHorizontally(
    initialOffsetX = {
        if (backWard) -it else it
    }
) togetherWith slideOutHorizontally(
    targetOffsetX = {
        if (backWard) it else -it
    }
)

/**
 * This is similar to the map function, but the flow that was
 * mapped is also included in the result:
 *  flow1.mapAndCombine(f(flow1) -> flow3)
 *  will return: flow(1, 3)
 */
inline fun <T, R> Flow<T>.mapAndCombine(crossinline transform: suspend (value: T) -> R): Flow<Pair<T, R>> = transform { value ->
    return@transform emit(Pair(value, transform(value)))
}