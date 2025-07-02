package io.github.wiiznokes.gitnote.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.model.SortOrder.Ascending
import io.github.wiiznokes.gitnote.ui.model.SortOrder.Descending
import io.github.wiiznokes.gitnote.ui.model.SortType.AlphaNumeric
import io.github.wiiznokes.gitnote.ui.model.SortType.Modification
import io.github.wiiznokes.gitnote.ui.screen.app.DrawerFolderModel
import io.github.wiiznokes.gitnote.ui.util.fuzzySort
import io.github.wiiznokes.gitnote.ui.util.mapAndCombine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

class GridViewModel : ViewModel() {

    companion object {
        private const val TAG = "GridViewModel"
    }


    private val storageManager: StorageManager = MyApp.appModule.storageManager

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase
    private val dao = db.repoDatabaseDao
    val uiHelper = MyApp.appModule.uiHelper

    private val _query = MutableStateFlow("")
    val query = _query

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    private val _currentNoteFolderRelativePath = MutableStateFlow(
        if (prefs.rememberLastOpenedFolder.getBlocking()) {
            prefs.lastOpenedFolder.getBlocking()
        } else {
            ""
        }
    )
    val currentNoteFolderRelativePath: StateFlow<String>
        get() = _currentNoteFolderRelativePath.asStateFlow()


    private val _selectedNotes: MutableStateFlow<List<Note>> = MutableStateFlow(emptyList())

    val selectedNotes: StateFlow<List<Note>>
        get() = _selectedNotes.asStateFlow()


    private val allNotes = dao.allNotes()


    init {
        Log.d(TAG, "init")

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.updateDatabaseAndRepo()
        }

        CoroutineScope(Dispatchers.IO).launch {
            allNotes.collect { allNotes ->
                selectedNotes.value.filter { selectedNote ->
                    allNotes.contains(selectedNote)
                }.let { newSelectedNotes ->
                    _selectedNotes.emit(newSelectedNotes)
                }
            }
        }
    }

    fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            _isRefreshing.emit(true)
            storageManager.updateDatabaseAndRepo()
            _isRefreshing.emit(false)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _query.emit(query)
        }
    }

    fun clearQuery() {
        viewModelScope.launch {
            _query.emit("")
        }
    }

    fun openFolder(relativePath: String) {
        viewModelScope.launch {
            _currentNoteFolderRelativePath.emit(relativePath)
            prefs.lastOpenedFolder.update(relativePath)
        }
    }

    fun createNoteFolder(relativeParentPath: String, name: String): Boolean {
        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_invalid_name))
            return false
        }

        val relativePath = "$relativeParentPath/$name"

        val noteFolder = NoteFolder.new(
            relativePath = relativePath
        )

        if (noteFolder.toFolderFs(prefs.repoPathBlocking()).exist()) {
            uiHelper.makeToast(uiHelper.getString(R.string.error_folder_already_exist))
            return false
        }

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.createNoteFolder(noteFolder)
        }

        return true
    }


    /**
     * @param add true if the note must be selected, false otherwise
     */
    fun selectNote(note: Note, add: Boolean) = viewModelScope.launch {
        if (add) {
            selectedNotes.value.plus(note)
        } else {
            selectedNotes.value.minus(note)
        }.let {
            _selectedNotes.emit(it)
        }
    }

    fun unselectAllNotes() = viewModelScope.launch {
        _selectedNotes.emit(emptyList())
    }

    fun deleteSelectedNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentSelectedNotes = selectedNotes.value
            unselectAllNotes()
            storageManager.deleteNotes(currentSelectedNotes)
            uiHelper.makeToast(
                uiHelper.getQuantityString(
                    R.plurals.success_notes_delete, currentSelectedNotes.size
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        selectNote(note, false)
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.deleteNote(note)
            uiHelper.makeToast(uiHelper.getQuantityString(R.plurals.success_notes_delete, 1))
        }
    }

    fun deleteFolder(noteFolder: NoteFolder) {
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.deleteNoteFolder(noteFolder)
            uiHelper.makeToast(uiHelper.getQuantityString(R.plurals.success_noteFolders_delete, 1))
        }
    }


    fun defaultNewNote(): Note {

        val defaultName = query.value.let {
            if (NameValidation.check(it)) {
                it
            } else ""
        }

        val defaultExtension = FileExtension.match(prefs.defaultExtension.getBlocking())
        val defaultFullName = "$defaultName.${defaultExtension.text}"

        return Note.new(
            relativePath = "${currentNoteFolderRelativePath.value}/$defaultFullName",
        )
    }


    private val notes = allNotes.combine(currentNoteFolderRelativePath) { allNotes, path ->
        if (path.isEmpty()) {
            allNotes
        } else {
            allNotes.filter {
                it.relativePath.startsWith("$path/")
            }
        }
    }.let { filteredNotesFlow ->
        combine(
            filteredNotesFlow, prefs.sortType.getFlow(), prefs.sortOrder.getFlow()
        ) { filteredNotes, sortType, sortOrder ->

            when (sortType) {
                Modification -> when (sortOrder) {
                    Ascending -> filteredNotes.sortedByDescending { it.lastModifiedTimeMillis }
                    Descending -> filteredNotes.sortedBy { it.lastModifiedTimeMillis }
                }

                AlphaNumeric -> when (sortOrder) {
                    Ascending -> filteredNotes.sortedBy { it.fullName() }
                    Descending -> filteredNotes.sortedByDescending { it.fullName() }
                }
            }
        }
    }.combine(query) { allNotesInCurrentPath, query ->
        if (query.isNotEmpty()) {
            fuzzySort(query, allNotesInCurrentPath)
        } else {
            allNotesInCurrentPath
        }
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )


    val gridNotes = notes.mapAndCombine { notes ->
        notes.groupBy {
            it.nameWithoutExtension()
        }
    }.combine(selectedNotes) { (notes, notesGroupByName), selectedNotes ->
        notes.map { note ->
            val name = note.nameWithoutExtension()

            GridNote(
                // if there is more than one note with the same name, draw the full path
                title = if (notesGroupByName[name]!!.size > 1) {
                    note.relativePath
                } else {
                    name
                }, selected = selectedNotes.contains(note), note = note
            )
        }
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )


    val drawerFolders = combine(
        dao.allNoteFolders(), currentNoteFolderRelativePath
    ) { notesFolders, path ->
        notesFolders.filter {
            it.parentPath() == path
        }
    }.combine(notes) { folders, notes ->
        folders.map { folder ->

            val (noteCount, lastModifiedTimeMillis) = notes
                .filter { it.parentPath().startsWith(folder.relativePath) }
                .fold(Pair(0, Long.MIN_VALUE)) { (count, max), note ->
                    (count + 1) to max(max, note.lastModifiedTimeMillis)
                }

            DrawerFolderModel(
                noteCount = noteCount,
                lastModifiedTimeMillis = max(lastModifiedTimeMillis, folder.lastModifiedTimeMillis),
                noteFolder = folder
            )
        }
    }.let { folders ->
        combine(
            folders, prefs.sortType.getFlow(), prefs.sortOrder.getFlow()
        ) { folders, sortType, sortOrder ->

            when (sortType) {
                Modification -> when (sortOrder) {
                    Ascending -> folders.sortedByDescending { it.lastModifiedTimeMillis }
                    Descending -> folders.sortedBy { it.lastModifiedTimeMillis }
                }

                AlphaNumeric -> when (sortOrder) {
                    Ascending -> folders.sortedBy { it.noteFolder.fullName() }
                    Descending -> folders.sortedByDescending { it.noteFolder.fullName() }
                }
            }
        }
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )

}

