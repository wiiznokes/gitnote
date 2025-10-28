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
import io.github.wiiznokes.gitnote.utils.mapAndCombine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val query: StateFlow<String> = _query

    val syncState = storageManager.syncState

    fun consumeOkSyncState() {
        viewModelScope.launch {
            storageManager.consumeOkSyncState()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()



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


    init {
        Log.d(TAG, "init")

        CoroutineScope(Dispatchers.IO).launch {
            gridNotes.collect {
                selectedNotes.value.filter { selectedNote ->
                    dao.isNoteExist(selectedNote.relativePath)
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
        }
    }

    fun deleteNote(note: Note) {
        selectNote(note, false)
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.deleteNote(note)
        }
    }

    fun deleteFolder(noteFolder: NoteFolder) {
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.deleteNoteFolder(noteFolder)
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


    @OptIn(ExperimentalCoroutinesApi::class)
    val note = combine(
        currentNoteFolderRelativePath,
        prefs.sortOrder.getFlow(),
        query,
    ) { currentNoteFolderRelativePath, sortOrder, query ->
        Triple(currentNoteFolderRelativePath, sortOrder, query)
    }.flatMapLatest { triple ->
        val (currentNoteFolderRelativePath, sortOrder, query) = triple

        if (query.isEmpty()) {
            dao.gridNotes(currentNoteFolderRelativePath, sortOrder)
        } else {
            dao.gridNotesWithQuery(currentNoteFolderRelativePath, sortOrder, query)
        }
    }

    val gridNotes = note.mapAndCombine { notes ->
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val drawerFolders = combine(
        currentNoteFolderRelativePath,
        prefs.sortOrderFolder.getFlow(),
    ) { currentNoteFolderRelativePath, sortOrder ->
        Pair(currentNoteFolderRelativePath, sortOrder)
    }.flatMapLatest { pair ->
        val (currentNoteFolderRelativePath, sortOrder) = pair
        dao.drawerFolders(currentNoteFolderRelativePath, sortOrder)
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )

}

