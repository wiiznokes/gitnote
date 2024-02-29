package com.example.gitnote.data.room

import android.content.Context
import android.util.Log
import androidx.room.CoroutinesRoom
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gitnote.AppModule
import com.example.gitnote.AppModuleImpl
import com.example.gitnote.MyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random


private const val TAG = "RepoDatabase"

@Database(
    entities = [NoteFolder::class, Note::class],
    version = 1
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
                .fallbackToDestructiveMigration()
                .addCallback(onMigration)
                .build()
        }
    }

}


