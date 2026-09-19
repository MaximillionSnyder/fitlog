package com.fitlog.app.ui.destinations

import androidx.compose.ui.graphics.vector.ImageVector
import com.fitlog.app.ui.components.FitLogIcons

/**
 * Rutas de la app. Las de primer nivel son las cinco pestanas de la barra inferior; el resto se
 * agrupa en "Más" o son pantallas de detalle con accion de volver.
 */
object Routes {
    private const val WORKOUT_ROOT = "workout"

    const val HOME = "home"
    const val WORKOUT = WORKOUT_ROOT
    const val WORKOUT_PATTERN = "$WORKOUT_ROOT?routineId={routineId}&autoStart={autoStart}"
    const val ROUTINE_ARG = "routineId"
    const val AUTO_START_ARG = "autoStart"
    const val PROGRESS = "progress"
    const val ROUTINES = "routines"
    const val MORE = "more"
    const val CATALOG = "catalog"
    const val CATALOG_DETAIL_PATTERN = "catalog/{exerciseId}"
    const val EXERCISE_ARG = "exerciseId"
    const val BODY = "body"
    const val COMPARISONS = "comparisons"
    const val TIPS = "tips"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"

    fun workout(routineId: String?) =
        "$WORKOUT?$ROUTINE_ARG=${routineId.orEmpty()}&$AUTO_START_ARG=true"

    fun catalogDetail(exerciseId: String): String = "$CATALOG/$exerciseId"
}

/** Destino de primer nivel: una pestana de la barra inferior. */
data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val topLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination(Routes.HOME, "Inicio", FitLogIcons.Home),
    TopLevelDestination(Routes.WORKOUT, "Entrenar", FitLogIcons.Dumbbell),
    TopLevelDestination(Routes.PROGRESS, "Progreso", FitLogIcons.Chart),
    TopLevelDestination(Routes.ROUTINES, "Rutinas", FitLogIcons.Calendar),
    TopLevelDestination(Routes.MORE, "Más", FitLogIcons.More),
)

/** Pantalla secundaria agrupada en "Más": titulo, descripcion e icono. */
data class SecondaryDestination(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val inMore: Boolean = true,
)

val secondaryDestinations: List<SecondaryDestination> = listOf(
    SecondaryDestination(
        route = Routes.CATALOG,
        title = "Catálogo",
        description = "Explorá ejercicios por grupo muscular y equipamiento",
        icon = FitLogIcons.Grid,
    ),
    SecondaryDestination(
        route = Routes.BODY,
        title = "Medidas",
        description = "Peso corporal y perímetros a lo largo del tiempo",
        icon = FitLogIcons.Scale,
    ),
    SecondaryDestination(
        route = Routes.COMPARISONS,
        title = "Comparativas",
        description = "Récords, mes contra mes y balance muscular",
        icon = FitLogIcons.Trophy,
    ),
    SecondaryDestination(
        route = Routes.TIPS,
        title = "Tips",
        description = "Observaciones sobre tus últimos entrenamientos",
        icon = FitLogIcons.Spark,
    ),
    SecondaryDestination(
        route = Routes.BACKUP,
        title = "Respaldo",
        description = "Exportá o fusioná tus datos entre dispositivos",
        icon = FitLogIcons.Shield,
    ),
    SecondaryDestination(
        route = Routes.SETTINGS,
        title = "Ajustes",
        description = "Tema, colores y estado de la base de datos",
        icon = FitLogIcons.Gear,
    ),
)

fun moreDestinations(): List<SecondaryDestination> = secondaryDestinations.filter { it.inMore }

fun titleForRoute(route: String?): String? = when (route) {
    Routes.WORKOUT -> "Entrenar"
    Routes.PROGRESS -> "Progreso"
    Routes.ROUTINES -> "Rutinas"
    Routes.MORE -> "Más"
    else -> secondaryDestinations.firstOrNull { it.route == route }?.title
}
