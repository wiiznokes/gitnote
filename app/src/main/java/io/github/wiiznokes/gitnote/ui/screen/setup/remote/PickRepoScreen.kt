package io.github.wiiznokes.gitnote.ui.screen.setup.remote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.provider.RepoInfo
import io.github.wiiznokes.gitnote.provider.UserInfo
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.NextButton
import io.github.wiiznokes.gitnote.ui.component.SetupPage
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.viewmodel.InitState
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelI
import io.github.wiiznokes.gitnote.ui.viewmodel.SetupViewModelMock
import io.github.wiiznokes.gitnote.utils.contains


sealed class Selected {
    data object None : Selected()
    data object Create : Selected()
    data class Index(val index: Int) : Selected()
}

@Composable
fun PickRepoScreen(
    onBackClick: () -> Unit,
    authStep2State: InitState,
    vm: SetupViewModelI,
    userInfo: UserInfo,
    repos: List<RepoInfo>,
    storageConfig: StorageConfiguration,
    onClone: () -> Unit,
    onSuccess: () -> Unit
) {

    AppPage(
        title = stringResource(R.string.select_or_create_repo),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        onBackClick = onBackClick,
        onBackClickEnabled = !authStep2State.isLoading(),
        disableVerticalScroll = true
    ) {

        SetupPage {


            val selected: MutableState<Selected> = remember {
                mutableStateOf(Selected.None)
            }

            val name = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(""))
            }

            val nameText = name.value.text

            val filteredRepos = remember(nameText, repos) {
                val new = repos.filter { it.name.contains(nameText, ignoreCase = true) }

                if (new.isEmpty()) {
                    selected.value = Selected.Create
                } else {
                    selected.value = Selected.Index(0)
                }
                new
            }

            val nameFocusRequester = remember { FocusRequester() }

            LaunchedEffect(null) {
                nameFocusRequester.requestFocus()
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester),
                value = name.value,
                onValueChange = {
                    name.value = it
                },
                placeholder = {
                    Text(text = stringResource(R.string.search_or_create_a_repo))
                },
                label = {
                    Text(text = stringResource(R.string.name))
                },
                singleLine = true,
            )

            Spacer(Modifier.height(15.dp))

            val showCreate = nameText.isNotEmpty() && !repos.contains { it.name == nameText }

            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                if (showCreate) {

                    val isSelected = selected.value == Selected.Create

                    item {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth(),
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
                                    contentDescription = "Create new repository",
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val repoName = "${userInfo.username}/$nameText"
                                Text(
                                    text = stringResource(R.string.create_private_repo, repoName),
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
                            .fillMaxWidth(),
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

            NextButton(
                modifier = Modifier.padding(vertical = 10.dp),
                text = stringResource(R.string.next),
                onClick = {

                    val selected = selected.value

                    if (selected is Selected.Create) {
                        vm.createRepoAutomatic(
                            repoName = "${userInfo.username}/$nameText",
                            storageConfig = storageConfig,
                            onSuccess = onSuccess
                        )
                    } else if (selected is Selected.Index) {

                        val repoInfo = filteredRepos[selected.index]

                        vm.cloneRepoAutomatic(
                            repoName = repoInfo.fullRepoName,
                            storageConfig = storageConfig,
                            onSuccess = onSuccess,
                        )
                    }

                    onClone()
                },
                enabled = selected.value !is Selected.None,
            )
        }
    }
}


@Preview
@Composable
private fun PickRepoScreenPreview() {
    PickRepoScreen(
        onBackClick = {},
        authStep2State = InitState.Idle,
        vm = SetupViewModelMock(),
        userInfo = UserInfo(username = "", name = "", email = ""),
        repos = listOf(
            RepoInfo(name = "repoName1", owner = "wiiz", url = "repoName1", 0),
            RepoInfo(name = "repoName2", owner = "wiiz", url = "repoName1", 1),
            RepoInfo(name = "repoName3", owner = "wiiz", url = "repoName1", 2),
            RepoInfo(name = "repoName4", owner = "wiiz", url = "repoName1", 3),
            RepoInfo(name = "repoName5", owner = "wiiz", url = "repoName1", 4),
            RepoInfo(name = "repoName6", owner = "wiiz", url = "repoName1", 5),
            RepoInfo(name = "repoName7", owner = "wiiz", url = "repoName1", 6),
            RepoInfo(name = "repoName8", owner = "wiiz", url = "repoName1", 7),
            RepoInfo(name = "repoName9", owner = "wiiz", url = "repoName1", 8),
            RepoInfo(name = "repoName10", owner = "wiiz", url = "repoName1", 9),
            RepoInfo(name = "repoName1", owner = "wiiz", url = "repoName1", 0),
            RepoInfo(name = "repoName2", owner = "wiiz", url = "repoName1", 1),
            RepoInfo(name = "repoName3", owner = "wiiz", url = "repoName1", 2),
            RepoInfo(name = "repoName4", owner = "wiiz", url = "repoName1", 3),
            RepoInfo(name = "repoName5", owner = "wiiz", url = "repoName1", 4),
            RepoInfo(name = "repoName6", owner = "wiiz", url = "repoName1", 5),
            RepoInfo(name = "repoName7", owner = "wiiz", url = "repoName1", 6),
            RepoInfo(name = "repoName8", owner = "wiiz", url = "repoName1", 7),
            RepoInfo(name = "repoName9", owner = "wiiz", url = "repoName1", 8),
            RepoInfo(name = "repoName10", owner = "wiiz", url = "repoName1", 9),
        ),
        storageConfig = StorageConfiguration.App,
        onSuccess = {},
        onClone = {}
    )
}