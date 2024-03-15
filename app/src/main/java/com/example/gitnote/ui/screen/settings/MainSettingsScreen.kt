package com.example.gitnote.ui.screen.settings

import android.content.ActivityNotFoundException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import com.example.gitnote.BuildConfig
import com.example.gitnote.R
import com.example.gitnote.ui.component.AppPage
import com.example.gitnote.ui.component.RequestConfirmationDialog
import com.example.gitnote.ui.component.SimpleIcon
import com.example.gitnote.ui.destination.SettingsDestination
import com.example.gitnote.ui.model.NoteMinWidth
import com.example.gitnote.ui.model.FileExtension
import com.example.gitnote.ui.model.SortOrder
import com.example.gitnote.ui.model.SortType
import com.example.gitnote.ui.theme.Theme
import com.example.gitnote.ui.viewmodel.SettingsViewModel
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate

@Composable
fun MainSettingsScreen(
    onBackClick: () -> Unit,
    navController: NavController<SettingsDestination>,
    onCloseRepo: () -> Unit,
    vm: SettingsViewModel
) {

    AppPage(
        title = "Settings",
        onBackClick = onBackClick,
    ) {


        SettingsSection(
            title = "User interface"
        ) {

            val theme by vm.prefs.theme.getAsState()
            MultipleChoiceSettings(
                title = "Theme",
                subtitle = theme.toString(),
                startIcon = Icons.Default.Palette,
                options = Theme.entries,
                onOptionClick = {
                    vm.update { vm.prefs.theme.update(it) }
                }
            )


            val dynamicColor by vm.prefs.dynamicColor.getAsState()
            ToggleableSettings(
                title = "Dynamic color",
                checked = dynamicColor,
                onCheckedChange = {
                    vm.update { vm.prefs.dynamicColor.update(it) }
                }
            )

        }

        SettingsSection(
            title = "Grid"
        ) {

            val sortType by vm.prefs.sortType.getAsState()
            MultipleChoiceSettings(
                title = "Sort type",
                subtitle = sortType.toString(),
                options = SortType.entries,
                onOptionClick = {
                    vm.update { vm.prefs.sortType.update(it) }
                }
            )

            val sortOrder by vm.prefs.sortOrder.getAsState()
            MultipleChoiceSettings(
                title = "Sort Order",
                subtitle = sortOrder.toString(),
                options = SortOrder.entries,
                onOptionClick = {
                    vm.update { vm.prefs.sortOrder.update(it) }
                }
            )

            val noteMinWidth by vm.prefs.noteMinWidth.getAsState()
            MultipleChoiceSettings(
                title = "Minimal width of a note",
                subtitle = noteMinWidth.toString(),
                options = NoteMinWidth.entries,
                onOptionClick = {
                    vm.update { vm.prefs.noteMinWidth.update(it) }
                }
            )

            val showFullNoteHeight by vm.prefs.showFullNoteHeight.getAsState()
            ToggleableSettings(
                title = "Show long notes entirely",
                checked = showFullNoteHeight,
                onCheckedChange = {
                    vm.update { vm.prefs.showFullNoteHeight.update(it) }
                }
            )

            val rememberLastOpenedFolder by vm.prefs.rememberLastOpenedFolder.getAsState()
            ToggleableSettings(
                title = "Remember last opened folder",
                checked = rememberLastOpenedFolder,
                onCheckedChange = {
                    vm.update { vm.prefs.rememberLastOpenedFolder.update(it) }
                }
            )

            val showFullPathOfNotes by vm.prefs.showFullPathOfNotes.getAsState()
            ToggleableSettings(
                title = "Always show the full path of notes",
                subtitle = "Note that the default behavior will only print the path if more than two notes share the same name",
                checked = showFullPathOfNotes,
                onCheckedChange = {
                    vm.update { vm.prefs.showFullPathOfNotes.update(it) }
                }
            )

            DefaultSettingsRow(
                title = "Folder filters",
                subTitle = "Define regex to filter folders of the repository"
            ) {
                navController.navigate(SettingsDestination.FolderFilters)
            }
        }

        SettingsSection(
            title = "Edit"
        ) {
            val defaultExtension by vm.prefs.defaultExtension.getAsState()
            MultipleChoiceSettings(
                title = "Default extension for notes",
                subtitle = defaultExtension,
                options = FileExtension.entries,
                onOptionClick = {
                    vm.update { vm.prefs.defaultExtension.update(it.text) }
                }
            )

            val showLinesNumber by vm.prefs.showLinesNumber.getAsState()
            ToggleableSettings(
                title = "Show lines number",
                checked = showLinesNumber
            ) {
                vm.update { vm.prefs.showLinesNumber.update(it) }
            }
        }

        SettingsSection(
            title = "Repository"
        ) {
            val userName by vm.prefs.userName.getAsState()
            StringSettings(
                title = "Username",
                subtitle = userName.ifEmpty { stringResource(id = R.string.none) },
                stringValue = userName,
                onChange = {
                    vm.update { vm.prefs.userName.update(it) }
                },
                showFullText = false,
            )

            val password by vm.prefs.password.getAsState()

            StringSettings(
                title = "Password",
                subtitle = password.let {
                    if (it.isEmpty())
                        stringResource(id = R.string.none)
                    else
                        "*".repeat(it.length)
                },
                stringValue = password,
                onChange = {
                    vm.update { vm.prefs.password.update(it) }
                },
                showFullText = false,
                keyboardType = KeyboardType.Password
            )

            val remoteUrl by vm.prefs.remoteUrl.getAsState()
            StringSettings(
                title = "Remote url",
                subtitle = remoteUrl.ifEmpty { stringResource(id = R.string.none) },
                stringValue = remoteUrl,
                onChange = {
                    vm.update { vm.prefs.remoteUrl.update(it) }
                },
                endContent = {
                    val uriHandler = LocalUriHandler.current
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(remoteUrl)
                            } catch (e: ActivityNotFoundException) {
                                vm.uiHelper.makeToast("Invalid link, can't open it")
                            }
                        }
                    ) {
                        SimpleIcon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        )

                    }
                },
                showFullText = false,
                keyboardType = KeyboardType.Uri
            )

            val expanded = rememberSaveable {
                mutableStateOf(false)
            }

            DefaultSettingsRow(
                title = "Close repository",
                startIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = {
                    expanded.value = true
                }
            )

            RequestConfirmationDialog(
                expanded = expanded,
                text = "Do you really want to close the repo?",
                onConfirmation = {
                    vm.closeRepo()
                    onCloseRepo()
                }
            )
        }

        SettingsSection(
            title = "About",
            isLast = true
        ) {
            var version = BuildConfig.VERSION_NAME
            version += if (BuildConfig.DEBUG) "-debug" else "-release"
            val clipboardManager = LocalClipboardManager.current

            DefaultSettingsRow(
                title = "Version",
                subTitle = version,
                onClick = {
                    clipboardManager.setText(AnnotatedString(version))
                }
            )

            DefaultSettingsRow(
                title = "Show logs",
                startIcon = Icons.AutoMirrored.Filled.Article,
                onClick = {
                    navController.navigate(SettingsDestination.Logs)
                }
            )

            val uriHandler = LocalUriHandler.current
            DefaultSettingsRow(
                title = "Report an issue",
                startIcon = Icons.Default.BugReport,
                onClick = {
                    uriHandler.openUri("https://github.com/wiiznokes/gitnote/issues/new/choose")
                }
            )
            DefaultSettingsRow(
                title = "Source code",
                startIcon = Icons.Default.Code,
                onClick = {
                    uriHandler.openUri("https://github.com/wiiznokes/gitnote")
                }
            )

            DefaultSettingsRow(
                title = "Libraries",
                startIcon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = {
                    navController.navigate(SettingsDestination.Libraries)
                }
            )
        }
    }
}

