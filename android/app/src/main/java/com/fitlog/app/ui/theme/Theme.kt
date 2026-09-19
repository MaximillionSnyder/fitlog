package com.fitlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FitLogColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF0B1220),
    secondary = Color(0xFFA5B4FC),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111A2E),
    onSurface = Color(0xFFE5E7EB),
)

@Composable
fun FitLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FitLogColors, content = content)
}
