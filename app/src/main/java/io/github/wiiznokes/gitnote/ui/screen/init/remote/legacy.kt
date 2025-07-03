package io.github.wiiznokes.gitnote.ui.screen.init.remote


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