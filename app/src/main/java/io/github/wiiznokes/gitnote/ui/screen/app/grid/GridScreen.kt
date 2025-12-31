package io.github.wiiznokes.gitnote.ui.screen.app.grid

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.model.NoteViewType
import io.github.wiiznokes.gitnote.ui.screen.app.DrawerScreen
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel


private const val TAG = "GridScreen"

private const val maxOffset = -500f
internal val topBarHeight = 80.dp

internal val topSpacerHeight = topBarHeight + 40.dp + 15.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GridScreen(
    onSettingsClick: () -> Unit,
    onEditClick: (Note, EditType) -> Unit,
) {

    val vm: GridViewModel = viewModel()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet {
            DrawerScreen(
                drawerState = drawerState,
                currentNoteFolderRelativePath = vm.currentNoteFolderRelativePath.collectAsState().value,
                drawerFolders = vm.drawerFolders.collectAsState().value,
                openFolder = vm::openFolder,
                deleteFolder = vm::deleteFolder,
                createNoteFolder = vm::createNoteFolder,
                allTags = vm.allTags.collectAsState<List<String>>().value,
                selectedTag = vm.selectedTag.collectAsState<String?>().value,
                onTagSelected = vm::selectTag,
            )
        }
    }) {

        val selectedNotes by vm.selectedNotes.collectAsState()

        if (selectedNotes.isNotEmpty()) {
            BackHandler {
                vm.unselectAllNotes()
            }
        }

        val noteViewType by vm.prefs.noteViewType.getAsState()

        val searchFocusRequester = remember { FocusRequester() }

        val fabExpanded = remember {
            mutableStateOf(false)
        }

        val offset = remember { mutableFloatStateOf(0f) }

        Scaffold(
            contentWindowInsets = WindowInsets.safeContent,
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {

                if (selectedNotes.isEmpty()) {
                    FloatingActionButtons(
                        vm = vm,
                        offset = offset.floatValue,
                        onEditClick = onEditClick,
                        searchFocusRequester = searchFocusRequester,
                        expanded = fabExpanded,
                    )
                }

            }) { padding ->

            val nestedScrollConnection = rememberNestedScrollConnection(
                offset = offset,
                fabExpanded = fabExpanded,
            )


            GridView(
                vm = vm,
                onEditClick = onEditClick,
                selectedNotes = selectedNotes,
                nestedScrollConnection = nestedScrollConnection,
                padding = padding,
                noteViewType = noteViewType,
            )

            TopBar(
                vm = vm,
                offset = offset.floatValue,
                selectedNotesNumber = selectedNotes.size,
                drawerState = drawerState,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester,
                padding = padding,
                onReloadDatabase = {
                    vm.reloadDatabase()
                }
            )

        }
    }
}


@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
private fun GridView(
    vm: GridViewModel,
    nestedScrollConnection: NestedScrollConnection,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<Note>,
    padding: PaddingValues,
    noteViewType: NoteViewType,
) {
    val gridNotes = vm.gridNotes.collectAsLazyPagingItems<GridNote>()
    val query = vm.query.collectAsState()


    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, {
        Log.d(TAG, "pull refresh")
        vm.refresh()
    })

    val showFullPathOfNotes = vm.prefs.showFullPathOfNotes.getAsState()
    val showFullTitleInListView = vm.prefs.showFullTitleInListView.getAsState()

    Box {

        // todo: scroll even when there is nothing to scroll
        // todo: add scroll bar

        val commonModifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
            .nestedScroll(nestedScrollConnection)

        when (noteViewType) {
            NoteViewType.Grid -> {
                val gridState = rememberLazyStaggeredGridState()

                LaunchedEffect(query.value) {
                    gridState.animateScrollToItem(index = 0)
                }

                GridNotesView(
                    gridNotes = gridNotes,
                    gridState = gridState,
                    modifier = commonModifier,
                    selectedNotes = selectedNotes,
                    showFullPathOfNotes = showFullPathOfNotes.value,
                    onEditClick = onEditClick,
                    vm = vm,
                )
            }

            NoteViewType.List -> {
                val listState = rememberLazyListState()

                LaunchedEffect(query.value) {
                    listState.animateScrollToItem(index = 0)
                }

                NoteListView(
                    gridNotes = gridNotes,
                    listState = listState,
                    modifier = commonModifier,
                    selectedNotes = selectedNotes,
                    showFullPathOfNotes = showFullPathOfNotes.value,
                    showFullTitleInListView = showFullTitleInListView.value,
                    onEditClick = onEditClick,
                    vm = vm,
                )
            }
        }

        // fix me: https://stackoverflow.com/questions/74594418/pullrefreshindicator-overlaps-with-scrollabletabrow
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarHeight + padding.calculateTopPadding()),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            scale = true
        )
    }

}


@Composable
private fun GridNotesView(
    gridNotes: LazyPagingItems<GridNote>,
    gridState: LazyStaggeredGridState,
    modifier: Modifier = Modifier,
    selectedNotes: List<Note>,
    showFullPathOfNotes: Boolean,
    onEditClick: (Note, EditType) -> Unit,
    vm: GridViewModel,
) {


    val noteMinWidth = vm.prefs.noteMinWidth.getAsState()
    val showFullNoteHeight = vm.prefs.showFullNoteHeight.getAsState()

    LazyVerticalStaggeredGrid(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 3.dp),
        columns = StaggeredGridCells.Adaptive(noteMinWidth.value.size.dp),
        state = gridState
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(topSpacerHeight))
        }

        items(
            count = gridNotes.itemCount,
            key = { index -> gridNotes[index]?.note?.id ?: index }
        ) { index ->
            val gridNote = gridNotes[index]
            if (gridNote != null) {
                NoteCard(
                    gridNote = gridNote,
                    vm = vm,
                    onEditClick = onEditClick,
                    selectedNotes = selectedNotes,
                    showFullPathOfNotes = showFullPathOfNotes,
                    showFullNoteHeight = showFullNoteHeight.value,
                    modifier = Modifier.padding(3.dp)
                )
            } else {
                // Placeholder for loading item
                Card(
                    modifier = Modifier.padding(3.dp).height(100.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...")
                    }
                }
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(topBarHeight + 10.dp))
        }
    }
}

@Composable
private fun NoteCard(
    gridNote: GridNote,
    vm: GridViewModel,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<Note>,
    showFullPathOfNotes: Boolean,
    showFullNoteHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val dropDownExpanded = remember {
        mutableStateOf(false)
    }

    val clickPosition = remember {
        mutableStateOf(Offset.Zero)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (dropDownExpanded.value) {
            BorderStroke(
                width = 2.dp, color = MaterialTheme.colorScheme.primary
            )
        } else if (gridNote.selected) {
            BorderStroke(
                width = 2.dp, color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1000.dp)
            )
        },
        modifier = modifier
            .sizeIn(
                maxHeight = if (showFullNoteHeight) Dp.Unspecified else 500.dp
            )
            .combinedClickable(onLongClick = {
                dropDownExpanded.value = true
            }, onClick = {
                if (selectedNotes.isEmpty()) {
                    onEditClick(
                        gridNote.note, EditType.Update
                    )
                } else {
                    vm.selectNote(
                        gridNote.note, add = !gridNote.selected
                    )
                }
            })
            .pointerInteropFilter {
                clickPosition.value = Offset(it.x, it.y)
                false
            },
    ) {
        Box {

            NoteActionsDropdown(
                vm = vm,
                gridNote = gridNote,
                selectedNotes = selectedNotes,
                dropDownExpanded = dropDownExpanded,
                clickPosition = clickPosition
            )

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                val title = if (showFullPathOfNotes || !gridNote.isUnique) {
                    gridNote.note.relativePath
                } else {
                    gridNote.note.nameWithoutExtension()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    if (gridNote.completed != null) {
                        Checkbox(
                            checked = gridNote.completed!!,
                            onCheckedChange = { vm.toggleCompleted(gridNote.note) },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (gridNote.note.fileExtension() is FileExtension.Md) {
//                                MarkdownText(
//                                    markdown = gridNote.note.content,
//                                    disableLinkMovementMethod = true,
//                                    isTextSelectable = false,
//                                    onLinkClicked = { }
//                                )
                    Text(
                        text = gridNote.note.content,
                        modifier = Modifier,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = gridNote.note.content,
                        modifier = Modifier,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoteActionsDropdown(
    vm: GridViewModel,
    gridNote: GridNote,
    selectedNotes: List<Note>,
    dropDownExpanded: MutableState<Boolean>,
    clickPosition: MutableState<Offset>,
) {

    // need this box for clickPosition
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
                if (gridNote.completed != null) CustomDropDownModel(
                    text = stringResource(R.string.convert_to_note),
                    onClick = { vm.convertToNote(gridNote.note) }) else CustomDropDownModel(
                    text = stringResource(R.string.convert_to_task),
                    onClick = { vm.convertToTask(gridNote.note) }),
            ),
            clickPosition = clickPosition
        )
    }
}

// https://stackoverflow.com/questions/73079388/android-jetpack-compose-keyboard-not-close
// https://medium.com/@debdut.saha.1/top-app-bar-animation-using-nestedscrollconnection-like-facebook-jetpack-compose-b446c109ee52
// todo: fix scroll is blocked when the full size of the grid is the screen,
//  the stretching will cause tbe offset to not change
@Composable
private fun rememberNestedScrollConnection(
    offset: MutableFloatState,
    fabExpanded: MutableState<Boolean>,
): NestedScrollConnection {


    val keyboardController = LocalSoftwareKeyboardController.current

    return remember {
        var shouldBlock = false

        object : NestedScrollConnection {
            fun calculateOffset(delta: Float): Offset {
                offset.floatValue = (offset.floatValue + delta).coerceIn(maxOffset, 0f)
                //Log.d(TAG, "calculateOffset(newOffset: ${offset.floatValue}, delta: $delta)")
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                //Log.d(TAG, "onPreScroll(available: ${available.y})")
                if (!shouldBlock) keyboardController?.hide()

                fabExpanded.value = false

                return calculateOffset(available.y)
            }

            override fun onPostScroll(
                consumed: Offset, available: Offset, source: NestedScrollSource
            ): Offset {
                //Log.d(TAG, "onPostScroll(consumed: ${consumed.y}, available: ${available.y})")
                return calculateOffset(available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                shouldBlock = true
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                shouldBlock = false
                return super.onPostFling(consumed, available)
            }

        }
    }
}
