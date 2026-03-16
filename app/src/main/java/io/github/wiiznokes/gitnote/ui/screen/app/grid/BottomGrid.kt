package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import kotlin.math.roundToInt


@Composable
fun FloatingActionButtons(
    vm: GridViewModel,
    offset: Float,
    onEditClick: (Note, EditType) -> Unit,
) {

    FloatingActionButton(
        modifier = Modifier
            .offset { IntOffset(x = 0, y = -offset.roundToInt()) },
        onClick = {
            onEditClick(vm.defaultNewNote(), EditType.Create)
        },
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "create note",
        )
    }
}
