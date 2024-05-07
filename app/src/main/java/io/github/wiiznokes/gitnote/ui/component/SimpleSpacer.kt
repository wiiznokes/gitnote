package io.github.wiiznokes.gitnote.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SimpleSpacer(
    height: Dp = 0.dp,
    width: Dp = 0.dp,
) {
    Spacer(
        modifier = Modifier
            .height(height)
            .width(width)
    )
}