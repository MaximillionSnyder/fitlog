package com.fitlog.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitlog.app.ui.destinations.TopLevelDestination
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Encabezado de pantalla: titulo del destino, accion de volver y las acciones propias de la
 * pantalla. Lo provee el shell para que todas las pantallas tengan el mismo encabezado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitLogTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = FitLogIcons.Back,
                        contentDescription = "Volver",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * Barra de navegacion inferior con los destinos de primer nivel.
 *
 * Usa el acento de marca para el indicador del destino activo, con el contenedor elevado para
 * separarse del contenido.
 */
@Composable
fun FitLogBottomBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val fitLog = MaterialTheme.fitLogColors
    Surface(
        color = colors.surfaceContainer,
        contentColor = colors.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        NavigationBar(
            containerColor = colors.surfaceContainer,
            contentColor = colors.onSurface,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(destination.route) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = { Text(text = destination.label) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = fitLog.accentText,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
