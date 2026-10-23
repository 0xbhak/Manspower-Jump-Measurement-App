package com.manspower.jumpmeasure.di

import android.content.Context
import androidx.room.Room
import com.manspower.jumpmeasure.data.local.AppDatabase
import com.manspower.jumpmeasure.data.local.dao.JumpDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jump_measure_db"
        )
        .fallbackToDestructiveMigration() // Clean reset: drop & recreate on version change
        .build()
    }

    @Provides
    fun provideJumpDao(appDatabase: AppDatabase): JumpDao {
        return appDatabase.jumpDao()
    }
}
