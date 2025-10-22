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

@RunWith(AndroidJUnit4::class)
class RepoDatabaseTest {

    private lateinit var db: RepoDatabase
    private lateinit var dao: RepoDatabaseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // https://issuetracker.google.com/issues/454083281
        db = Room.inMemoryDatabaseBuilder(
            context = context,
            klass = RepoDatabase::class.java
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

        dao.insertNoteFolder(NoteFolder.new("notes"))
        dao.insertNoteFolder(NoteFolder.new("notes/work"))
        dao.insertNote(Note.new("notes/work/a.txt"))

        dao.testing()

    }
}