package com.fitlog.app.di

import android.content.Context
import com.fitlog.app.data.AppSettings
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
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings = AppSettings(context)
}
