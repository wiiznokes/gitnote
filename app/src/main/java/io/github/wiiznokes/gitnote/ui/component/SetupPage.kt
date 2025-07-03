package io.github.wiiznokes.gitnote.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SetupPage(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp)
    ) {
        if (title != null) {
            SetupTitle(
                title = title
            )
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}


@Composable
fun SetupLine(
    text: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
    ) {
        Text(text = text)
        content()
    }
}

@Composable
private fun SetupTitle(
    title: String,
) {
    Text(
        modifier = Modifier
            ,
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun NextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        modifier = Modifier
            .fillMaxSize(),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(text = text)
    }
}

@Composable
fun SetupButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    link: Boolean = false,
) {
    Button(
        modifier = Modifier
            .fillMaxSize(),
        onClick = onClick,
        enabled = enabled
    ) {
        if (link) {
            Text(text = text)
            Spacer(Modifier.width(5.dp))
            SimpleIcon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            )
        } else {
            Text(text = text)
        }
    }
}