package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.BuildConfig
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.manager.SyncState
import io.github.wiiznokes.gitnote.manager.SyncState.Ok
import io.github.wiiznokes.gitnote.manager.SyncState.Pull
import io.github.wiiznokes.gitnote.manager.SyncState.Push
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.model.NoteViewType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


private const val TAG = "TopGridScreen"

private val ButtonSize = 35.dp

@Composable
fun TopBar(
    padding: PaddingValues,
    offset: Float,
    selectedNotesNumber: Int,
    drawerState: DrawerState,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
    onReloadDatabase: () -> Unit,
    query: String,
    clearQuery: () -> Unit,
    search: (String) -> Unit,
    noteViewType: NoteViewType,
    syncState: SyncState,
    consumeOkSyncState: () -> Unit,
    isReadOnlyModeActive: Boolean,
    updateSettings: (suspend AppPreferences.() -> Unit) -> Unit,
    unselectAllNotes: () -> Unit,
    deleteSelectedNotes: () -> Unit,
) {

    AnimatedContent(
        targetState = selectedNotesNumber == 0,
        label = "",
    ) { shouldShowSearchBar ->
        if (shouldShowSearchBar) {
            SearchBar(
                padding = padding,
                offset = offset,
                drawerState = drawerState,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester,
                onReloadDatabase = onReloadDatabase,
                query = query,
                clearQuery = clearQuery,
                search = search,
                noteViewType = noteViewType,
                syncState = syncState,
                consumeOkSyncState = consumeOkSyncState,
                isReadOnlyModeActive = isReadOnlyModeActive,
                updateSettings = updateSettings,
            )
        } else {
            SelectableTopBar(
                padding = padding,
                selectedNotesNumber = selectedNotesNumber,
                unselectAllNotes = unselectAllNotes,
                deleteSelectedNotes = deleteSelectedNotes,
            )
        }
    }
}


@Composable
private fun SearchBar(
    padding: PaddingValues,
    offset: Float,
    drawerState: DrawerState,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
    onReloadDatabase: () -> Unit,
    query: String,
    clearQuery: () -> Unit,
    search: (String) -> Unit,
    noteViewType: NoteViewType,
    syncState: SyncState,
    consumeOkSyncState: () -> Unit,
    isReadOnlyModeActive: Boolean,
    updateSettings: (suspend AppPreferences.() -> Unit) -> Unit,
) {


    val queryTextField = remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }

    val focusManager = LocalFocusManager.current
    fun clearQuery2() {
        queryTextField.value = TextFieldValue("")
        clearQuery()
        focusManager.clearFocus()
    }

    if (query.isNotEmpty()) {
        BackHandler {
            clearQuery2()
        }
    }


    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = 10.dp)
            .padding(top = 15.dp)
            .offset { IntOffset(x = 0, y = offset.roundToInt()) }
            .focusRequester(searchFocusRequester),
        value = queryTextField.value,
        onValueChange = {
            queryTextField.value = it
            search(it.text)
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(100),
        placeholder = {
            Text(text = stringResource(R.string.search_in_notes))
        },
        singleLine = true,
        leadingIcon = {
            val scope = rememberCoroutineScope()

            IconButton(
                onClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            ) {
                SimpleIcon(
                    imageVector = Icons.Rounded.Menu,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                val isEmpty = query.isEmpty()

                if (isEmpty) {
                    SyncStateIcon(
                        state = syncState,
                        onConsumeOkSyncState = consumeOkSyncState
                    )
                }

                IconButton(
                    modifier = Modifier.size(ButtonSize),
                    onClick = {
                        updateSettings {
                            this.noteViewType.update(
                                when (noteViewType) {
                                    NoteViewType.Grid -> NoteViewType.List
                                    NoteViewType.List -> NoteViewType.Grid
                                }
                            )
                        }
                    }
                ) {
                    SimpleIcon(
                        imageVector = if (noteViewType == NoteViewType.Grid) {
                            Icons.AutoMirrored.Rounded.ViewList
                        } else {
                            Icons.Rounded.ViewModule
                        },
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(
                            if (noteViewType == NoteViewType.Grid) {
                                R.string.switch_to_list_view
                            } else {
                                R.string.switch_to_grid_view
                            }
                        )
                    )
                }

                if (isEmpty) {
                    Box {
                        val expanded = remember { mutableStateOf(false) }
                        IconButton(
                            modifier = Modifier.size(ButtonSize),
                            onClick = { expanded.value = true },
                        ) {
                            SimpleIcon(
                                imageVector = Icons.Rounded.MoreVert,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }


                        @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
                        CustomDropDown(
                            expanded = expanded,
                            options = listOf(
                                CustomDropDownModel(
                                    text = stringResource(R.string.settings),
                                    onClick = onSettingsClick
                                ),
                                CustomDropDownModel(
                                    text = if (isReadOnlyModeActive) stringResource(
                                        R.string.read_only_mode_deactive
                                    ) else stringResource(R.string.read_only_mode_activate),
                                    onClick = {
                                        updateSettings {
                                            this.isReadOnlyModeActive.update(!isReadOnlyModeActive)
                                        }
                                    }
                                ),
                                if (BuildConfig.BUILD_TYPE != "release") {
                                    CustomDropDownModel(
                                        text = stringResource(R.string.reload_database),
                                        onClick = onReloadDatabase
                                    )
                                } else null
                            )
                        )
                    }
                }

                if (!isEmpty) {
                    IconButton(
                        onClick = { clearQuery2() }
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Rounded.Close,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )

}

@Composable
private fun SelectableTopBar(
    padding: PaddingValues,
    selectedNotesNumber: Int,
    unselectAllNotes: () -> Unit,
    deleteSelectedNotes: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding())
            .height(topBarHeight - 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current)
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        unselectAllNotes()
                    }
                ) {
                    SimpleIcon(
                        imageVector = Icons.Rounded.Close,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = selectedNotesNumber.toString(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    val expanded = remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            expanded.value = true
                        }
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Rounded.MoreVert,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    CustomDropDown(
                        expanded = expanded,
                        options = listOf(
                            CustomDropDownModel(
                                text = pluralStringResource(
                                    R.plurals.delete_selected_notes,
                                    selectedNotesNumber
                                ),
                                onClick = { deleteSelectedNotes() }
                            )
                        )
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncStateIcon(
    state: SyncState,
    onConsumeOkSyncState: () -> Unit
) {
    var modifier: Modifier = Modifier

    if (state.isLoading()) {

        val infiniteTransition = rememberInfiniteTransition()
        val alpha = infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500),
                repeatMode = RepeatMode.Reverse
            )
        )

        modifier = modifier.alpha(alpha.value)
    }

    val tooltipState = rememberTooltipState(isPersistent = true)
    var visible by remember(state) { if (state is Ok) mutableStateOf(!state.isConsumed) else mutableStateOf(true) }

    if (state is Ok) {
        LaunchedEffect(visible) {
            delay(1000)
            visible = false
            onConsumeOkSyncState()
            tooltipState.dismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        val scope = rememberCoroutineScope()

        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(state.message())
                }
            },
            state = tooltipState
        ) {
            IconButton(
                modifier = Modifier.size(ButtonSize),
                onClick = {
                    scope.launch {
                        if (tooltipState.isVisible) {
                            tooltipState.dismiss()
                        } else {
                            tooltipState.show()
                        }
                    }
                }
            ) {
                when (state) {
                    is SyncState.Error -> Icon(
                        painter = painterResource(R.drawable.cloud_alert_24px),
                        contentDescription = "Sync Error",
                        modifier = modifier
                    )
                    is Ok -> Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Sync Done",
                        modifier = modifier,
                    )
                    Pull -> Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Pulling",
                        modifier = modifier,
                    )
                    Push -> Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Pushing",
                        modifier = modifier,
                    )
                }
            }

        }
    }
}


@Composable
@Preview
private fun TopBarPreview() {
    TopBar(
        padding = PaddingValues(),
        offset = 0f,
        drawerState = rememberDrawerState(DrawerValue.Closed),
        onSettingsClick = {},
        searchFocusRequester = remember { FocusRequester() },
        onReloadDatabase = { },
        query = "",
        clearQuery = { },
        search = {},
        noteViewType = NoteViewType.Grid,
        syncState = SyncState.Error("hello"),
        consumeOkSyncState = {},
        isReadOnlyModeActive = true,
        updateSettings = { },
        selectedNotesNumber = 0,
        unselectAllNotes = { },
        deleteSelectedNotes = {}
    )
}