package com.meghna.audioanalyzer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AudioSession)

    @Query("SELECT * FROM audio_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<AudioSession>>

    @Query("DELETE FROM audio_sessions")
    suspend fun deleteAllSessions()
}
