package com.fitlog.app.di

import android.content.Context
import androidx.room.Room
import com.fitlog.app.data.FitLogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    const val DATABASE_NAME = "fitlog.db"

    @Provides
    @Singleton
    fun provideFitLogDatabase(@ApplicationContext context: Context): FitLogDatabase =
        Room.databaseBuilder(context, FitLogDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
}
