package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import java.text.DateFormat
import java.util.Date
import androidx.paging.compose.LazyPagingItems

@Composable
internal fun NoteListView(
    gridNotes: LazyPagingItems<GridNote>,
    listState: LazyListState,
    topSpacerHeight: Dp,
    selectedNotes: List<Note>,
    showFullPathOfNotes: Boolean,
    onEditClick: (Note, EditType) -> Unit,
    vm: GridViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState
    ) {
        item {
            Spacer(modifier = Modifier.height(topSpacerHeight))
        }

        items(
            count = gridNotes.itemCount,
            key = { index ->
                val note = gridNotes[index]!!
                note.note.id
            }
        ) { index ->
            val gridNote = gridNotes[index]!!
            NoteListRow(
                gridNote = gridNote,
                vm = vm,
                onEditClick = onEditClick,
                selectedNotes = selectedNotes,
                showFullPathOfNotes = showFullPathOfNotes,
            )
        }

        item {
            Spacer(modifier = Modifier.height(topBarHeight + 10.dp))
        }
    }
}

@Composable
private fun NoteListRow(
    gridNote: GridNote,
    vm: GridViewModel,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<Note>,
    showFullPathOfNotes: Boolean,
) {
    val dropDownExpanded = remember { mutableStateOf(false) }
    val clickPosition = remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val formattedDate = remember(gridNote.note.lastModifiedTimeMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(gridNote.note.lastModifiedTimeMillis))
    }

    val title = gridNote.note.relativePath

    val rowBackground =
        if (gridNote.selected) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onLongClick = { dropDownExpanded.value = true },
                onClick = {
                    if (selectedNotes.isEmpty()) {
                        onEditClick(gridNote.note, EditType.Update)
                    } else {
                        vm.selectNote(gridNote.note, add = !gridNote.selected)
                    }
                }
            )
            .pointerInteropFilter {
                clickPosition.value = androidx.compose.ui.geometry.Offset(it.x, it.y)
                false
            }
    ) {
        Box {
            CustomDropDown(
                expanded = dropDownExpanded,
                shape = MaterialTheme.shapes.medium,
                options = listOf(
                    CustomDropDownModel(
                        text = stringResource(R.string.delete_this_note),
                        onClick = { vm.deleteNote(gridNote.note) }),
                    if (selectedNotes.isEmpty()) CustomDropDownModel(
                        text = stringResource(R.string.select_multiple_notes),
                        onClick = { vm.selectNote(gridNote.note, true) }) else null,
                ),
                clickPosition = clickPosition
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(80.dp)
        )
    }
}
