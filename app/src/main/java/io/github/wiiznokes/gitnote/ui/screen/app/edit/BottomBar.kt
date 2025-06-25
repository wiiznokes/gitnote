package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.ui.viewmodel.EditViewModel

val bottomBarHeight = 50.dp

@Composable
fun BottomBar(
    vm: EditViewModel,
    isReadOnlyModeActive: Boolean,
) {

    val textFormatExpanded = rememberSaveable(isReadOnlyModeActive) { mutableStateOf(false) }

    val modifier = Modifier
        .fillMaxWidth()
        .height(bottomBarHeight)
        .scrollable(rememberScrollState(initial = 0), orientation = Orientation.Horizontal)

    if (textFormatExpanded.value) {
        TextFormatRow(vm, modifier = modifier, textFormatExpanded)
    } else {
        DefaultRow(vm, modifier = modifier, textFormatExpanded, isReadOnlyModeActive)
    }
}

@Composable
private fun DefaultRow(
    vm: EditViewModel,
    modifier: Modifier,
    textFormatExpanded: MutableState<Boolean>,
    isReadOnlyModeActive: Boolean,
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallButton(
                onClick = {
                    textFormatExpanded.value = true
                },
                enabled = !isReadOnlyModeActive,
                imageVector = Icons.Default.TextFormat,
                contentDescription = "text format"
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            val history = vm.historyManager.collectAsState().value

            SmallButton(
                onClick = { vm.undo() },
                enabled = !isReadOnlyModeActive && history.index > 0,
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "undo"
            )
            SmallButton(
                onClick = { vm.redo() },
                enabled = !isReadOnlyModeActive && history.size - 1 > history.index,
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "redo"
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallButton(
                onClick = {},
                imageVector = Icons.Default.MoreVert,
                contentDescription = "more actions"
            )
        }
    }
}

@Composable
private fun TextFormatRow(
    vm: EditViewModel,
    modifier: Modifier,
    textFormatExpanded: MutableState<Boolean>
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scrollable(rememberScrollState(initial = 0), orientation = Orientation.Horizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SmallButton(
            onClick = {},
            imageVector = Icons.Default.Title,
            contentDescription = "title"
        )

        SmallButton(
            onClick = {},
            imageVector = Icons.Default.FormatBold,
            contentDescription = "bold"
        )
        SmallButton(
            onClick = {},
            imageVector = Icons.Default.FormatItalic,
            contentDescription = "italic"
        )

        SmallSeparator()

        SmallButton(
            onClick = {},
            imageVector = Icons.Default.Link,
            contentDescription = "link"
        )

        SmallButton(
            onClick = {},
            imageVector = Icons.Default.Code,
            contentDescription = "code"
        )
        SmallButton(
            onClick = {},
            imageVector = Icons.Default.FormatQuote,
            contentDescription = "quote"
        )

        SmallSeparator()

        SmallButton(
            onClick = {},
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = "unordered list"
        )
        SmallButton(
            onClick = {},
            imageVector = Icons.Default.Checklist,
            contentDescription = "checklist"
        )
        SmallButton(
            onClick = {},
            imageVector = Icons.Default.FormatListNumbered,
            contentDescription = "list number"
        )


        SmallSeparator()

        SmallButton(
            onClick = {
                textFormatExpanded.value = false
            },
            imageVector = Icons.Default.Close,
            contentDescription = "close"
        )
    }
}

@Composable
private fun SmallSeparator(
) {
    VerticalDivider(
        modifier = Modifier.padding(horizontal = 5.dp),
        color = Color.Gray,
        thickness = 1.dp,
    )
}

@Composable
private fun SmallButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    enabled: Boolean = true,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}