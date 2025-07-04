package io.github.wiiznokes.gitnote.ui.screen.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.GetStringDialog
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.theme.LocalSpaces
import io.github.wiiznokes.gitnote.ui.viewmodel.FileExplorerViewModel
import io.github.wiiznokes.gitnote.ui.viewmodel.viewModelFactory

@Composable
fun FileExplorerScreen(
    path: String?,
    onDirectoryClick: (String) -> Unit,
    onFinish: (String) -> Unit,
    onBackClick: () -> Unit,
    title: String
) {


    val vm: FileExplorerViewModel = viewModel(
        factory = viewModelFactory {
            FileExplorerViewModel(
                path = path
            )
        },
        key = path
    )


    AppPage(
        title = vm.currentDir.path,
        titleStyle = MaterialTheme.typography.titleSmall,
        onBackClick = onBackClick,
        disableVerticalScroll = true,
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
                actionText = stringResource(R.string.create_new_folder)
            ) {
                if (vm.createDir(it)) {
                    onDirectoryClick(NodeFs.Folder.fromPath(vm.currentDir.path, it).path)
                }
            }
        },
        bottomBar = {

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalSpaces.current.medium),
                onClick = {
                    onFinish(vm.currentDir.path)
                }
            ) {
                Text(text = title)
            }
        }
    ) {

        LazyColumn(
            modifier = Modifier,
        ) {

            vm.currentDir.parent()?.let { parent ->
                item {
                    FolderRow(
                        item = FolderItem(
                            path = parent.path,
                            fullName = ".."
                        ),
                        onDirectoryClick = onDirectoryClick,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            items(vm.folders) { folderFs ->
                FolderRow(
                    item = FolderItem(
                        path = folderFs.path,
                        fullName = folderFs.fullName
                    ),
                    onDirectoryClick = onDirectoryClick
                )
            }
        }
    }
}


private data class FolderItem(
    val path: String,
    val fullName: String
)

@Composable
private fun FolderRow(
    item: FolderItem,
    onDirectoryClick: (String) -> Unit,
    color: Color = LocalContentColor.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onDirectoryClick(item.path)
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleIcon(
            modifier = Modifier
                .size(50.dp),
            imageVector = Icons.Rounded.Folder,
            tint = color
        )
        Text(
            text = item.fullName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = color
        )
    }
}