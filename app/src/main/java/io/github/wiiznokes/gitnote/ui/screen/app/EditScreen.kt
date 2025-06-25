package io.github.wiiznokes.gitnote.ui.screen.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.destination.EditParams
import io.github.wiiznokes.gitnote.ui.model.EditType
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.viewmodel.newEditViewModel


private const val TAG = "EditScreen"

/**
 * initialEditType and initialNote equal null
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    editParams: EditParams,
    onFinished: () -> Unit,
) {


    val vm = newEditViewModel(editParams)

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

    val isReadOnlyModeActive =
        !vm.shouldForceNotReadOnlyMode.value && vm.prefs.isReadOnlyModeActive.getAsState().value

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(15.dp)

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            vm.shouldSaveWhenQuitting = false
                            onFinished()
                        },
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
                        readOnly = isReadOnlyModeActive,
                        singleLine = true,
                        placeholder = {
                            Text(text = stringResource(R.string.note_name))
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
                    ExtensionChooser(vm.fileExtension, isReadOnlyModeActive)
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            vm.save()
                        },
                        enabled = !isReadOnlyModeActive
                    ) {
                        SimpleIcon(
                            imageVector = Icons.Default.Save,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            vm.setReadOnlyMode(!isReadOnlyModeActive)
                        },
                    ) {
                        SimpleIcon(
                            imageVector = if (isReadOnlyModeActive) {
                                Icons.Default.Lock
                            } else {
                                Icons.Default.LockOpen
                            },
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // bug: https://issuetracker.google.com/issues/224005027
            //AnimatedVisibility(visible = currentNoteFolderRelativePath.isNotEmpty()) {
            if (!isReadOnlyModeActive && vm.name.value.text.isNotEmpty()) {
                FloatingActionButton(
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        vm.save(onSuccess = onFinished)
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

        if (isReadOnlyModeActive && vm.fileExtension.value is FileExtension.Md) {
            SelectionContainer {
                RichText(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(15.dp),
                    style = RichTextStyle(
                        stringStyle = RichTextStringStyle(
                            linkStyle = TextLinkStyles(
                                style = SpanStyle(
                                    color = if (isSystemInDarkTheme()) Color(0xFF3268ae) else
                                        Color(0xFF5a9ae6)
                                )
                            )
                        )
                    )
                ) {
                    Markdown(vm.content.value.text)
                }
            }
        } else {
            TextField(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .focusRequester(textFocusRequester),
                value = vm.content.value,
                onValueChange = {
                    if (vm.fileExtension.value is FileExtension.Md) {
                        vm.content.value = markdownSmartEditor(vm.content.value, it)
                    } else {
                        vm.content.value = it
                    }
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
                        vm.save(onSuccess = onFinished)
                    }
                ),
                readOnly = isReadOnlyModeActive
            )
        }
    }
}


@Composable
private fun ExtensionChooser(
    fileExtension: MutableState<FileExtension>,
    isReadOnly: Boolean,
) {
    Box {
        val expanded = remember { mutableStateOf(false) }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            onClick = { expanded.value = true },
            enabled = !isReadOnly
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

private fun markdownSmartEditor(
    prev: TextFieldValue,
    v: TextFieldValue
): TextFieldValue {
    if (v.selection.start == v.selection.end) {
        val cursorPos = v.selection.start
        if (cursorPos > 0 && cursorPos <= v.text.length) {
            if (v.text[cursorPos - 1] == '\n') {

                val lineBefore = v.text.substring(0, cursorPos - 1).lastIndexOf('\n').let {
                    if (it == -1) 0 else it + 1
                }.let {
                    v.text.substring(it, cursorPos - 1)
                }

                val currentLine = v.text.indexOf('\n', startIndex = cursorPos).let {
                    if (it == -1) v.text.length else it
                }.let {
                    v.text.substring(cursorPos, it)
                }

                if (currentLine.isNotEmpty()) {
                    return v
                }

                // handle delete key when the line is:
                // - x
                //
                if (prev.text.length >= v.text.length) {
                    return v
                }

                // remove
                if (lineBefore == "- " || lineBefore == "* ") {
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos - 3) + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos - 3)
                    )
                }

                // add a new line
                if (lineBefore.startsWith("- ")) {
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos) + "- " + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + 2)
                    )
                }

                if (lineBefore.startsWith("* ")) {
                    return TextFieldValue(
                        text = v.text.substring(0, cursorPos) + "* " + v.text.substring(
                            cursorPos,
                            v.text.length
                        ),
                        selection = TextRange(cursorPos + 2)
                    )
                }
            }
        }
    }
    return v
}