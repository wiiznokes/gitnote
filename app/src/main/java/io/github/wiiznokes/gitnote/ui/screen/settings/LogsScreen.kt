package io.github.wiiznokes.gitnote.ui.screen.settings

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.BuildConfig
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.min


private const val TAG = "LogsScreen"


private enum class LogLevel(val logCat: String) {
    ERROR("E"),
    WARNING("W"),
    INFO("I"),
    DEBUG("D"),
}

private val TextSizeRange = 0..2

@Composable
private fun getTextStyleFromInt(id: Int): TextStyle {
    return when {
        id <= 0 -> MaterialTheme.typography.bodySmall
        id == 1 -> MaterialTheme.typography.bodyMedium
        else -> MaterialTheme.typography.bodyLarge
    }
}


@Composable
fun LogsScreen(
    onBackClick: () -> Unit,
    vm: SettingsViewModel
) {

    val logLevel = remember {
        mutableStateOf(LogLevel.ERROR)
    }

    val logState: MutableState<String> = remember {
        mutableStateOf("Loading...")
    }


    SideEffect {
        CoroutineScope(Dispatchers.IO).launch {
            logState.value = getLogs(logLevel.value)
        }
    }

    val textSizeId: MutableState<Int> = remember {
        mutableIntStateOf(1)
    }

    AppPage(
        title = "Logs",
        onBackClick = onBackClick,
        disableVerticalScroll = true,
        actions = {
            IconButton(
                onClick = {
                    textSizeId.value = max(textSizeId.value - 1, TextSizeRange.first)
                }
            ) {
                SimpleIcon(imageVector = Icons.Default.Remove)
            }

            IconButton(
                onClick = {
                    textSizeId.value = min(textSizeId.value + 1, TextSizeRange.last)
                }
            ) {
                SimpleIcon(imageVector = Icons.Default.Add)
            }

            val logLevelExpanded = remember {
                mutableStateOf(false)
            }

            val options: MutableList<CustomDropDownModel> = LogLevel.entries.map {
                CustomDropDownModel(text = it.toString()) {
                    logLevel.value = it
                    CoroutineScope(Dispatchers.IO).launch {
                        logState.value = getLogs(logLevel.value)
                    }
                }
            }.toMutableList()

            val clipboardManager = LocalClipboard.current

            options.add(
                CustomDropDownModel(
                text = stringResource(R.string.copy_all_logs),
                onClick = {
                    val data = ClipData(
                        ClipDescription("logs of gitnote", arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)),
                        ClipData.Item(logState.value)
                    )

                    vm.viewModelScope.launch {
                        clipboardManager.setClipEntry(ClipEntry(data))
                    }

                }
            ))

            CustomDropDown(expanded = logLevelExpanded, options = options)
            IconButton(onClick = { logLevelExpanded.value = true }) {
                SimpleIcon(imageVector = Icons.Default.MoreVert)
            }
        }
    ) {
        SelectionContainer {
            Text(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(5.dp),
                text = logState.value,
                style = getTextStyleFromInt(id = textSizeId.value)
            )
        }
    }
}


private fun getLogs(logLevel: LogLevel): String {
    try {
        Log.d(TAG, "run logcat")
        val process = Runtime.getRuntime()
            .exec("logcat -d --format=time *:${logLevel.logCat} | grep ${BuildConfig.APPLICATION_ID}")

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            val log = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                log.appendLine(line)
                line = reader.readLine()
            }
            return log.toString()
        }

    } catch (e: IOException) {
        return "Error while try to get the logs: ${e.message}"
    }
}
