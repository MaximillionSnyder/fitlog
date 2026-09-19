package com.fitlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitlog.app.ui.CatalogScreen
import com.fitlog.app.ui.WorkoutScreen
import com.fitlog.app.ui.DbStatusUi
import com.fitlog.app.ui.DbStatusViewModel
import com.fitlog.app.ui.theme.FitLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitLogTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = HOME_ROUTE) {
                    composable(HOME_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            HomeScreen(
                                onOpenCatalog = { navController.navigate(CATALOG_ROUTE) },
                                onOpenWorkout = { navController.navigate(WORKOUT_ROUTE) },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(CATALOG_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            CatalogScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(WORKOUT_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            WorkoutScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val HOME_ROUTE = "home"
        const val CATALOG_ROUTE = "catalog"
        const val WORKOUT_ROUTE = "workout"
    }
}

@Composable
private fun HomeScreen(
    onOpenCatalog: () -> Unit,
    onOpenWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DbStatusViewModel = hiltViewModel(),
) {
    val status by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "FitLog", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "Registro de entrenamiento local-first",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ESTADO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                when (val current = status) {
                    DbStatusUi.Cargando -> Text("Iniciando base de datos…")
                    is DbStatusUi.Error -> Text(
                        text = "Error: ${current.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    is DbStatusUi.Listo -> StatusDetails(current)
                }
            }
        }

        Button(onClick = onOpenWorkout, modifier = Modifier.fillMaxWidth()) {
            Text("Entrenar")
        }

        OutlinedButton(onClick = onOpenCatalog, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir catálogo")
        }
    }
}

@Composable
private fun StatusDetails(status: DbStatusUi.Listo) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Base de datos lista", style = MaterialTheme.typography.titleMedium)
        StatusRow("Esquema", "v${status.schemaVersion}")
        StatusRow("SQLite", status.sqliteVersion)
        StatusRow("Archivo", status.databaseName)
        StatusRow("Grupos musculares", status.muscleGroupCount.toString())
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}
