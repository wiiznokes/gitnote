package io.github.wiiznokes.gitnote.ui.screen.app.grid

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DrawerState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.viewmodel.GridViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


private const val TAG = "TopGridScreen"

@Composable
fun TopBar(
    vm: GridViewModel,
    padding: PaddingValues,
    offset: Float,
    selectedNotesNumber: Int,
    drawerState: DrawerState,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester,
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
                vm = vm,
                onSettingsClick = onSettingsClick,
                searchFocusRequester = searchFocusRequester
            )
        } else {
            SelectableTopBar(
                padding = padding,
                vm = vm,
                selectedNotesNumber = selectedNotesNumber
            )
        }
    }
}


@Composable
private fun SearchBar(
    padding: PaddingValues,
    offset: Float,
    drawerState: DrawerState,
    vm: GridViewModel,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester
) {


    var queryTextField by remember {
        Log.d(TAG, "")
        mutableStateOf(
            TextFieldValue(
                text = vm.query.value,
                selection = TextRange(vm.query.value.length)
            )
        )
    }

    val focusManager = LocalFocusManager.current
    fun clearQuery() {
        queryTextField = TextFieldValue("")
        vm.clearQuery()
        focusManager.clearFocus()
    }

    val query = vm.query.collectAsState()
    if (query.value.isNotEmpty()) {
        BackHandler {
            clearQuery()
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
        value = queryTextField,
        onValueChange = {
            queryTextField = it
            vm.search(it.text)
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
        trailingIcon = if (queryTextField.text.isEmpty()) {
            {
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

                    val readOnlyMode = vm.prefs.isReadOnlyModeActive.getAsState().value

                    CustomDropDown(
                        expanded = expanded,
                        options = listOf(
                            CustomDropDownModel(
                                text = stringResource(R.string.settings),
                                onClick = onSettingsClick
                            ),
                            CustomDropDownModel(
                                text = if (readOnlyMode) stringResource(
                                    R.string.read_only_mode_deactive
                                ) else stringResource(R.string.read_only_mode_activate),
                                onClick = {
                                    vm.viewModelScope.launch {
                                        vm.prefs.isReadOnlyModeActive.update(!readOnlyMode)
                                    }
                                }
                            ),
                        )
                    )
                }
            }
        } else {
            {
                IconButton(
                    onClick = {
                        clearQuery()
                    }
                ) {
                    SimpleIcon(
                        imageVector = Icons.Rounded.Close,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )

}

@Composable
private fun SelectableTopBar(
    padding: PaddingValues,
    vm: GridViewModel,
    selectedNotesNumber: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .height(topBarHeight)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    vm.unselectAllNotes()
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
                            onClick = { vm.deleteSelectedNotes() }
                        )
                    )
                )
            }
        }
    }
}
