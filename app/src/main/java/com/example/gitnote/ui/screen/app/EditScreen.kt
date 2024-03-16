package com.example.gitnote.ui.screen.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gitnote.data.room.Note
import com.example.gitnote.ui.component.CustomDropDown
import com.example.gitnote.ui.component.CustomDropDownModel
import com.example.gitnote.ui.component.SimpleIcon
import com.example.gitnote.ui.model.EditType
import com.example.gitnote.ui.model.FileExtension
import com.example.gitnote.ui.viewmodel.EditViewModel
import com.example.gitnote.ui.viewmodel.viewModelFactory


private const val TAG = "EditScreen"


// todo: maybe use fragment to reuse the note of gridview
//  like in google keep, this will potentially allow a nice transition
//  from a note of the grid to edit
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    initialEditType: EditType,
    initialNote: Note,
    onFinished: () -> Unit,
) {


    val vm = viewModel<EditViewModel>(
        factory = viewModelFactory {
            EditViewModel(initialEditType, initialNote)
        }
    )


    val nameFocusRequester = remember { FocusRequester() }
    val textFocusRequester = remember { FocusRequester() }

    // tricks to request focus only one time
    var lastId: Boolean by rememberSaveable { mutableStateOf(false) }
    if (!lastId) {
        lastId = true
        LaunchedEffect(null) {
            if (vm.editType == EditType.Create) {
                nameFocusRequester.requestFocus()
            }
        }
    }

    Scaffold(
        contentColor = MaterialTheme.colorScheme.background,
        topBar = {

            val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(15.dp)

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onFinished,
                    ) {
                        SimpleIcon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        )
                    }
                },
                title = {

                    TextField(
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester),
                        value = vm.name.value,
                        onValueChange = {
                            vm.name.value = it
                        },
                        singleLine = true,
                        placeholder = {
                            Text(text = "Note name")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                            focusedContainerColor = backgroundColor,
                            unfocusedContainerColor = backgroundColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                textFocusRequester.requestFocus()
                            }
                        )
                    )
                },
                actions = {
                    ExtensionChooser(vm.fileExtension)
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            vm.onValidation(onSuccess = null)
                        }
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Default.Save,
                        )
                    }
                }
            )
        },
        floatingActionButton = {

            // bug: https://issuetracker.google.com/issues/224005027
            //AnimatedVisibility(visible = currentNoteFolderRelativePath.isNotEmpty()) {
            if (vm.name.value.text.isNotEmpty() && vm.content.value.text.isNotEmpty()) {
                FloatingActionButton(
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        vm.onValidation(onSuccess = onFinished)
                    }
                ) {
                    SimpleIcon(
                        imageVector = Icons.Default.Done,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        TextField(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .focusRequester(textFocusRequester),
            value = vm.content.value,
            onValueChange = {
                vm.content.value = it
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    vm.onValidation(onSuccess = onFinished)
                }
            )
        )

    }
}


@Composable
private fun ExtensionChooser(
    fileExtension: MutableState<FileExtension>
) {
    Box {
        val expanded = remember { mutableStateOf(false) }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            onClick = {
                expanded.value = true
            }
        ) {
            Text(
                text = '.' + fileExtension.value.text,
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis
            )
        }

        CustomDropDown(
            expanded = expanded,
            options = FileExtension.entries.map {
                CustomDropDownModel(
                    text = ".${it.text}",
                    onClick = {
                        fileExtension.value = it
                    }
                )
            }
        )
    }
}