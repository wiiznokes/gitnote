package io.github.wiiznokes.gitnote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.data.room.RepoDatabase.Companion.buildFactory
import io.github.wiiznokes.gitnote.data.room.RepoDatabaseDao
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration.MEMORY_DB_PATH
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "RepoDatabaseTest"

@RunWith(AndroidJUnit4::class)
class RepoDatabaseTest {

    private lateinit var db: RepoDatabase
    private lateinit var dao: RepoDatabaseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // https://issuetracker.google.com/issues/454083281
        db = Room.databaseBuilder(
            context = context,
            klass = RepoDatabase::class.java,
            name = TAG
        )
            .allowMainThreadQueries()
            .openHelperFactory(buildFactory(MEMORY_DB_PATH))
            .build()


        dao = db.repoDatabaseDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDrawerFoldersQuery() = runTest {

        dao.insertNoteFolder(NoteFolder.new(""))
        dao.insertNoteFolder(NoteFolder.new("test1"))
        dao.insertNoteFolder(NoteFolder.new("test2"))
        dao.insertNoteFolder(NoteFolder.new("test1/test1.2"))


        dao.insertNote(Note.new("test1/1-1.md"))
        dao.insertNote(Note.new("test1/1-2.md"))
        dao.insertNote(Note.new("test1/test1.2/1.2-1.md"))
        dao.insertNote(Note.new("test2/2-1.md"))
        dao.insertNote(Note.new("test1/1-3.md"))

        dao.testing()

    }
}