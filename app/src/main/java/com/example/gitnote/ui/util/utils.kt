package com.example.gitnote.ui.util

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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

fun crossfade() = fadeIn(tween()) togetherWith fadeOut(tween())

fun slide(backWard: Boolean = false) = slideInHorizontally(
    initialOffsetX = {
        if (backWard) -it else it
    }
) togetherWith slideOutHorizontally(
    targetOffsetX = {
        if (backWard) it else -it
    }
)