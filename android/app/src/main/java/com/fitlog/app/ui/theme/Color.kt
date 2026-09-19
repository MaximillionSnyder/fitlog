package com.fitlog.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de FitLog.
 *
 * Los roles de Material 3 cubren los componentes estandar; [FitLogColors] agrega los tokens de
 * marca (acento lima, datos cian) y los semanticos (exito, aviso, peligro) que una app de
 * entrenamiento necesita y que Material no expresa.
 */

// Marca: acento lima (accion y foco) y datos cian (series y metricas).
private val LimeBright = Color(0xFFBEF264)
private val LimeDeep = Color(0xFF4D7C0F)
private val CyanBright = Color(0xFF67E8F9)
private val CyanDeep = Color(0xFF0E7490)
private val AmberBright = Color(0xFFFCD34D)
private val AmberDeep = Color(0xFFB45309)
private val RoseBright = Color(0xFFFDA4AF)
private val RoseDeep = Color(0xFFBE123C)
private val EmeraldBright = Color(0xFF6EE7B7)
private val EmeraldDeep = Color(0xFF047857)

// Superficies: azul noche con capas para que las tarjetas se separen sin bordes duros.
private val NightBackground = Color(0xFF070B15)
private val NightSurface = Color(0xFF0E1526)
private val NightSurfaceLow = Color(0xFF0B111F)
private val NightSurfaceHigh = Color(0xFF16203A)
private val NightSurfaceHighest = Color(0xFF1E2A47)
private val NightOutline = Color(0xFF2A3655)
private val NightOnSurface = Color(0xFFE7ECF5)
private val NightOnSurfaceVariant = Color(0xFF9BA8C0)

// Superficies claras: blanco humedo con capas frias suaves.
private val DayBackground = Color(0xFFF6F8FC)
private val DaySurface = Color(0xFFFFFFFF)
private val DaySurfaceLow = Color(0xFFF1F4FA)
private val DaySurfaceHigh = Color(0xFFE9EEF8)
private val DaySurfaceHighest = Color(0xFFDDE5F3)
private val DayOutline = Color(0xFFC6CFDF)
private val DayOnSurface = Color(0xFF101828)
private val DayOnSurfaceVariant = Color(0xFF4A5568)

@Immutable
data class FitLogColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val data: Color,
    val dataSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val isDark: Boolean,
)

val FitLogDarkPalette = FitLogColors(
    accent = LimeBright,
    onAccent = Color(0xFF14200A),
    accentSoft = Color(0xFF243318),
    accentText = LimeBright,
    data = CyanBright,
    dataSoft = Color(0xFF10303A),
    success = EmeraldBright,
    successSoft = Color(0xFF0F2E24),
    warning = AmberBright,
    warningSoft = Color(0xFF33280E),
    danger = RoseBright,
    dangerSoft = Color(0xFF3A1520),
    isDark = true,
)

val FitLogLightPalette = FitLogColors(
    accent = LimeDeep,
    onAccent = Color(0xFFF8FEE8),
    accentSoft = Color(0xFFEAF7C9),
    accentText = LimeDeep,
    data = CyanDeep,
    dataSoft = Color(0xFFDCF3F9),
    success = EmeraldDeep,
    successSoft = Color(0xFFDCF5EA),
    warning = AmberDeep,
    warningSoft = Color(0xFFFDF0D5),
    danger = RoseDeep,
    dangerSoft = Color(0xFFFCE4E9),
    isDark = false,
)

val FitLogDarkColors = darkColorScheme(
    primary = LimeBright,
    onPrimary = Color(0xFF14200A),
    primaryContainer = Color(0xFF2B3D12),
    onPrimaryContainer = Color(0xFFE4FBB8),
    secondary = CyanBright,
    onSecondary = Color(0xFF06232B),
    secondaryContainer = Color(0xFF113541),
    onSecondaryContainer = Color(0xFFC7F1FA),
    tertiary = AmberBright,
    onTertiary = Color(0xFF2A1D02),
    tertiaryContainer = Color(0xFF33280E),
    onTertiaryContainer = Color(0xFFFCE6B4),
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceContainerLowest = NightSurfaceLow,
    surfaceContainerLow = Color(0xFF0C1322),
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    outline = NightOutline,
    outlineVariant = Color(0xFF1F2A42),
    error = RoseBright,
    onError = Color(0xFF3A0A14),
    errorContainer = Color(0xFF4A1220),
    onErrorContainer = Color(0xFFFFD9DF),
    inverseSurface = NightOnSurface,
    inverseOnSurface = NightSurface,
    scrim = Color(0xFF000000),
)

val FitLogLightColors = lightColorScheme(
    primary = LimeDeep,
    onPrimary = Color(0xFFF8FEE8),
    primaryContainer = Color(0xFFE4F6C0),
    onPrimaryContainer = Color(0xFF22330A),
    secondary = CyanDeep,
    onSecondary = Color(0xFFF2FBFD),
    secondaryContainer = Color(0xFFD8F1F8),
    onSecondaryContainer = Color(0xFF062A33),
    tertiary = AmberDeep,
    onTertiary = Color(0xFFFFFBF2),
    tertiaryContainer = Color(0xFFFBEBC8),
    onTertiaryContainer = Color(0xFF2E2004),
    background = DayBackground,
    onBackground = DayOnSurface,
    surface = DaySurface,
    onSurface = DayOnSurface,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = DayOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = DaySurfaceLow,
    surfaceContainer = DaySurface,
    surfaceContainerHigh = DaySurfaceHigh,
    surfaceContainerHighest = DaySurfaceHighest,
    outline = DayOutline,
    outlineVariant = Color(0xFFDCE3EF),
    error = RoseDeep,
    onError = Color(0xFFFFF5F6),
    errorContainer = Color(0xFFFBDDE3),
    onErrorContainer = Color(0xFF3F0A17),
    inverseSurface = DayOnSurface,
    inverseOnSurface = Color(0xFFF4F7FC),
    scrim = Color(0xFF000000),
)

val LocalFitLogColors = staticCompositionLocalOf { FitLogDarkPalette }
