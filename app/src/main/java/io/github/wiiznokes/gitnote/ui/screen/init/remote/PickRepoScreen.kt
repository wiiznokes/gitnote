package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import io.github.wiiznokes.gitnote.util.contains


sealed class Selected {
    data object None : Selected()
    data object Create : Selected()
    data class Index(val index: Int) : Selected()
}

@Composable
fun PickRepoScreen(
    onBackClick: () -> Unit,
    vm: InitViewModel,
    storageConfig: StorageConfiguration,
    onSuccess: () -> Unit
) {
    AppPage(
        title = "Select or Create a Repository",
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        onBackClick = onBackClick,
        disableVerticalScroll = true
    ) {

        val selected: MutableState<Selected> = remember {
            mutableStateOf(Selected.None)
        }

        val name = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(""))
        }

        val nameText = name.value.text

        val filteredRepos = remember(nameText, vm.repos) {
            val new = vm.repos.filter { it.name.contains(nameText) }

            if (nameText.isEmpty()) {
                selected.value = Selected.None
            } else if (new.isEmpty()) {
                selected.value = Selected.Create
            } else if (new.size == 1) {
                selected.value = Selected.Index(1)
            } else {
                selected.value = Selected.None
            }
            new
        }

        val nameFocusRequester = remember { FocusRequester() }

        LaunchedEffect(null) {
            nameFocusRequester.requestFocus()
        }

        OutlinedTextField(
            modifier = Modifier
                .focusRequester(nameFocusRequester),
            value = name.value,
            onValueChange = {
                name.value = it
            },
            placeholder = {
                Text(text = "Search or Create a Repo")
            },
            label = {
                Text(text = "Name")
            },
            singleLine = true,
        )


        val showCreate = nameText.isNotEmpty() && !vm.repos.contains { it.name == nameText }

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (showCreate) {

                val isSelected = selected.value == Selected.Create

                item {
                    Button(
                        onClick = {
                            if (isSelected) {
                                selected.value = Selected.None
                            } else {
                                selected.value = Selected.Create
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (!isSelected) BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        ) else null,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isSelected) 6.dp else 2.dp
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create new repo",
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create private repo ${vm.userInfo.username}/$nameText",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            itemsIndexed(filteredRepos) { index, item ->

                val selectedValue = selected.value
                val isSelected = selectedValue is Selected.Index && selectedValue.index == index

                Button(
                    modifier = Modifier
                        .widthIn(200.dp),
                    onClick = {
                        if (isSelected) {
                            selected.value = Selected.None
                        } else {
                            selected.value = Selected.Index(index)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = if (!isSelected) BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    ) else null,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isSelected) 6.dp else 2.dp
                    )
                ) {
                    Text(
                        text = item.fullRepoName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

        }


        Button(
            modifier = Modifier
                .width(250.dp),
            onClick = {

                val selected = selected.value

                if (selected is Selected.Create) {
                    vm.createNewRepoOnRemote(
                        repoName = "${vm.userInfo.username}/$nameText",
                        storageConfig = storageConfig,
                        onSuccess = onSuccess
                    )
                } else if (selected is Selected.Index) {

                    val repoInfo = filteredRepos[selected.index]

                    vm.cloneRepoFromAutomatic(
                        repoName = repoInfo.fullRepoName,
                        storageConfig = storageConfig,
                        onSuccess = onSuccess,
                    )
                }

            },
            enabled = selected.value !is Selected.None && vm.authState2.collectAsState().value.isClickable(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
        ) {
            Text(text = "Next")
        }
    }
}