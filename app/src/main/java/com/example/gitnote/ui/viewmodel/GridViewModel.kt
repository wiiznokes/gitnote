package com.example.gitnote.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitnote.MyApp
import com.example.gitnote.R
import com.example.gitnote.data.AppPreferences
import com.example.gitnote.data.platform.FolderFs
import com.example.gitnote.data.room.Note
import com.example.gitnote.data.room.NoteFolder
import com.example.gitnote.data.room.RepoDatabase
import com.example.gitnote.helper.NameValidation
import com.example.gitnote.manager.StorageManager
import com.example.gitnote.ui.screen.app.DrawerFolderModel
import com.example.gitnote.ui.util.fuzzySort
import com.example.gitnote.util.contains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val query = _query

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    private val _currentNoteFolderRelativePath = MutableStateFlow("")
    val currentNoteFolderRelativePath: StateFlow<String>
        get() = _currentNoteFolderRelativePath.asStateFlow()


    private val allNotes = dao.allNotes()
    private val allNoteFolders = dao.allNoteFolders()

    init {
        Log.d(TAG, "init")

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.updateDatabaseAndRepo()
        }

        CoroutineScope(Dispatchers.IO).launch {
            allNotes.collect { allNotes ->
                //  Log.d(TAG, "filter selected note, depend on allNotes")
                selectedNotes.value.filter { selectedNote ->
                    allNotes.contains { note ->
                        note.relativePath == selectedNote
                    }
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
        }
    }

    fun createNoteFolder(relativeParentPath: String, name: String): Boolean {
        if (!NameValidation.check(name)) {
            uiHelper.makeToast(uiHelper.getString(R.string.invalid_name))
            return false
        }

        val relativePath = "$relativeParentPath/$name"

        prefs.repoPath.getBlocking().let { rootPath ->
            if (FolderFs.fromPath(rootPath, relativePath).exist()) {
                uiHelper.makeToast(uiHelper.getString(R.string.folder_already_exist))
                return false
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            storageManager.createNoteFolder(
                noteFolder = NoteFolder.new(
                    relativePath = relativePath
                )
            )
        }

        return true
    }


    private val _selectedNotes: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val selectedNotes: StateFlow<List<String>>
        get() = _selectedNotes.asStateFlow()


    /**
     * @param add true if the note must be selected, false otherwise
     */
    fun selectNote(relativePath: String, add: Boolean) = viewModelScope.launch {
        if (add) {
            selectedNotes.value.plus(relativePath)
        } else {
            selectedNotes.value.minus(relativePath)
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
            uiHelper.makeToast("Notes successfully deleted")
        }
    }

    fun deleteNote(note: Note) {
        selectNote(note.relativePath, false)
        CoroutineScope(Dispatchers.IO).launch {
            storageManager.deleteNote(note)
            uiHelper.makeToast("Note successfully deleted")
        }
    }

    fun deleteFolder(relativePath: String) {
        // todo
    }


    private val allNotesInCurrentPath =
        allNotes.combine(currentNoteFolderRelativePath) { allNotes, path ->
            //Log.d(TAG, "filter all notes in path, depend on AllNotes and current path")
            allNotes.filter {
                it.relativePath.startsWith(path)
            }
        }.stateIn(
            CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val filteredNotes = allNotesInCurrentPath.combine(query) { notes, query ->
        //Log.d(TAG, "filter all notes in query, depend on allNotesInCurrentPath and query")
        if (query.isNotEmpty()) {
            fuzzySort(query, notes)
        } else {
            notes
        }
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )


    val drawerFolders =
        allNoteFolders.combine(currentNoteFolderRelativePath) { notesFolders, path ->
            notesFolders.filter {
                it.parentPath() == path
            }
        }.combine(filteredNotes) { folders, notes ->
            folders.map { folder ->
                DrawerFolderModel(
                    relativePath = folder.relativePath,
                    fullName = folder.fullName(),
                    noteCount = notes.count {
                        it.parentPath() == folder.relativePath
                    },
                    id = folder.id
                )
            }
        }.stateIn(
            CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
        )

}