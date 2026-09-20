package com.fitlog.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitlog.app.data.CatalogDao
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.BackupDao
import com.fitlog.app.data.BackupRepository
import com.fitlog.app.data.BodyMetricsDao
import com.fitlog.app.data.BodyMetricsRepository
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

    /**
     * Migracion 002: metricas de actividad importada.
     *
     * Las sesiones importadas (Huawei Health o GPX) traen distancia, calorias, frecuencia cardiaca,
     * pasos y desnivel; hasta ahora vivian solo en la nota.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE session ADD COLUMN distance_m REAL")
            db.execSQL("ALTER TABLE session ADD COLUMN calories REAL")
            db.execSQL("ALTER TABLE session ADD COLUMN avg_heart_rate REAL")
            db.execSQL("ALTER TABLE session ADD COLUMN max_heart_rate REAL")
            db.execSQL("ALTER TABLE session ADD COLUMN steps INTEGER")
            db.execSQL("ALTER TABLE session ADD COLUMN elevation_gain_m REAL")
            db.execSQL("ALTER TABLE session ADD COLUMN source TEXT")
            recordMigration(db, 2, "002_actividad_importada")
        }
    }

    @Provides
    @Singleton
    fun provideFitLogDatabase(@ApplicationContext context: Context): FitLogDatabase =
        Room.databaseBuilder(context, FitLogDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addMigrations(MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    recordMigration(db, 1, "001_esquema_inicial")
                    recordMigration(db, 2, "002_actividad_importada")
                }
            })
            .build()

    /** Deja constancia de la migracion aplicada, igual que hace la web. */
    private fun recordMigration(db: SupportSQLiteDatabase, version: Int, name: String) {
        db.execSQL(
            "INSERT OR REPLACE INTO schema_migration (version, name, applied_at) VALUES (?, ?, ?)",
            arrayOf<Any>(version, name, System.currentTimeMillis()),
        )
    }

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
    fun provideBodyMetricsDao(database: FitLogDatabase): BodyMetricsDao = database.bodyMetricsDao()

    @Provides
    @Singleton
    fun provideBodyMetricsRepository(bodyMetricsDao: BodyMetricsDao): BodyMetricsRepository = BodyMetricsRepository(dao = bodyMetricsDao)

    @Provides
    @Singleton
    fun provideBackupDao(database: FitLogDatabase): BackupDao = database.backupDao()

    @Provides
    @Singleton
    fun provideBackupRepository(
        database: FitLogDatabase,
        backupDao: BackupDao,
    ): BackupRepository = BackupRepository(database = database, backupDao = backupDao)

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
