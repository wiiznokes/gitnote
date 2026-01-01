package io.github.wiiznokes.gitnote.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.helper.FrontmatterParser
import io.github.wiiznokes.gitnote.helper.NameValidation
import io.github.wiiznokes.gitnote.manager.StorageManager
import io.github.wiiznokes.gitnote.ui.model.FileExtension
import io.github.wiiznokes.gitnote.ui.model.GridNote
import io.github.wiiznokes.gitnote.ui.model.NoteViewType
import io.github.wiiznokes.gitnote.ui.model.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map as flowMap
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

    private val refreshCounter = MutableStateFlow(0)

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


    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    fun selectTag(tag: String?) {
        viewModelScope.launch {
            _selectedTag.emit(tag)
        }
    }


    init {
        Log.d(TAG, "init")
    }

    suspend fun refreshSelectedNotes() {
        selectedNotes.value.filter { selectedNote ->
            dao.isNoteExist(selectedNote.relativePath)
        }.let { newSelectedNotes ->
            _selectedNotes.emit(newSelectedNotes)
        }
    }

    fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            _isRefreshing.emit(true)
            storageManager.updateDatabaseAndRepo()
            refreshSelectedNotes()
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

    fun toggleViewType() {
        viewModelScope.launch {
            val next = if (prefs.noteViewType.get() == NoteViewType.Grid) {
                NoteViewType.List
            } else {
                NoteViewType.Grid
            }
            prefs.noteViewType.update(next)
        }
    }

    fun deleteSelectedNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentSelectedNotes = selectedNotes.value
            unselectAllNotes()
            storageManager.deleteNotes(currentSelectedNotes)
        }
    }

    fun deleteNote(note: Note) {
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

        val currentNoteFolderRelativePath = currentNoteFolderRelativePath.value

        val parent = if (currentNoteFolderRelativePath == "") {
            prefs.defaultPathForNewNote.getBlocking()
        } else currentNoteFolderRelativePath

        return Note.new(
            relativePath = "$parent/$defaultFullName",
        )
    }

    fun defaultNewTask(): Note {
        val note = defaultNewNote()
        val content = FrontmatterParser.addCompleted(note.content)
        return note.copy(content = content)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = combine(
        currentNoteFolderRelativePath as Flow<Any>,
        prefs.sortOrder.getFlow() as Flow<Any>,
        query as Flow<Any>,
        selectedTag as Flow<Any>,
        refreshCounter as Flow<Any>
    ) { a, b, c, e, d ->
        val currentNoteFolderRelativePath = a as String
        val sortOrder = b as SortOrder
        val query = c as String
        val selectedTag = e as String?
        val refreshCounter = d as Int
        Pager(
            config = PagingConfig(pageSize = 50),
            pagingSourceFactory = {
                if (query.isEmpty()) {
                    dao.gridNotes(currentNoteFolderRelativePath, sortOrder, selectedTag)
                } else {
                    dao.gridNotesWithQuery(currentNoteFolderRelativePath, sortOrder, query, selectedTag)
                }
            }
        ).flow.cachedIn(viewModelScope)
    }.flatMapLatest { it }

    val gridNotes = combine(
        pagingFlow,
        selectedNotes
    ) { pagingData: PagingData<GridNote>, selectedNotes: List<Note> ->
        pagingData.map { gridNote ->
            gridNote.copy(
                selected = selectedNotes.contains(gridNote.note),
                completed = FrontmatterParser.parseCompletedOrNull(gridNote.note.content)
            )
        }
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), PagingData.empty()
    )

    val allTags = dao.allNotes().flowMap { notes: List<Note> ->
        notes.flatMap { note: Note ->
            FrontmatterParser.parseTags(note.content)
        }.distinct().sorted()
    }.stateIn(
        CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // todo: use pager
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

    fun reloadDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val res = storageManager.updateDatabase(force = true)
            res.onFailure {
                uiHelper.makeToast("$it")
            }
        }
    }

    fun toggleCompleted(note: Note) {
        viewModelScope.launch {
            val newContent = FrontmatterParser.toggleCompleted(note.content)
            val newNote = note.copy(
                content = newContent,
                lastModifiedTimeMillis = System.currentTimeMillis()
            )
            val result = storageManager.updateNote(newNote, note)
            result.onFailure {
                uiHelper.makeToast("Failed to update note: $it")
            }
            // Trigger refresh
            refreshCounter.value++
        }
    }

    fun convertToTask(note: Note) {
        viewModelScope.launch {
            val newContent = FrontmatterParser.addCompleted(note.content)
            val newNote = note.copy(
                content = newContent,
                lastModifiedTimeMillis = System.currentTimeMillis()
            )
            val result = storageManager.updateNote(newNote, note)
            result.onFailure {
                uiHelper.makeToast("Failed to convert note: $it")
            }
            // Trigger refresh
            refreshCounter.value++
        }
    }

    fun convertToNote(note: Note) {
        viewModelScope.launch {
            val newContent = FrontmatterParser.removeCompleted(note.content)
            val newNote = note.copy(
                content = newContent,
                lastModifiedTimeMillis = System.currentTimeMillis()
            )
            val result = storageManager.updateNote(newNote, note)
            result.onFailure {
                uiHelper.makeToast("Failed to convert note: $it")
            }
            // Trigger refresh
            refreshCounter.value++
        }
    }
}
