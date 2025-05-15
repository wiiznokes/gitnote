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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.screen.app.DrawerScreen
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel


private const val TAG = "GridScreen"

private const val maxOffset = -500f
internal val topBarHeight = 80.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GridScreen(
    onSettingsClick: () -> Unit,
    onEditClick: (Note, EditType) -> Unit,
    onStorageFailure: () -> Unit,
) {

    val vm: GridViewModel = viewModel()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet {
            DrawerScreen(
                vm = vm, drawerState = drawerState
            )
        }
    }) {

        val selectedNotes by vm.selectedNotes.collectAsState()

        if (selectedNotes.isNotEmpty()) {
            BackHandler {
                vm.unselectAllNotes()
            }
        }

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
                topBarHeight = topBarHeight,
                onEditClick = onEditClick,
                selectedNotes = selectedNotes,
                nestedScrollConnection = nestedScrollConnection,
            )

            TopBar(
                vm = vm,
                offset = offset.floatValue,
                selectedNotesNumber = selectedNotes.size,
                drawerState = drawerState,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester,
                padding = padding
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
    topBarHeight: Dp,
    vm: GridViewModel,
    nestedScrollConnection: NestedScrollConnection,
    onEditClick: (Note, EditType) -> Unit,
    selectedNotes: List<Note>,
) {
    val gridNotes by vm.gridNotes.collectAsState()

    val gridState = rememberLazyStaggeredGridState()


    val query = vm.query.collectAsState()

    var lastQuery: String = rememberSaveable { query.value }

    if (lastQuery != query.value) {
        @Suppress("UNUSED_VALUE")
        lastQuery = query.value
        LaunchedEffect(null) {
            gridState.animateScrollToItem(index = 0)
        }
    }


    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, {
        Log.d(TAG, "pull refresh")
        vm.refresh()
    })

    Box {

        // todo: scroll even when there is nothing to scroll
        // todo: add scroll bar

        val noteMinWidth = vm.prefs.noteMinWidth.getAsState()
        val showFullPathOfNotes = vm.prefs.showFullPathOfNotes.getAsState()
        val showFullNoteHeight = vm.prefs.showFullNoteHeight.getAsState()

        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .nestedScroll(nestedScrollConnection), contentPadding = PaddingValues(
                horizontal = 3.dp
            ), columns = StaggeredGridCells.Adaptive(noteMinWidth.value.size.dp), state = gridState

        ) {

            item(
                span = StaggeredGridItemSpan.FullLine
            ) {
                Spacer(modifier = Modifier.height(topBarHeight + 40.dp + 15.dp))
            }

            items(items = gridNotes, key = { it.note.id }) { gridNote ->

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
                    modifier = Modifier
                        .sizeIn(
                            maxHeight = if (showFullNoteHeight.value) Dp.Unspecified else 500.dp
                        )
                        .padding(3.dp)
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

                        // need this box for clickPosition
                        Box {
                            CustomDropDown(
                                expanded = dropDownExpanded,
                                shape = MaterialTheme.shapes.medium,
                                options = listOf(
                                    CustomDropDownModel(
                                        text = stringResource(R.string.delete_this_note),
                                        onClick = {
                                            vm.deleteNote(gridNote.note)
                                        }),
                                    if (selectedNotes.isEmpty()) CustomDropDownModel(
                                        text = stringResource(
                                            R.string.select_multiple_notes
                                        ), onClick = {
                                            vm.selectNote(gridNote.note, true)
                                        }) else null,
                                ),
                                clickPosition = clickPosition
                            )
                        }
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                text = if (showFullPathOfNotes.value) gridNote.note.relativePath else gridNote.title,
                                modifier = Modifier.padding(bottom = 6.dp),
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )

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

            item(
                span = StaggeredGridItemSpan.FullLine
            ) {
                Spacer(modifier = Modifier.height(topBarHeight + 10.dp))
            }
        }

        // fix me: https://stackoverflow.com/questions/74594418/pullrefreshindicator-overlaps-with-scrollabletabrow
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarHeight),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            scale = true
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
                Log.d(TAG, "calculateOffset(newOffset: ${offset.floatValue}, delta: $delta)")
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