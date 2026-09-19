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
        )
    } catch (error: Exception) {
        DbStatusUi.Error(error.message ?: "Error desconocido")
    }

    private fun queryScalar(sql: String): String {
        val cursor = database.openHelper.readableDatabase.query(sql)
        return cursor.use { if (it.moveToFirst()) it.getString(0) else "" }
    }
}
