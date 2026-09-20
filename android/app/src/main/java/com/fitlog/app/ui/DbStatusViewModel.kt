package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.di.DatabaseModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface DbStatusUi {
    data object Cargando : DbStatusUi

    data class Listo(
        val sqliteVersion: String,
        val schemaVersion: Int,
        val databaseName: String,
        val muscleGroupCount: Int,
        /** Resumen de los entrenamientos guardados, para comprobar que se importaron. */
        val sessions: Int,
        val importedSessions: Int,
        val sessionsWithMetrics: Int,
        val firstSessionAtMs: Long?,
        val lastSessionAtMs: Long?,
    ) : DbStatusUi

    data class Error(val message: String) : DbStatusUi
}

@HiltViewModel
class DbStatusViewModel @Inject constructor(
    private val database: FitLogDatabase,
) : ViewModel() {

    private val _state = MutableStateFlow<DbStatusUi>(DbStatusUi.Cargando)
    val state: StateFlow<DbStatusUi> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = DbStatusUi.Cargando
            _state.value = withContext(Dispatchers.IO) { loadStatus() }
        }
    }

    private suspend fun loadStatus(): DbStatusUi = try {
        DbStatusUi.Listo(
            sqliteVersion = queryScalar("SELECT sqlite_version()"),
            schemaVersion = queryScalar("PRAGMA user_version").toIntOrNull() ?: -1,
            databaseName = database.openHelper.databaseName ?: DatabaseModule.DATABASE_NAME,
            muscleGroupCount = database.metaDao().muscleGroupCount(),
            sessions = countOf("SELECT COUNT(*) FROM session WHERE deleted_at IS NULL"),
            importedSessions = countOf(
                "SELECT COUNT(*) FROM session WHERE deleted_at IS NULL " +
                    "AND (notes LIKE 'Huawei Health%' OR notes LIKE 'GPX%')"
            ),
            sessionsWithMetrics = countOf(
                "SELECT COUNT(*) FROM session WHERE deleted_at IS NULL " +
                    "AND (distance_m IS NOT NULL OR avg_heart_rate IS NOT NULL " +
                    "OR calories IS NOT NULL OR steps IS NOT NULL)"
            ),
            firstSessionAtMs = timestampOf("SELECT MIN(started_at) FROM session WHERE deleted_at IS NULL"),
            lastSessionAtMs = timestampOf("SELECT MAX(started_at) FROM session WHERE deleted_at IS NULL"),
        )
    } catch (error: Exception) {
        DbStatusUi.Error(error.message ?: "Error desconocido")
    }

    private fun countOf(sql: String): Int = queryScalar(sql).toIntOrNull() ?: 0

    private fun timestampOf(sql: String): Long? = queryScalar(sql).toLongOrNull()

    private fun queryScalar(sql: String): String {
        val cursor = database.openHelper.readableDatabase.query(sql)
        return cursor.use { if (it.moveToFirst()) it.getString(0) else "" }
    }
}
