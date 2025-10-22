package io.github.wiiznokes.gitnote.ui.screen.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Embedded
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.GetStringDialog
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.component.SimpleSpacer
import io.github.wiiznokes.gitnote.ui.theme.IconDefaultSize
import io.github.wiiznokes.gitnote.ui.theme.LocalSpaces
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import kotlinx.coroutines.launch


private const val TAG = "DrawerScreen"

data class DrawerFolderModel(
    @Embedded val noteFolder: NoteFolder,
    val noteCount: Int,
)


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DrawerScreen(
    vm: GridViewModel,
    drawerState: DrawerState
) {
    val currentNoteFolderRelativePath by vm.currentNoteFolderRelativePath.collectAsState()
    val currentNoteFolders by vm.drawerFolders.collectAsState()

    fun getParent(path: String) = path.substringBeforeLast(
        delimiter = "/",
        missingDelimiterValue = ""
    )

    if (drawerState.isOpen) {

        if (currentNoteFolderRelativePath.isEmpty()) {
            val scope = rememberCoroutineScope()
            BackHandler {
                scope.launch { drawerState.close() }
            }
        } else {
            BackHandler {
                vm.openFolder(getParent(currentNoteFolderRelativePath))
            }
        }
    }

    Scaffold(
        topBar = {
            RowNFoldersNavigation(
                vm = vm,
                currentPath = currentNoteFolderRelativePath
            )
        },
        floatingActionButton = {

            // bug: https://issuetracker.google.com/issues/224005027
            //AnimatedVisibility(visible = currentNoteFolderRelativePath.isNotEmpty()) {
            if (currentNoteFolderRelativePath.isNotEmpty()) {
                FloatingActionButton(
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        vm.openFolder(getParent(currentNoteFolderRelativePath))
                    }
                ) {
                    SimpleIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    ) { paddingValues ->


        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues = paddingValues),
            state = listState
        ) {

            items(
                currentNoteFolders,
                key = { it.noteFolder.id }) { drawerNoteFolder ->
                Box {
                    val dropDownExpanded = remember {
                        mutableStateOf(false)
                    }

                    val clickPosition = remember {
                        mutableStateOf(Offset.Zero)
                    }

                    // need this box for clickPosition
                    Box {
                        CustomDropDown(
                            expanded = dropDownExpanded,
                            shape = MaterialTheme.shapes.medium,
                            options = listOf(
                                CustomDropDownModel(
                                    text = stringResource(R.string.delete_this_folder),
                                    onClick = {
                                        vm.deleteFolder(drawerNoteFolder.noteFolder)
                                    }
                                ),
                            ),
                            clickPosition = clickPosition
                        )
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onLongClick = {
                                        dropDownExpanded.value = true
                                    },
                                    onClick = {
                                        vm.openFolder(drawerNoteFolder.noteFolder.relativePath)
                                    }
                                )
                                .pointerInteropFilter {
                                    clickPosition.value = Offset(it.x, it.y)
                                    false
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                                Text(
                                    text = drawerNoteFolder.noteCount.toString(),
                                    modifier = Modifier
                                        .padding(LocalSpaces.current.smallPadding)
                                )


                                Row(
                                    modifier = Modifier
                                        .padding(LocalSpaces.current.smallPadding),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SimpleIcon(
                                        modifier = Modifier
                                            .size(IconDefaultSize),
                                        imageVector = Icons.Rounded.Folder
                                    )

                                    SimpleSpacer(width = LocalSpaces.current.smallPadding)

                                    Text(
                                        text = drawerNoteFolder.noteFolder.fullName(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowNFoldersNavigation(
    vm: GridViewModel,
    currentPath: String
) {
    val containers = if (currentPath.isEmpty()) emptyList() else currentPath.split('/')

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    vm.openFolder("")
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = null,
                )
            }
        },
        title = {
            LazyRow(
                modifier = Modifier
            ) {

                itemsIndexed(containers) { index, item ->

                    if (index != 0) Text(
                        text = "/",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )

                    Text(
                        modifier = Modifier
                            .clickable {
                                val containersPart = containers.slice(0..index).iterator()
                                var path = ""

                                for (folder in containersPart) {
                                    path += if (containersPart.hasNext()) {
                                        "$folder/"
                                    } else {
                                        folder
                                    }
                                }
                                vm.openFolder(path)
                            },
                        text = item,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
            }
        },
        actions = {
            val showCreateNewFolder = rememberSaveable {
                mutableStateOf(false)
            }

            IconButton(onClick = {
                showCreateNewFolder.value = true
            }) {
                SimpleIcon(
                    imageVector = Icons.Rounded.CreateNewFolder
                )
            }

            GetStringDialog(
                expanded = showCreateNewFolder,
                label = stringResource(R.string.new_folder_label),
                actionText = stringResource(R.string.create_new_folder),
                unExpandedOnValidation = false
            ) {
                if (vm.createNoteFolder(currentPath, it)) {
                    showCreateNewFolder.value = false
                }
            }
        }
    )
}