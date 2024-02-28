package com.example.gitnote.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gitnote.ui.theme.LocalSpaces


private const val TAG = "GetStringDialog"

@Composable
fun GetStringDialog(
    expanded: MutableState<Boolean>,
    label: String,
    actionText: String,
    defaultString: String = "",
    singleLine: Boolean = true,
    unExpandedOnValidation: Boolean = true,
    onValidation: (String) -> Unit,
) {

    BaseDialog(expanded = expanded) {

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(null) {
            focusRequester.requestFocus()
        }

        var name by remember(expanded) {
            mutableStateOf(
                TextFieldValue(
                    defaultString,
                    selection = TextRange(defaultString.length)
                )
            )
        }



        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester),
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = singleLine,
            keyboardActions = KeyboardActions(
                onDone = {
                    onValidation(name.text)
                    if (unExpandedOnValidation) {
                        expanded.value = false
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(LocalSpaces.current.dialogSeparation))

        SimpleButton(
            modifier = Modifier
                .widthIn(min = 150.dp),
            text = actionText
        ) {
            onValidation(name.text)
            if (unExpandedOnValidation) {
                expanded.value = false
            }
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    GetStringDialog(
        expanded = remember {
            mutableStateOf(true)
        },
        label = "label",
        actionText = "action",
        defaultString = "hahaha"
    ) {
    }
}