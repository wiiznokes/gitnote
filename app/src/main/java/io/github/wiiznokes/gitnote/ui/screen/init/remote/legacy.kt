package io.github.wiiznokes.gitnote.ui.screen.init.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.rememberNavController
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.NewRepoState
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.destination.RemoteDestination
import io.github.wiiznokes.gitnote.ui.model.Provider
import io.github.wiiznokes.gitnote.ui.theme.LocalSpaces
import io.github.wiiznokes.gitnote.ui.viewmodel.CloneState
import io.github.wiiznokes.gitnote.ui.viewmodel.InitViewModel
import kotlinx.coroutines.launch


/*
@Composable
private fun OpenLinkButton(
    text: String,
    url: String
) {

    val uriHandler = LocalUriHandler.current
    Button(
        onClick = {
            uriHandler.openUri(url)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = text)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = text
            )
        }

    }
}


@Composable
private fun OpenLinks(
    vm: InitViewModel,
    provider: MutableState<Provider>
) {

    Column(modifier = Modifier.padding(15.dp)) {

        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.quick_links),
            style = MaterialTheme.typography.bodyMedium
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                var providerExpanded by remember {
                    mutableStateOf(false)
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                    Box {

                        Button(
                            modifier = Modifier.padding(start = 7.dp),
                            onClick = { providerExpanded = !providerExpanded },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(
                                text = provider.value.name
                            )
                        }


                        DropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false }
                        ) {

                            Provider.entries.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(text = it.name)
                                    },
                                    onClick = {
                                        providerExpanded = false
                                        provider.value = it
                                        vm.viewModelScope.launch {
                                            vm.prefs.provider.update(it)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }


                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        provider.value.listRepo?.let {
                            OpenLinkButton(
                                text = stringResource(R.string.quick_links_see_repos),
                                url = it
                            )
                        }
                        provider.value.createToken?.let {
                            OpenLinkButton(
                                text = stringResource(R.string.quick_links_create_token),
                                url = it
                            )
                        }
                        provider.value.createRepo?.let {
                            OpenLinkButton(
                                text = stringResource(R.string.quick_links_create_repo),
                                url = it
                            )
                        }
                        provider.value.mainPage?.let {
                            OpenLinkButton(
                                text = stringResource(R.string.quick_links_home_page),
                                url = it
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 20.dp, horizontal = 15.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }

}


@Composable
fun RemoteScreen2(
    vm: InitViewModel,
    repoState: NewRepoState,
    onInitSuccess: () -> Unit,
    onBackClick: () -> Unit
) {

    val navController =
        rememberNavController(startDestination = RemoteDestination.SelectProvider(repoState))

    NavBackHandler(navController)

    AppPage(
        title = stringResource(R.string.app_page_clone_repository),
        horizontalAlignment = Alignment.CenterHorizontally,
        onBackClick = onBackClick,
    ) {

        val provider = remember {
            val provider = vm.prefs.provider.getBlocking()
            mutableStateOf(provider)
        }

        OpenLinks(
            vm = vm,
            provider = provider
        )

        ElevatedCard(
            modifier = Modifier
                .padding(vertical = 50.dp)
        ) {
            Text(
                text = stringResource(R.string.where_repo_is_cloned),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = repoState.repoPath(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                )
            )
        }

        var repoUrl by remember {
            val repoUrl = vm.prefs.remoteUrl.getBlocking()
            mutableStateOf(TextFieldValue(repoUrl))
        }

        InitGroupExplication(
            explication = stringResource(R.string.clone_step_url)
        ) {
            OutlinedTextField(
                value = repoUrl,
                onValueChange = {
                    repoUrl = it
                },
                label = {
                    Text(text = stringResource(R.string.clone_step_url_label))
                },
                singleLine = true,
                isError = repoUrl.text.contains(" ") || repoUrl.text.isEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                )
            )
        }

        var withCreed by remember {
            mutableStateOf(true)
        }

        ElevatedCard(
            modifier = Modifier
                .padding(top = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.with_credentials))
                IconButton(
                    onClick = { withCreed = !withCreed }
                ) {
                    if (withCreed) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = null
                        )
                    }
                }
            }
        }


        var userName by remember {
            val userName = vm.prefs.userName.getBlocking()
            mutableStateOf(TextFieldValue(userName))
        }

        var password by remember {
            val password = vm.prefs.password.getBlocking()
            mutableStateOf(TextFieldValue(password))
        }


        AnimatedVisibility(visible = withCreed) {
            Column {
                InitGroupExplication(
                    explication = stringResource(R.string.clone_step_username)
                ) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                        },
                        label = {
                            Text(text = stringResource(R.string.clone_step_username_label))
                        },
                        singleLine = true,
                        isError = userName.text.isEmpty()
                    )

                }

                InitGroupExplication(
                    explication = stringResource(R.string.clone_step_password)
                ) {

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                        },
                        label = {
                            Text(stringResource(R.string.clone_step_password_label))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = userName.text.isEmpty()
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(40.dp))

        val cloneState by vm.cloneState.collectAsState()

        val currentCloneState = cloneState

        if (currentCloneState is CloneState.Cloning) {
            Text(text = "${currentCloneState.percent} %")
        }

        Button(
            onClick = {
                vm.cloneRepo(
                    repoState = repoState,
                    repoUrl = repoUrl.text,
                    gitCreed = if (withCreed) GitCreed(
                        userName = userName.text,
                        password = password.text
                    ) else null,
                    onSuccess = onInitSuccess
                )
            },
            enabled = cloneState.isClickable()
        ) {
            Text(text = stringResource(R.string.clone_repo_button))
            Icon(imageVector = Icons.Default.Download, contentDescription = null)
        }
    }

}


@Composable
private fun InitGroupExplication(
    explication: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalSpaces.current.medium),
            text = explication,
            style = MaterialTheme.typography.titleSmall
        )

        content()
    }
}
*/