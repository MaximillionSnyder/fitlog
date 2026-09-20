package com.fitlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitlog.app.data.AppSettings
import com.fitlog.app.ui.BackupScreen
import com.fitlog.app.ui.BodyMetricsScreen
import com.fitlog.app.ui.CatalogScreen
import com.fitlog.app.ui.ComparisonsScreen
import com.fitlog.app.ui.ExerciseDetailScreen
import com.fitlog.app.ui.HomeScreen
import com.fitlog.app.ui.ImportScreen
import com.fitlog.app.ui.MoreScreen
import com.fitlog.app.ui.ProgressScreen
import com.fitlog.app.ui.RoutinesScreen
import com.fitlog.app.ui.SessionDetailScreen
import com.fitlog.app.ui.SettingsScreen
import com.fitlog.app.ui.TipsScreen
import com.fitlog.app.ui.WorkoutScreen
import com.fitlog.app.ui.WorkoutSessionViewModel
import com.fitlog.app.ui.components.FitLogBottomBar
import com.fitlog.app.ui.components.FitLogTopBar
import com.fitlog.app.ui.destinations.Routes
import com.fitlog.app.ui.destinations.titleForRoute
import com.fitlog.app.ui.destinations.topLevelDestinations
import com.fitlog.app.ui.motion.LocalSharedTransitionScope
import com.fitlog.app.ui.motion.sharedNavBounds
import com.fitlog.app.ui.motion.NavEntryScopes
import com.fitlog.app.ui.theme.FitLogTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by appSettings.dynamicColor.collectAsStateWithLifecycle()
            FitLogTheme(mode = themeMode, dynamicColor = dynamicColor) {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        FitLogApp(appSettings = appSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun FitLogApp(
    appSettings: AppSettings,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: WorkoutSessionViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoute = currentRoute?.substringBefore('?')
    val isTopLevel = topLevelDestinations.any { it.route == topLevelRoute }
    val title = titleForRoute(topLevelRoute)
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    // Cada cambio de destino revalida si hay una sesion abierta (el detalle lo arma Entrenar).
    LaunchedEffect(currentRoute) { sessionViewModel.refresh() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (title != null) {
                FitLogTopBar(
                    title = title,
                    onBack = if (isTopLevel) null else ({ navController.popBackStack() }),
                    titleModifier = navController.sharedTitleModifier(topLevelRoute),
                ) {
                    if (session.active && topLevelRoute != Routes.WORKOUT) {
                        TextAction(
                            label = "Continuar",
                            onClick = { navController.navigate(Routes.WORKOUT) },
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (isTopLevel) {
                FitLogBottomBar(
                    destinations = topLevelDestinations,
                    currentRoute = topLevelRoute,
                    onSelect = { route -> navController.navigateTopLevel(route) },
                    modifier = Modifier.navigationBarsPadding(),
                    badgedRoute = if (session.active) Routes.WORKOUT else null,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            composable(Routes.HOME) {
                NavEntryScopes(this) {
                    HomeScreen(
                        // Iniciar entra a Entrenar con la sesion ya abierta; continuar solo entra.
                        onStartWorkout = { navController.navigate(Routes.workout(null)) },
                        onOpenWorkout = { navController.navigate(Routes.WORKOUT) },
                        onOpenRoutines = { navController.navigate(Routes.ROUTINES) },
                        onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                        onOpenComparisons = { navController.navigate(Routes.COMPARISONS) },
                        onOpenBody = { navController.navigate(Routes.BODY) },
                    )
                }
            }

            composable(
                route = Routes.WORKOUT_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ROUTINE_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Routes.AUTO_START_ARG) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entry ->
                NavEntryScopes(this) {
                    WorkoutScreen(
                        initialRoutineId = entry.arguments?.getString(Routes.ROUTINE_ARG),
                        autoStart = entry.arguments?.getBoolean(Routes.AUTO_START_ARG) == true,
                        onOpenSessionDetail = { sessionId ->
                            navController.navigate(Routes.sessionDetail(sessionId))
                        },
                    )
                }
            }

            composable(
                route = Routes.SESSION_DETAIL_PATTERN,
                arguments = listOf(
                    navArgument(Routes.SESSION_ARG) { type = NavType.StringType }
                ),
            ) { entry ->
                NavEntryScopes(this) {
                    SessionDetailScreen(
                        sessionId = entry.arguments?.getString(Routes.SESSION_ARG).orEmpty(),
                    )
                }
            }

            composable(Routes.PROGRESS) {
                NavEntryScopes(this) {
                    ProgressScreen()
                }
            }

            composable(Routes.ROUTINES) {
                NavEntryScopes(this) {
                    RoutinesScreen(
                        onTrainRoutine = { routineId ->
                            navController.navigate(Routes.workout(routineId))
                        },
                    )
                }
            }

            composable(Routes.MORE) {
                NavEntryScopes(this) {
                    MoreScreen(
                        onOpenRoute = { route -> navController.navigate(route) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
            }

            composable(Routes.CATALOG) {
                NavEntryScopes(this) {
                    CatalogScreen(
                        onOpenDetail = { exerciseId ->
                            navController.navigate(Routes.catalogDetail(exerciseId))
                        },
                    )
                }
            }

            composable(
                route = Routes.CATALOG_DETAIL_PATTERN,
                arguments = listOf(
                    navArgument(Routes.EXERCISE_ARG) { type = NavType.StringType }
                ),
            ) { entry ->
                NavEntryScopes(this) {
                    ExerciseDetailScreen(
                        exerciseId = entry.arguments?.getString(Routes.EXERCISE_ARG).orEmpty(),
                        catalogEntry = navController.getBackStackEntry(Routes.CATALOG),
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            composable(Routes.BODY) {
                NavEntryScopes(this) {
                    BodyMetricsScreen()
                }
            }

            composable(Routes.COMPARISONS) {
                NavEntryScopes(this) {
                    ComparisonsScreen(
                        onOpenWorkout = { navController.navigate(Routes.WORKOUT) },
                    )
                }
            }

            composable(Routes.TIPS) {
                NavEntryScopes(this) {
                    TipsScreen()
                }
            }

            composable(Routes.BACKUP) {
                NavEntryScopes(this) {
                    BackupScreen()
                }
            }

            composable(Routes.IMPORT) {
                NavEntryScopes(this) {
                    ImportScreen()
                }
            }

            composable(Routes.SETTINGS) {
                NavEntryScopes(this) {
                    SettingsScreen(
                        appSettings = appSettings,
                        viewModel = hiltViewModel(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * El titulo del encabezado participa de la transicion cuando la pantalla tiene un acceso en Inicio
 * (la tarjeta de acceso se transforma en el encabezado del destino).
 */
@Composable
private fun NavHostController.sharedTitleModifier(route: String?): Modifier =
    when (route) {
        Routes.ROUTINES -> Modifier.sharedNavBounds("home-routines")
        Routes.PROGRESS -> Modifier.sharedNavBounds("home-progress")
        Routes.COMPARISONS -> Modifier.sharedNavBounds("home-comparisons")
        else -> Modifier
    }

/**
 * Navegacion entre pestanas: una sola copia por destino, estado propio conservado y sin apilar
 * pantallas de primer nivel.
 */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
