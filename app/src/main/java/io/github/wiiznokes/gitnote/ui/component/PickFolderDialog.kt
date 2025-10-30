package io.github.wiiznokes.gitnote.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.screen.app.RowNFoldersNavigation
import io.github.wiiznokes.gitnote.ui.theme.IconDefaultSize
import io.github.wiiznokes.gitnote.ui.theme.LocalSpaces
import io.github.wiiznokes.gitnote.utils.getParentPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


private const val TAG = "PickFolder"

class PickFolderVm: ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase
    private val dao = db.repoDatabaseDao
    val uiHelper = MyApp.appModule.uiHelper
    private val storageManager: StorageManager = MyApp.appModule.storageManager


    private val _currentNoteFolderRelativePath = MutableStateFlow("")

    val currentNoteFolderRelativePath: StateFlow<String>
        get() = _currentNoteFolderRelativePath.asStateFlow()

    private val _selected: MutableStateFlow<String?> = MutableStateFlow(null)

    val selected: StateFlow<String?>
        get() = _selected.asStateFlow()

    fun select(value: String?) {
        viewModelScope.launch {
            _selected.emit(value)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val noteFolders = currentNoteFolderRelativePath.flatMapLatest { currentNoteFolderRelativePath ->
        dao.noteFolders(currentNoteFolderRelativePath)
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun openFolder(relativePath: String) {
        viewModelScope.launch {
            _currentNoteFolderRelativePath.emit(relativePath)
        }
    }

    fun createNoteFolder(relativeParentPath: String, name: String): Boolean {
        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return false
        }

        val relativePath = "$relativeParentPath/$name"

        val noteFolder = NoteFolder.new(
            relativePath = relativePath
        )

        if (noteFolder.toFolderFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_folder_already_exist))
            return false
        }

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.createNoteFolder(noteFolder)
        }

        return true
    }

}

@Composable
fun PickFolderDialog(
    expanded: MutableState<Boolean>,
    onSelectedFolder: (String) -> Unit,
) {

    if (expanded.value) {
        val vm: PickFolderVm  = viewModel()

        PickFolderDialogInternal(
            expanded = expanded,
            onSelectedFolder = onSelectedFolder,
            selected = vm.selected.collectAsState().value,
            select = vm::select,
            currentNoteFolderRelativePath = vm.currentNoteFolderRelativePath.collectAsState().value,
            noteFolders = vm.noteFolders.collectAsState().value,
            openFolder = vm::openFolder,
            createNoteFolder = vm::createNoteFolder
        )
    }
}
@Composable
private fun PickFolderDialogInternal(
    expanded: MutableState<Boolean>,
    selected: String?,
    select: (String?) -> Unit,
    currentNoteFolderRelativePath: String,
    noteFolders: List<NoteFolder>,
    openFolder: (String) -> Unit,
    createNoteFolder: (relativeParentPath: String, name: String) -> Boolean,
    onSelectedFolder: (String) -> Unit,
) {

    BackHandler(enabled = expanded.value) {
        if (currentNoteFolderRelativePath.isEmpty()) {
            expanded.value = false
            select(null)
        } else {
            openFolder(getParentPath(currentNoteFolderRelativePath))
            select(null)
        }
    }

    BaseDialog(
        expanded = expanded,
        modifier = Modifier
            .fillMaxHeight(0.8f),
        verticalScrollEnabled = false
    ) {

        RowNFoldersNavigation(
            currentPath = currentNoteFolderRelativePath,
            openFolder = openFolder,
            createNoteFolder = createNoteFolder
        )

        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .weight(1f),
            state = listState
        ) {

            items(
                noteFolders,
                key = { it.id }) { noteFolder ->

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openFolder(noteFolder.relativePath)
                                select(null)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                            Checkbox(
                                modifier = Modifier
                                    .padding(LocalSpaces.current.smallPadding),
                                checked = selected == noteFolder.relativePath,
                                onCheckedChange = {
                                    if (it)
                                        select(noteFolder.relativePath)
                                    else
                                        select(null)
                                }
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
                                    text = noteFolder.fullName(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            enabled = selected != null,
            onClick = {
                selected?.let {
                    onSelectedFolder(selected)
                    expanded.value = false
                }
            }
        ) {

            Text(stringResource(R.string.pick_folder))
        }
    }
}


@Composable
@Preview
private fun PickFolderDialogPreview() {
    PickFolderDialogInternal(
        expanded = remember { mutableStateOf(true) },
        selected = null,
        select = {},
        currentNoteFolderRelativePath = "",
        noteFolders = listOf(),
        openFolder = {},
        createNoteFolder = { _,_ ->
            true
        },
        onSelectedFolder = {},
    )
}