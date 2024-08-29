package io.github.wiiznokes.gitnote.ui.screen.settings

import android.content.ActivityNotFoundException
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import io.github.wiiznokes.gitnote.BuildConfig
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.RequestConfirmationDialog
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.destination.SettingsDestination
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.model.NoteMinWidth
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.ui.model.SortType
import io.github.wiiznokes.gitnote.ui.theme.Theme
import io.github.wiiznokes.gitnote.ui.viewmodel.SettingsViewModel

@Composable
fun MainSettingsScreen(
    onBackClick: () -> Unit,
    navController: NavController<SettingsDestination>,
    onCloseRepo: () -> Unit,
    vm: SettingsViewModel
) {

    AppPage(
        title = stringResource(id = R.string.settings),
        onBackClick = onBackClick,
    ) {


        SettingsSection(
            title = stringResource(R.string.user_interface)
        ) {

            val theme by vm.prefs.theme.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.theme),
                subtitle = theme.toString(),
                startIcon = Icons.Default.Palette,
                options = Theme.entries,
                onOptionClick = {
                    vm.update { vm.prefs.theme.update(it) }
                }
            )


            val dynamicColor by vm.prefs.dynamicColor.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.dynamic_colors),
                checked = dynamicColor,
                onCheckedChange = {
                    vm.update { vm.prefs.dynamicColor.update(it) }
                }
            )

        }

        SettingsSection(
            title = stringResource(R.string.grid)
        ) {

            val sortType by vm.prefs.sortType.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.sort_type),
                subtitle = sortType.toString(),
                options = SortType.entries,
                onOptionClick = {
                    vm.update { vm.prefs.sortType.update(it) }
                }
            )

            val sortOrder by vm.prefs.sortOrder.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.sort_order),
                subtitle = sortOrder.toString(),
                options = SortOrder.entries,
                onOptionClick = {
                    vm.update { vm.prefs.sortOrder.update(it) }
                }
            )

            val noteMinWidth by vm.prefs.noteMinWidth.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.minimal_note_width),
                subtitle = noteMinWidth.toString(),
                options = NoteMinWidth.entries,
                onOptionClick = {
                    vm.update { vm.prefs.noteMinWidth.update(it) }
                }
            )

            val showFullNoteHeight by vm.prefs.showFullNoteHeight.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.show_long_notes),
                checked = showFullNoteHeight,
                onCheckedChange = {
                    vm.update { vm.prefs.showFullNoteHeight.update(it) }
                }
            )

            val rememberLastOpenedFolder by vm.prefs.rememberLastOpenedFolder.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.remember_last_opened_folder),
                checked = rememberLastOpenedFolder,
                onCheckedChange = {
                    vm.update { vm.prefs.rememberLastOpenedFolder.update(it) }
                }
            )

            val showFullPathOfNotes by vm.prefs.showFullPathOfNotes.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.show_the_full_notes_path),
                subtitle = stringResource(R.string.show_the_full_notes_path_subtitle),
                checked = showFullPathOfNotes,
                onCheckedChange = {
                    vm.update { vm.prefs.showFullPathOfNotes.update(it) }
                }
            )

            /*
            DefaultSettingsRow(
                title = stringResource(R.string.folder_filters),
                subTitle = stringResource(R.string.folder_filters_subtitle)
            ) {
                navController.navigate(SettingsDestination.FolderFilters)
            }
             */
        }

        SettingsSection(
            title = stringResource(R.string.edit)
        ) {
            val defaultExtension by vm.prefs.defaultExtension.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.default_note_extension),
                subtitle = defaultExtension,
                options = FileExtension.entries,
                onOptionClick = {
                    vm.update { vm.prefs.defaultExtension.update(it.text) }
                }
            )

            /*
            val showLinesNumber by vm.prefs.showLinesNumber.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.show_lines_number),
                checked = showLinesNumber
            ) {
                vm.update { vm.prefs.showLinesNumber.update(it) }
            }
             */
        }

        SettingsSection(
            title = stringResource(R.string.repository)
        ) {
            val userName by vm.prefs.userName.getAsState()
            StringSettings(
                title = stringResource(R.string.username),
                subtitle = userName.ifEmpty { stringResource(id = R.string.none) },
                stringValue = userName,
                onChange = {
                    vm.update { vm.prefs.userName.update(it) }
                },
                showFullText = false,
            )

            val password by vm.prefs.password.getAsState()

            StringSettings(
                title = stringResource(R.string.password),
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
                title = stringResource(R.string.remote_url),
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
                                vm.uiHelper.makeToast(vm.uiHelper.getString(R.string.error_invalid_link))
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
                title = stringResource(R.string.close_repository),
                startIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = {
                    expanded.value = true
                }
            )

            RequestConfirmationDialog(
                expanded = expanded,
                text = stringResource(R.string.close_repository_confirmation),
                onConfirmation = {
                    vm.closeRepo()
                    onCloseRepo()
                }
            )
        }

        SettingsSection(
            title = stringResource(R.string.about),
            isLast = true
        ) {
            var version = BuildConfig.VERSION_NAME
            version += "-${BuildConfig.GIT_HASH.substring(0..6)}"
            version += if (BuildConfig.DEBUG) "-debug" else "-release"
            val clipboardManager = LocalClipboardManager.current

            DefaultSettingsRow(
                title = stringResource(R.string.version),
                subTitle = version,
                onClick = {
                    clipboardManager.setText(AnnotatedString(version))
                }
            )

            DefaultSettingsRow(
                title = stringResource(R.string.show_logs),
                startIcon = Icons.AutoMirrored.Filled.Article,
                onClick = {
                    navController.navigate(SettingsDestination.Logs)
                }
            )

            val uriHandler = LocalUriHandler.current
            DefaultSettingsRow(
                title = stringResource(R.string.report_an_issue),
                startIcon = Icons.Default.BugReport,
                onClick = {
                    uriHandler.openUri("https://github.com/wiiznokes/gitnote/issues/new/choose")
                }
            )
            DefaultSettingsRow(
                title = stringResource(R.string.source_code),
                startIcon = Icons.Default.Code,
                onClick = {
                    uriHandler.openUri("https://github.com/wiiznokes/gitnote")
                }
            )

            DefaultSettingsRow(
                title = stringResource(R.string.libraries),
                startIcon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = {
                    navController.navigate(SettingsDestination.Libraries)
                }
            )
        }
    }
}

