package com.meghna.audioanalyzer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AudioSession::class],
    version = 1,
    exportSchema = false
)
abstract class AudioDatabase : RoomDatabase() {
    abstract fun audioSessionDao(): AudioSessionDao
}
