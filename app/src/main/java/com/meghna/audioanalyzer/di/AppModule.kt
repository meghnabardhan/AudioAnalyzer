package com.meghna.audioanalyzer.di

import android.content.Context
import androidx.room.Room
import com.meghna.audioanalyzer.data.db.AudioDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioDatabase(
        @ApplicationContext context: Context
    ): AudioDatabase {
        return Room.databaseBuilder(
            context,
            AudioDatabase::class.java,
            "audio_analyzer_database"
        ).build()
    }
}
