package io.github.wiiznokes.gitnote.ui.screen.settings

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewModelScope
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import io.github.wiiznokes.gitnote.BuildConfig
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.component.AppPage
import io.github.wiiznokes.gitnote.ui.component.DefaultSettingsRow
import io.github.wiiznokes.gitnote.ui.component.MultipleChoiceSettings
import io.github.wiiznokes.gitnote.ui.component.PickFolderDialog
import io.github.wiiznokes.gitnote.ui.component.RequestConfirmationDialog
import io.github.wiiznokes.gitnote.ui.component.SettingsSection
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.component.StringSettings
import io.github.wiiznokes.gitnote.ui.component.ToggleableSettings
import io.github.wiiznokes.gitnote.ui.destination.SettingsDestination
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.model.NoteMinWidth
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import io.github.wiiznokes.gitnote.ui.model.TagDisplayMode
import io.github.wiiznokes.gitnote.ui.theme.Theme
import io.github.wiiznokes.gitnote.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
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

            val sortOrder by vm.prefs.sortOrder.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.sort_order),
                subtitle = sortOrder.toString(),
                options = SortOrder.entries,
                onOptionClick = {
                    vm.update { vm.prefs.sortOrder.update(it) }
                }
            )

            val sortOrderFolder by vm.prefs.sortOrderFolder.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.sort_order_folder),
                subtitle = sortOrderFolder.toString(),
                options = SortOrder.entries,
                onOptionClick = {
                    vm.update { vm.prefs.sortOrderFolder.update(it) }
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

            val showFullTitleInListView by vm.prefs.showFullTitleInListView.getAsState()
            ToggleableSettings(
                title = stringResource(R.string.show_full_title_in_list_view),
                subtitle = stringResource(R.string.show_full_title_in_list_view_subtitle),
                checked = showFullTitleInListView,
                onCheckedChange = {
                    vm.update { vm.prefs.showFullTitleInListView.update(it) }
                }
            )

            val tagDisplayMode by vm.prefs.tagDisplayMode.getAsState()
            MultipleChoiceSettings(
                title = stringResource(R.string.tag_display_mode),
                subtitle = tagDisplayMode.toString(),
                options = TagDisplayMode.entries,
                onOptionClick = {
                    vm.update { vm.prefs.tagDisplayMode.update(it) }
                }
            )


            val defaultPathForNewNote by vm.prefs.defaultPathForNewNote.getAsState()
            val pickFolderDialogExpanded = rememberSaveable { mutableStateOf(false) }
            DefaultSettingsRow(
                title = stringResource(R.string.defaultPathForNewNote),
                subTitle = "Only when located in the root folder.\nCurrent value: \"$defaultPathForNewNote\"",
                onClick = { pickFolderDialogExpanded.value = true }
            )

            PickFolderDialog(
                expanded = pickFolderDialogExpanded,
                onSelectedFolder = {
                    vm.update { vm.prefs.defaultPathForNewNote.update(it) }
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

            val gitAuthorName by vm.prefs.gitAuthorName.getAsState()
            StringSettings(
                title = stringResource(R.string.git_author_name),
                subtitle = gitAuthorName.ifEmpty { stringResource(id = R.string.none) },
                stringValue = gitAuthorName,
                onChange = { updated ->
                    vm.update { vm.prefs.gitAuthorName.update(updated.trim()) }
                }
            )

            val gitAuthorEmail by vm.prefs.gitAuthorEmail.getAsState()
            StringSettings(
                title = stringResource(R.string.git_author_email),
                subtitle = gitAuthorEmail.ifEmpty { stringResource(id = R.string.none) },
                stringValue = gitAuthorEmail,
                onChange = { updated ->
                    vm.update { vm.prefs.gitAuthorEmail.update(updated.trim()) }
                },
                keyboardType = KeyboardType.Email
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
                            } catch (_: Exception) {
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
            val version =
                "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}-${
                    BuildConfig.GIT_HASH.substring(
                        0..6
                    )
                }"
            val clipboardManager = LocalClipboard.current

            DefaultSettingsRow(
                title = stringResource(R.string.version),
                subTitle = version,
                onClick = {
                    val data = ClipData(
                        ClipDescription(
                            "version of gitnote",
                            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        ),
                        ClipData.Item(version)
                    )

                    vm.viewModelScope.launch {
                        clipboardManager.setClipEntry(ClipEntry(data))
                    }
                }
            )

            DefaultSettingsRow(
                title = stringResource(R.string.reload_database),
                startIcon = Icons.Default.Refresh,
                onClick = {
                    vm.reloadDatabase()
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
                    uriHandler.openUri("https://github.com/wiiznokes/gitnote/issues")
                }
            )
            DefaultSettingsRow(
                title = stringResource(R.string.source_code),
                startIcon = Icons.Default.Code,
                onClick = {
                    uriHandler.openUri("https://github.com/wiiznokes/gitnote")
                }
            )
        }
    }
}
