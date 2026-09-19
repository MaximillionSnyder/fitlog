package com.fitlog.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitlog.app.data.CatalogDao
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.ComparisonsDao
import com.fitlog.app.data.ComparisonsRepository
import com.fitlog.app.data.ProgressDao
import com.fitlog.app.data.ProgressRepository
import com.fitlog.app.data.RoutinesDao
import com.fitlog.app.data.RoutinesRepository
import com.fitlog.app.data.WorkoutDao
import com.fitlog.app.data.WorkoutRepository
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
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "INSERT OR REPLACE INTO schema_migration (version, name, applied_at) VALUES (1, '001_esquema_inicial', ?)",
                        arrayOf(System.currentTimeMillis()),
                    )
                }
            })
            .build()

    @Provides
    @Singleton
    fun provideCatalogDao(database: FitLogDatabase): CatalogDao = database.catalogDao()

    @Provides
    @Singleton
    fun provideWorkoutDao(database: FitLogDatabase): WorkoutDao = database.workoutDao()

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDao: WorkoutDao,
        routinesDao: RoutinesDao,
    ): WorkoutRepository = WorkoutRepository(dao = workoutDao, routinesDao = routinesDao)

    @Provides
    @Singleton
    fun provideRoutinesDao(database: FitLogDatabase): RoutinesDao = database.routinesDao()

    @Provides
    @Singleton
    fun provideRoutinesRepository(routinesDao: RoutinesDao): RoutinesRepository = RoutinesRepository(dao = routinesDao)

    @Provides
    @Singleton
    fun provideProgressDao(database: FitLogDatabase): ProgressDao = database.progressDao()

    @Provides
    @Singleton
    fun provideProgressRepository(progressDao: ProgressDao): ProgressRepository = ProgressRepository(dao = progressDao)

    @Provides
    @Singleton
    fun provideComparisonsDao(database: FitLogDatabase): ComparisonsDao = database.comparisonsDao()

    @Provides
    @Singleton
    fun provideComparisonsRepository(comparisonsDao: ComparisonsDao): ComparisonsRepository = ComparisonsRepository(dao = comparisonsDao)

    @Provides
    @Singleton
    fun provideCatalogRepository(
        @ApplicationContext context: Context,
        catalogDao: CatalogDao,
    ): CatalogRepository = CatalogRepository(
        dao = catalogDao,
        seedJsonProvider = {
            context.assets.open("seed/catalog.json").bufferedReader().use { reader -> reader.readText() }
        },
    )
}
