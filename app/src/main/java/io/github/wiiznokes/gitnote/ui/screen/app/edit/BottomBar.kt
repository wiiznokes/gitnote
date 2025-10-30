package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.TextVM
import io.github.wiiznokes.gitnote.utils.getParentPath

val bottomBarHeight = 50.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultRow(
    vm: TextVM,
    modifier: Modifier = Modifier,
    isReadOnlyModeActive: Boolean,
    leftContent: @Composable () -> Unit = {}
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .scrollable(rememberScrollState(initial = 0), orientation = Orientation.Horizontal),
    ) {

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftContent()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter),
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

        val bottomSheetExpanded = rememberSaveable { mutableStateOf(false) }

        if (bottomSheetExpanded.value) {
            ModalBottomSheet(onDismissRequest = { bottomSheetExpanded.value = false }) {
                Text(
                    modifier = Modifier
                        .padding(10.dp),
                    text = stringResource(R.string.extension, vm.previousNote.fileExtension().text)
                )
                Text(
                    modifier = Modifier
                        .padding(10.dp),
                    text = stringResource(
                        R.string.parent,
                        getParentPath(vm.previousNote.relativePath)
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallButton(
                onClick = { bottomSheetExpanded.value = true },
                imageVector = Icons.Default.MoreVert,
                contentDescription = "more actions"
            )
        }
    }
}


@Composable
fun SmallSeparator(
) {
    VerticalDivider(
        modifier = Modifier.padding(horizontal = 5.dp),
        color = Color.Gray,
        thickness = 1.dp,
    )
}

@Composable
fun SmallButton(
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