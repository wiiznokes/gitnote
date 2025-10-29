package io.github.wiiznokes.gitnote.data.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
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
                .openHelperFactory(buildFactory(context.filesDir.toPath().resolve(TAG).toString()))
                .build()
        }

        fun buildFactory(path: String): SupportSQLiteOpenHelper.Factory {
            return object : SupportSQLiteOpenHelper.Factory {
                override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
                    val config = SQLiteDatabaseConfiguration(
                        path,
                        SQLiteDatabase.OPEN_CREATE or SQLiteDatabase.OPEN_READWRITE
                    )

                    config.functions.add(SQLiteFunction("rank", 1, Rank))
                    config.functions.add(SQLiteFunction("parentPath", 1, ParentPath))
                    config.functions.add(SQLiteFunction("fullName", 1, FullName))

                    val options = RequerySQLiteOpenHelperFactory.ConfigurationOptions { config }
                    return RequerySQLiteOpenHelperFactory(listOf(options)).create(configuration)
                }

            }
        }

    }

}


