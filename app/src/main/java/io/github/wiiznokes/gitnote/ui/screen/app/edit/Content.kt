package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.viewmodel.EditViewModel


@Composable
fun Content(
    vm: EditViewModel,
    textFocusRequester: FocusRequester,
    onFinished: () -> Unit,
    isReadOnlyModeActive: Boolean,
) {

    val textContent = vm.content.collectAsState().value

    if (isReadOnlyModeActive && vm.fileExtension.value is FileExtension.Md) {
        SelectionContainer {
            RichText(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
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
                Markdown(textContent.text)
            }
        }
    } else {
        TextField(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(textFocusRequester),
            value = textContent,
            onValueChange = { vm.onValueChange(it) },
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