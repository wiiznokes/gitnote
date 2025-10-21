package io.github.wiiznokes.gitnote.data.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.wiiznokes.gitnote.MyApp
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration
import io.requery.android.database.sqlite.SQLiteFunction
import kotlinx.coroutines.runBlocking
import kotlin.random.Random


private const val TAG = "RepoDatabase"

@Database(
    entities = [NoteFolder::class, Note::class, NoteFts::class],
    version = 2
)
abstract class RepoDatabase : RoomDatabase() {

    abstract val repoDatabaseDao: RepoDatabaseDao

    companion object {
        fun generateUid() = Random.Default.nextInt()

        fun buildDatabase(context: Context): RepoDatabase {
            val onMigration = object : Callback() {
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    Log.d(TAG, "onDestructiveMigration")
                    runBlocking {
                        MyApp.appModule.appPreferences.databaseCommit.update("")
                    }
                }
            }

            return Room
                .databaseBuilder(
                    context = context,
                    klass = RepoDatabase::class.java,
                    name = TAG
                )
                .fallbackToDestructiveMigration(true)
                .addCallback(onMigration)
                .openHelperFactory { configuration ->
                    val config = SQLiteDatabaseConfiguration(
                        context.filesDir.toPath().resolve(TAG).toString(),
                        SQLiteDatabase.OPEN_CREATE or SQLiteDatabase.OPEN_READWRITE
                    )

                    config.functions.add(SQLiteFunction("rank", 1, Rank))

                    val options = RequerySQLiteOpenHelperFactory.ConfigurationOptions { config }
                    RequerySQLiteOpenHelperFactory(listOf(options)).create(configuration)
                }
                .build()
        }
    }

}


