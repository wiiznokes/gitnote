package io.github.wiiznokes.gitnote.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.GetStringDialog
import io.github.wiiznokes.gitnote.ui.component.RequestConfirmationDialog
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.viewmodel.SettingsViewModel


@Composable
fun FolderFiltersScreen(
    onBackClick: () -> Unit,
    vm: SettingsViewModel
) {

    val folderFilters = vm.prefs.folderFilters.getAsState()


    AppPage(
        title = "Ignore list",
        onBackClick = onBackClick,
        actions = {
            val showCreateFilter = rememberSaveable {
                mutableStateOf(false)
            }

            GetStringDialog(
                expanded = showCreateFilter,
                label = "Filter",
                "Add filter"
            ) { newFilter ->
                vm.update {
                    val new = folderFilters.value.plus(newFilter)
                    vm.update {
                        vm.prefs.folderFilters.update(new)
                    }
                }
            }

            IconButton(
                onClick = { showCreateFilter.value = true }) {
                SimpleIcon(imageVector = Icons.Default.Add)
            }
        },
        disableVerticalScroll = true
    ) {


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(folderFilters.value.toList()) {

                val showEditFilter = rememberSaveable {
                    mutableStateOf(false)
                }

                GetStringDialog(
                    expanded = showEditFilter,
                    label = "Filter",
                    actionText = "Change filter",
                    defaultString = it
                ) { newFilter ->
                    vm.update {
                        val new = folderFilters.value.minus(it).plus(newFilter)
                        vm.update {
                            vm.prefs.folderFilters.update(new)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showEditFilter.value = true
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier
                            .padding(10.dp),
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val showDeleteDialog = rememberSaveable {
                        mutableStateOf(false)
                    }

                    RequestConfirmationDialog(
                        expanded = showDeleteDialog,
                        text = "Do you really want to remove this filter ?"
                    ) {
                        vm.update {
                            vm.prefs.folderFilters.update(folderFilters.value.minus(it))
                        }
                    }

                    IconButton(onClick = {
                        showDeleteDialog.value = true
                    }) {
                        SimpleIcon(imageVector = Icons.Default.Delete)
                    }
                }

            }
        }
    }
}