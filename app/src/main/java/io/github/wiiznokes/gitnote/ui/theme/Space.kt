package io.github.wiiznokes.gitnote.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


val IconDefaultSize = 50.dp

// todo: use more this class
data class Spaces(
    val dialogSeparation: Dp = 80.dp,
    val smallPadding: Dp = 10.dp,
    val medium: Dp = 10.dp,
)


val LocalSpaces = compositionLocalOf { Spaces() }