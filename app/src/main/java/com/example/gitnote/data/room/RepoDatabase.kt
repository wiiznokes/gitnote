package com.example.gitnote.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import kotlin.random.Random

@Database(
    entities = [NoteFolder::class, Note::class],
    version = 2
)
abstract class RepoDatabase : RoomDatabase() {
    abstract val repoDatabaseDao: RepoDatabaseDao

    companion object {
        fun generateUid() = Random.Default.nextInt()
    }
}

