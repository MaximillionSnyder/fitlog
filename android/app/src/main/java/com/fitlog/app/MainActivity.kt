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
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitlog.app.ui.BackupScreen
import com.fitlog.app.ui.BodyMetricsScreen
import com.fitlog.app.ui.CatalogScreen
import com.fitlog.app.ui.ComparisonsScreen
import com.fitlog.app.ui.ProgressScreen
import com.fitlog.app.ui.TipsScreen
import com.fitlog.app.ui.RoutinesScreen
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
                                onOpenRoutines = { navController.navigate(ROUTINES_ROUTE) },
                                onOpenProgress = { navController.navigate(PROGRESS_ROUTE) },
                                onOpenComparisons = { navController.navigate(COMPARISONS_ROUTE) },
                                onOpenTips = { navController.navigate(TIPS_ROUTE) },
                                onOpenBody = { navController.navigate(BODY_ROUTE) },
                                onOpenBackup = { navController.navigate(BACKUP_ROUTE) },
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
                    composable(
                        route = WORKOUT_PATTERN,
                        arguments = listOf(
                            navArgument(ROUTINE_ARG) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                    ) { backStackEntry ->
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            WorkoutScreen(
                                onBack = { navController.popBackStack() },
                                initialRoutineId = backStackEntry.arguments?.getString(ROUTINE_ARG),
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(BACKUP_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            BackupScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(BODY_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            BodyMetricsScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(TIPS_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            TipsScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(COMPARISONS_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            ComparisonsScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(PROGRESS_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            ProgressScreen(
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    composable(ROUTINES_ROUTE) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                            RoutinesScreen(
                                onBack = { navController.popBackStack() },
                                onTrainRoutine = { routineId ->
                                    navController.navigate("workout?$ROUTINE_ARG=$routineId")
                                },
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
        const val ROUTINE_ARG = "routineId"
        const val WORKOUT_ROUTE = "workout"
        const val WORKOUT_PATTERN = "workout?$ROUTINE_ARG={$ROUTINE_ARG}"
        const val ROUTINES_ROUTE = "routines"
        const val PROGRESS_ROUTE = "progress"
        const val COMPARISONS_ROUTE = "comparisons"
        const val TIPS_ROUTE = "tips"
        const val BODY_ROUTE = "body"
        const val BACKUP_ROUTE = "backup"
    }
}

@Composable
private fun HomeScreen(
    onOpenCatalog: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenComparisons: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenBackup: () -> Unit,
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

        OutlinedButton(onClick = onOpenProgress, modifier = Modifier.fillMaxWidth()) {
            Text("Progreso")
        }

        OutlinedButton(onClick = onOpenComparisons, modifier = Modifier.fillMaxWidth()) {
            Text("Comparativas")
        }

        OutlinedButton(onClick = onOpenTips, modifier = Modifier.fillMaxWidth()) {
            Text("Tips")
        }

        OutlinedButton(onClick = onOpenBody, modifier = Modifier.fillMaxWidth()) {
            Text("Medidas")
        }

        OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth()) {
            Text("Respaldo")
        }

        OutlinedButton(onClick = onOpenRoutines, modifier = Modifier.fillMaxWidth()) {
            Text("Rutinas")
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
