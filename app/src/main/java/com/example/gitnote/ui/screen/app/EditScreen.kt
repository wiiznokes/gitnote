package com.example.gitnote.ui.screen.app

import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    editType: EditType,
    initialNote: Note,
    onFinished: () -> Unit,
) {

    Log.d(TAG, "init: $initialNote, $editType")
    val vm = viewModel<EditViewModel>(
        factory = viewModelFactory { EditViewModel() }
    )

    val name = remember {
        val initialName = initialNote.nameWithoutExtension()
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(initialName.length)))
    }
    var content by remember {
        mutableStateOf(
            TextFieldValue(
                initialNote.content,
                selection = TextRange(initialNote.content.length)
            )
        )
    }

    val fileExtension = remember {
        mutableStateOf(initialNote.fileExtension())
    }


    val nameFocusRequester = remember { FocusRequester() }
    val textFocusRequester = remember { FocusRequester() }

    LaunchedEffect(null) {
        when (editType) {
            EditType.Create -> nameFocusRequester.requestFocus()
            EditType.Update -> textFocusRequester.requestFocus()
        }
    }

    val onValidation = {
        when (editType) {
            EditType.Create -> vm.create(
                parentPath = initialNote.parentPath(),
                name = name.value.text,
                fileExtension = fileExtension.value,
                content = content.text,
                id = initialNote.id
            ).onSuccess { onFinished() }

            EditType.Update -> vm.update(
                previousNote = initialNote,
                parentPath = initialNote.parentPath(),
                name = name.value.text,
                fileExtension = fileExtension.value,
                content = content.text,
                id = initialNote.id
            ).onSuccess { onFinished() }
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
                        value = name.value,
                        onValueChange = {
                            name.value = it
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
                    ExtensionChooser(fileExtension)
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            onValidation()
                        }
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Default.Done,
                        )
                    }
                }
            )
        },
        floatingActionButton = {

            // bug: https://issuetracker.google.com/issues/224005027
            //AnimatedVisibility(visible = currentNoteFolderRelativePath.isNotEmpty()) {
            if (name.value.text.isNotEmpty() && content.text.isNotEmpty()) {
                FloatingActionButton(
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        onValidation()
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
            value = content,
            onValueChange = {
                content = it
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
                    onValidation()
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