package com.meghna.audioanalyzer.di

import android.content.Context
import android.media.AudioManager
import androidx.room.Room
import com.meghna.audioanalyzer.data.db.AudioDatabase
import com.meghna.audioanalyzer.data.repository.AudioRepository
import com.meghna.audioanalyzer.data.repository.AudioRepositoryImpl
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

    @Provides
    @Singleton
    fun provideAudioManager(
        @ApplicationContext context: Context
    ): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    @Singleton
    fun provideAudioRepository(
        audioManager: AudioManager,
        @ApplicationContext context: Context
    ): AudioRepository {
        return AudioRepositoryImpl(audioManager, context)
    }
}
