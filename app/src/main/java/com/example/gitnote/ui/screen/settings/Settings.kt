package com.example.gitnote.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.gitnote.R
import com.example.gitnote.ui.component.BaseDialog
import com.example.gitnote.ui.component.GetStringDialog
import com.example.gitnote.ui.component.SimpleIcon


private val padding = 12.dp


@Composable
fun SettingsSection(
    title: String,
    isLast: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Text(
            modifier = Modifier
                .padding(padding),
            text = title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        content()

        if (!isLast) {
            HorizontalDivider()
        }
    }

}

@Composable
fun DefaultSettingsRow(
    title: String,
    subTitle: String? = null,
    startIcon: ImageVector? = null,
    endContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {


        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (endContent != null) Arrangement.SpaceBetween else Arrangement.End
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                endContent?.invoke()

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    startIcon?.let {
                        SimpleIcon(
                            modifier = Modifier
                                .padding(end = padding),
                            imageVector = it
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(end = padding),
                    ) {
                        Text(
                            modifier = Modifier,
                            text = title,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        subTitle?.let {
                            if (it.isNotEmpty()) {
                                Text(
                                    modifier = Modifier,
                                    text = it,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun <T> MultipleChoiceSettings(
    title: String,
    subtitle: String? = null,
    startIcon: ImageVector? = null,
    endContent: (@Composable () -> Unit)? = null,
    options: List<T>,
    onOptionClick: (T) -> Unit,
) {

    val expanded = remember {
        mutableStateOf(false)
    }

    DefaultSettingsRow(
        title = title,
        subTitle = subtitle,
        startIcon = startIcon,
        endContent = endContent,
        onClick = {
            expanded.value = true
        }
    )

    BaseDialog(
        expanded = expanded,
    ) {
        options.forEach { option ->
            Button(
                modifier = Modifier.fillMaxWidth(0.8f),
                onClick = {
                    expanded.value = false
                    onOptionClick(option)
                }
            ) {
                Text(text = option.toString())
            }
        }
    }
}


@Composable
fun ToggleableSettings(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {

    DefaultSettingsRow(
        title = title,
        subTitle = subtitle,
        startIcon = icon,
        endContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun StringSettings(
    title: String,
    subtitle: String? = null,
    startIcon: ImageVector? = null,
    endContent: (@Composable () -> Unit)? = null,
    stringValue: String,
    onChange: (String) -> Unit,
) {

    val expanded = remember {
        mutableStateOf(false)
    }

    DefaultSettingsRow(
        title = title,
        subTitle = subtitle,
        startIcon = startIcon,
        endContent = endContent,
        onClick = {
            expanded.value = true
        }
    )

    GetStringDialog(
        expanded = expanded,
        label = title,
        actionText = stringResource(id = R.string.save),
        defaultString = stringValue,
        onValidation = onChange
    )

}
