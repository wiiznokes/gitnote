package com.example.gitnote.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SimpleButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit = {},
) {
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = text,
            style = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center
            )
        )
    }
}


@Composable
fun BoxScope.CenteredButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    text: String
) {
    Button(
        modifier = modifier
            .align(Alignment.Center),
        onClick = onClick
    ) {
        Text(text = text)
    }
}