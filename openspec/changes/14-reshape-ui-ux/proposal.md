## Why

Las dos interfaces de FitLog son funcionales pero planas y con poca jerarquía. En Android, Inicio es una columna de ocho botones y el estado de la base de datos ocupa el lugar del contenido útil; cada pantalla repite su propio encabezado y no hay una barra de navegación persistente, así que moverse entre secciones obliga a volver siempre a Inicio. El tema es fijo oscuro (`darkColorScheme` único, sin modo claro) y los colores se repiten sueltos en cada archivo. El usuario pidió mejorar UI y UX con conceptos modernos y rehacer cómo se ven las interfaces, con prioridad en Android.

## What Changes

- **Sistema de diseño explícito** en Android: tokens de color de marca (acento lima/cian, semánticos de éxito/aviso/peligro), tema **dual claro/oscuro** con un modo de tema del usuario (`sistema`, `claro`, `oscuro`) y la opción de colores dinámicos de Android 12+, tipografía con cifras tabulares para datos y escala de formas/espaciado.
- **App shell nuevo**: barra de navegación inferior con cinco destinos (Inicio, Entrenar, Progreso, Rutinas, Más) que conserva el estado de cada pestaña, y una pantalla **Más** que agrupa Catálogo, Medidas, Comparativas, Tips, Respaldo y Ajustes. Se retiran los ocho botones planos de Inicio y las tarjetas de estado de la base de datos pasan a Ajustes.
- **Inicio como panel**: hero con saludo y acción primaria de entrenamiento, tarjeta de sesión activa cuando hay una en curso, y un bento de estadísticas (sesiones y volumen de los últimos 7 días, comparación con los 7 anteriores, racha, total histórico, último peso corporal) más accesos directos al contenido.
- **Biblioteca de componentes propia**: `StatTile`, `SectionHeader`, `FitLogCard`, `BrandIcon` (iconos vectoriales propios, sin depender de `material-icons-extended`), estados vacíos con acción y barras/chips consistentes, para que las nueve pantallas dejen de inventar su propio estilo.
- **Tema claro/oscuro persistente**: la preferencia se guarda en el dispositivo y se aplica al reabrir la app.
- **Web alineada después**: los tokens, el shell responsivo (tab bar inferior en móvil + rail lateral en escritorio) y el panel de Inicio se replican en la web en una fase posterior del mismo cambio.
- **Non-goals**: no cambian el esquema de datos, las migraciones, el seed, los vectores compartidos ni las fórmulas de dominio; no se agregan dependencias de UI nuevas (ni `material-icons-extended`, ni librerías de animación en web); no se toca el detalle de ejercicio ni el comportamiento de `platform/motion`; no hay rediseño de la lógica de negocio de cada pantalla, solo de su presentación.

## Capabilities

### New Capabilities

- `platform/design-system`: tokens de color, tema claro/oscuro/dinámico, tipografía y formas, biblioteca de componentes de UI y estados vacíos/carga/error compartidos entre las pantallas de Android (y, en su fase web, de la web).
- `platform/app-shell`: estructura de navegación y arquitectura de información de las apps: destinos de primer nivel, barra inferior/sidebar, pantalla "Más", conservación de estado por pestaña y ajustes de apariencia.

### Modified Capabilities

<!-- Ninguna: `platform/scaffold` cubre el arranque y la persistencia, y `platform/motion` las animaciones; ninguno cambia su contrato. -->

## Impact

- Android: nuevos `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Shape.kt`, `ui/theme/Theme.kt` (reescrito), `ui/components/` (`FitLogCard`, `StatTile`, `SectionHeader`, `BrandIcon`, `PrimaryAction`), `ui/HomeScreen.kt` + `ui/HomeViewModel.kt` (nuevos), `ui/MoreScreen.kt` (nuevo), `ui/AppSettings.kt` (preferencia de tema) y `MainActivity.kt` (shell con barra inferior y rutas).
- Android: se modifican los encabezados de Catálogo, Medidas, Comparativas, Tips, Respaldo, Progreso, Rutinas y Entrenar para usar los componentes del sistema de diseño; la lógica de sus ViewModels no cambia.
- Web: fase posterior del mismo cambio (tokens en `index.css`, primitivas en `ui/`, shell responsivo y `App.tsx`).
- Dependencias: ninguna nueva. Los colores dinámicos usan `android.os.Build` y `dynamicDarkColorScheme`/`dynamicLightColorScheme` de Material 3. La preferencia de tema usa `SharedPreferences` del propio contexto.
- Datos: sin migraciones, sin cambios de esquema, seed ni vectores compartidos.
- Tests: se agrega un test unitario de la agregación de estadísticas de Inicio; el resto de la verificación es compilación en CI y prueba manual en el APK.
