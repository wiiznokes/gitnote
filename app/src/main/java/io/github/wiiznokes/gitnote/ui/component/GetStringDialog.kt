
package io.github.wiiznokes.gitnote.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.ui.theme.LocalSpaces

@Composable
fun GetStringDialog(
    expanded: MutableState<Boolean>,
    label: String,
    actionText: String,
    defaultString: String = "",
    singleLine: Boolean = true,
    unExpandedOnValidation: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValidation: (String) -> Unit,
) {
    BaseDialog(expanded = expanded) {
        val focusRequester = remember { FocusRequester() }

        val textFieldState = rememberTextFieldState(
            initialText = defaultString
        )

        var passwordVisible by rememberSaveable {
            mutableStateOf(false)
        }

        fun validate() {
            onValidation(textFieldState.text.toString())

            if (unExpandedOnValidation) {
                expanded.value = false
            }
        }

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        val isPassword = keyboardType == KeyboardType.Password

        val keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
        )

        if (isPassword) {
            OutlinedSecureTextField(
                state = textFieldState,
                modifier = Modifier.focusRequester(focusRequester),
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                keyboardOptions = keyboardOptions,
                textObfuscationMode = if (passwordVisible) {
                    TextObfuscationMode.Visible
                } else {
                    TextObfuscationMode.Hidden
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = if (passwordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            },
                        )
                    }
                },
                onKeyboardAction = {
                    validate()
                },
            )
        } else {
            OutlinedTextField(
                state = textFieldState,
                modifier = Modifier.focusRequester(focusRequester),
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                lineLimits = if (singleLine) {
                    androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
                } else {
                    androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine()
                },
                keyboardOptions = keyboardOptions,
                onKeyboardAction = {
                    validate()
                },
            )
        }

        Spacer(
            modifier = Modifier.height(
                LocalSpaces.current.dialogSeparation
            )
        )

        SimpleButton(
            modifier = Modifier.widthIn(min = 150.dp),
            text = actionText,
        ) {
            validate()
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    GetStringDialog(
        expanded = remember { mutableStateOf(true) },
        label = "label",
        actionText = "action",
        defaultString = "hahaha",
        keyboardType = KeyboardType.Password,
    ) {}
}
