## Contexto

FitLog tiene dos clientes sobre el mismo modelo de datos: Android (Kotlin + Compose + Material 3) y web (React 19 + Tailwind 4). Hoy la presentación no tiene un sistema: el tema de Android es un `darkColorScheme` con seis colores, los colores de marca se repiten literales en cada pantalla, Inicio es una lista de botones y la web usa la paleta por defecto de Tailwind con un header de nueve botones. El usuario pidió rehacer las interfaces con conceptos modernos, con prioridad en Android (es el cliente de uso diario) y con tema claro y oscuro.

Restricciones del entorno: Android solo compila en GitHub Actions (en local no hay JDK ni SDK), así que cada paso debe ser compilable de una vez y verificarse por CI; la web sí se compila y testea localmente con Node 24.

## Decisiones

### Tokens antes que pantallas

Se define un sistema de tokens (color, tipografía, forma, espaciado) y una biblioteca de componentes antes de tocar pantallas. El motivo es que el reshape no se degrade: cada pantalla debe construirse con las mismas primitivas y no con estilos ad-hoc. El sistema vive en `ui/theme/` y `ui/components/`, y es la única fuente de color en la app.

### Acento de marca y roles semánticos

Los roles de Material 3 no alcanzan para expresar la identidad de una app de entrenamiento (racha, PR, volumen). Se agregan dos grupos de tokens propios expuestos por `LocalFitLogColors`:

- **Marca**: `accent` (lima, acción y foco), `accentSoft` (relleno de contenedores de acento), `accentText` (variante legible sobre superficie) y `data`/`dataSoft` (cian, series y métricas).
- **Semánticos**: `success`, `warning`, `danger` con sus variantes suaves.

`primary` de Material 3 se mapea al acento lima y `tertiary` al cian, para que los componentes estándar (botones, switches, indicadores) hereden la identidad sin retoques.

### Tema dual con elección del usuario

`FitLogTheme(mode, dynamicColor)` recibe un `ThemeMode` (`SISTEMA`, `CLARO`, `OSCURO`) y decide:

1. Si el usuario activó colores dinámicos y el sistema es Android 12+, se usan `dynamicLightColorScheme`/`dynamicDarkColorScheme` con el contexto de la actividad: la app se ve integrada al teléfono.
2. Si no, se usan los esquemas de marca `FitLogLightColors`/`FitLogDarkColors`.

Los tokens de marca (acento, semánticos) no dependen de Material: se definen por separado para claro y oscuro y se eligen con `isSystemInDarkTheme()` o el modo forzado. La preferencia se guarda con `SharedPreferences` (sin dependencias nuevas) detrás de `AppSettings`, un `StateFlow` observable desde Compose.

### Navegación: cinco destinos y una pantalla "Más"

La barra inferior tiene cinco pestañas: **Inicio, Entrenar, Progreso, Rutinas, Más**. Catálogo, Medidas, Comparativas, Tips, Respaldo y Ajustes viven en "Más", que es una lista de tarjetas con descripción. Se usa un único `NavHost` con las rutas de primer nivel y las secundarias; la barra inferior se oculta en las secundarias y en el detalle de ejercicio.

Navegar entre pestañas usa `popUpTo(startDestination) { saveState = true }`, `launchSingleTop = true` y `restoreState = true`: el estado de cada pestaña (scroll, filtros, formularios) se conserva y la pila no crece. Los encabezados de las pantallas destino pasan a un `TopAppBar` del shell, con la acción propia de cada pantalla como `actions`.

### Inicio como panel, no como menú

Inicio deja de ser un índice de botones:

- **Hero**: saludo según la hora, resumen de una línea y acción primaria ("Iniciar entrenamiento" o "Continuar sesión").
- **Sesión activa**: cuando hay una sesión en curso, una tarjeta con duración, series registradas y volumen acumulado, y el botón para continuar.
- **Bento de estadísticas**: sesiones y volumen de los últimos 7 días con la variación contra los 7 anteriores, racha de semanas con al menos una sesión, sesiones totales y último peso corporal. Cada tile es una unidad visual con etiqueta, valor grande y delta opcional.
- **Accesos directos**: Rutinas, Catálogo y Comparativas como tarjetas con icono y descripción.

La agregación es de presentación: se calcula en `HomeViewModel` a partir de las sesiones y las medidas existentes, sin consultas nuevas al esquema ni columnas derivadas persistidas. El rango de 7 días se calcula con `Progress.DAY_MS`, la misma constante que usa el resto del dominio.

### Iconos propios

No se agrega `material-icons-extended` (son ~4 MB de recursos). Los iconos que la app necesita se dibujan como `ImageVector` en `ui/components/BrandIcon.kt` con un estilo geométrico consistente (trazo redondeado, viewport 24x24), agrupados en un `object FitLogIcons` para que las pantallas no repitan `PathData`.

### Estados vacíos con acción

`EmptyState` pasa a aceptar un título, un mensaje y una acción opcional. Un estado vacío que solo dice "no hay datos" deja al usuario sin salida; con la acción (por ejemplo "Agregar rutina") la pantalla vacía es el punto de partida del flujo.

## Riesgos y mitigaciones

- **Compilación solo en CI**: cada tarea se escribe para compilar sin poder ejecutarla localmente; los cambios se agrupan por archivo, se validan con `openspec validate` antes del push y el push a `main` dispara `android.yml`.
- **Contraste en tema claro**: el acento lima sobre blanco no alcanza contraste para texto; en claro el acento se usa como relleno con `onAccent` oscuro y el texto de acento usa una variante oscurecida (`accentText`).
- **Colores dinámicos**: los tokens de marca podrían chocar con la paleta del sistema; el modo dinámico se ofrece como opción apagada por defecto y solo cambia los roles de Material, no los semánticos.
- **Estados conservados por pestaña**: `saveState`/`restoreState` de Navigation Compose conserva el estado de los `ViewModel` con scope de back stack; las pantallas ya usan `hiltViewModel()` con ese scope, así que no hay cambios de arquitectura.

## Alternativas descartadas

- **Bottom bar con las nueve secciones**: demasiadas pestañas, etiquetas ilegibles y sin jerarquía.
- **Navigation rail o drawer**: en un teléfono, el rail desperdicia ancho y el drawer esconde la navegación detrás de un gesto.
- **Copiar los iconos de Material Symbols**: rutas de 24x24 ajenas al proyecto, sin garantía de que el `PathData` sea correcto; se prefieren iconos propios, más simples y verificables.
- **Hilt + DataStore para la preferencia de tema**: correcto a largo plazo, pero agrega dependencia y módulo para un solo booleano; `SharedPreferences` detrás de `AppSettings` alcanza y se puede migrar después.
- **Migrar el detalle de ejercicio al shell**: queda como está; el cambio no toca `platform/motion`.
