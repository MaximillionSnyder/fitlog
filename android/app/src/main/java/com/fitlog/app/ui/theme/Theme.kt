package com.fitlog.app.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Modo de tema elegido por el usuario. */
enum class ThemeMode(val wire: String, val label: String) {
    SYSTEM("system", "Seguir al sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro"),
    ;

    companion object {
        val default = SYSTEM

        fun fromWire(value: String?): ThemeMode =
            entries.firstOrNull { it.wire == value } ?: default
    }
}

@Composable
fun FitLogTheme(
    mode: ThemeMode = ThemeMode.default,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val dynamicScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }

    val colorScheme = dynamicScheme ?: if (dark) FitLogDarkColors else FitLogLightColors
    val fitLogColors = if (dark) FitLogDarkPalette else FitLogLightPalette

    CompositionLocalProvider(LocalFitLogColors provides fitLogColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FitLogTypography,
            shapes = FitLogShapes,
            content = content,
        )
    }
}

/** Acceso a los tokens de marca y semanticos desde cualquier composable. */
val MaterialTheme.fitLogColors: FitLogColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFitLogColors.current

/**
 * Fondo de marca: un lavado del acento en la parte superior que se disuelve en el fondo. Da
 * profundidad al panel de Inicio sin agregar imagenes ni capas costosas.
 */
@Composable
fun Modifier.fitLogBackground(): Modifier {
    val colors = MaterialTheme.colorScheme
    val accent = LocalFitLogColors.current.accent
    return this.then(
        Modifier.background(
            Brush.verticalGradient(
                0f to accent.copy(alpha = if (LocalFitLogColors.current.isDark) 0.10f else 0.14f),
                0.35f to Color.Transparent,
                1f to Color.Transparent,
            )
        )
    ).background(colors.background)
}

/** Contenedor raiz de una pantalla: aplica el fondo de marca y el color de superficie base. */
@Composable
fun FitLogScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .fitLogBackground(),
        content = content,
    )
}
