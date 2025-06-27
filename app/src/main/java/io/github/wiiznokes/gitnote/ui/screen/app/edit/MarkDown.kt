package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.MarkDownVM

@Composable
fun MarkDownContent(
    vm: MarkDownVM,
    textFocusRequester: FocusRequester,
    onFinished: () -> Unit,
    isReadOnlyModeActive: Boolean,
    textContent: TextFieldValue,
) {
    if (isReadOnlyModeActive) {
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
        GenericTextField(
            vm = vm,
            textFocusRequester = textFocusRequester,
            onFinished = onFinished,
            textContent = textContent
        )
    }
}


@Composable
fun TextFormatRow(
    vm: MarkDownVM,
    modifier: Modifier = Modifier,
    textFormatExpanded: MutableState<Boolean>
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .scrollable(rememberScrollState(initial = 0), orientation = Orientation.Horizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SmallButton(
            onClick = { vm.onTitle() },
            imageVector = Icons.Default.Title,
            contentDescription = "title"
        )

        SmallButton(
            onClick = { vm.onBold() },
            imageVector = Icons.Default.FormatBold,
            contentDescription = "bold"
        )
        SmallButton(
            onClick = { vm.onItalic() },
            imageVector = Icons.Default.FormatItalic,
            contentDescription = "italic"
        )

        SmallSeparator()

//        SmallButton(
//            onClick = {},
//            imageVector = Icons.Default.Link,
//            contentDescription = "link"
//        )
//
        SmallButton(
            onClick = { vm.onCode() },
            imageVector = Icons.Default.Code,
            contentDescription = "code"
        )
//        SmallButton(
//            onClick = {},
//            imageVector = Icons.Default.FormatQuote,
//            contentDescription = "quote"
//        )
//
//        SmallSeparator()
//
//        SmallButton(
//            onClick = {},
//            imageVector = Icons.AutoMirrored.Filled.List,
//            contentDescription = "unordered list"
//        )
//        SmallButton(
//            onClick = {},
//            imageVector = Icons.Default.Checklist,
//            contentDescription = "checklist"
//        )
//        SmallButton(
//            onClick = {},
//            imageVector = Icons.Default.FormatListNumbered,
//            contentDescription = "list number"
//        )


        SmallSeparator()

        SmallButton(
            onClick = {
                textFormatExpanded.value = false
            },
            imageVector = Icons.Default.Close,
            contentDescription = "close"
        )
    }
}