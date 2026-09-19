## 1. Sistema de diseño Android

- [x] 1.1 Crear `ui/theme/Color.kt` con la paleta de marca (acento lima, datos cian, semánticos) y los `ColorScheme` claro y oscuro completos; verificar que CI compila
- [x] 1.2 Crear `ui/theme/Type.kt` y `ui/theme/Shape.kt` con la escala tipográfica (cifras tabulares para datos) y las formas del sistema; verificar que CI compila
- [x] 1.3 Reescribir `ui/theme/Theme.kt` con `FitLogTheme(mode)`, colores dinámicos en Android 12+, `LocalFitLogColors` y la extensión de fondo con gradiente de marca; verificar que CI compila
- [x] 1.4 Crear `ui/components/BrandIcon.kt` con el set de iconos vectoriales propios (sin `material-icons-extended`); verificar que CI compila
- [x] 1.5 Crear `ui/components/FitLogCard.kt`, `StatTile.kt`, `SectionHeader.kt` y `PrimaryAction.kt`; verificar que CI compila
- [x] 1.6 Extender `ui/motion/EmptyState.kt` con acción opcional y usarlo en todas las listas vacías; verificar que CI compila

## 2. App shell y navegación

- [x] 2.1 Crear `ui/AppSettings.kt` con la preferencia de tema (`sistema`/`claro`/`oscuro`) persistida en el dispositivo; verificar que CI compila
- [x] 2.2 Crear `ui/destinations/FitLogDestination.kt` con los destinos de primer nivel (ruta, etiqueta, icono) y los agrupados en "Más"; verificar que CI compila
- [x] 2.3 Reescribir `MainActivity.kt` con el scaffold de barra inferior, navegación con `saveState`/`restoreState` y single-top por pestaña; verificar que CI compila
- [x] 2.4 Crear `ui/MoreScreen.kt` con las tarjetas de Catálogo, Medidas, Comparativas, Tips y Respaldo, y la entrada a Ajustes; verificar que CI compila
- [x] 2.5 Mover el estado de la base de datos de Inicio a una pantalla de Ajustes con el selector de tema, el modo dinámico y el resumen del esquema; verificar que CI compila
- [x] 2.6 Quitar de las pantallas destino el encabezado propio y usar el encabezado del shell, conservando las acciones de cada pantalla; verificar que CI compila

## 3. Inicio como panel

- [x] 3.1 Crear `ui/HomeViewModel.kt` con la agregación de últimos 7 días, 7 anteriores, racha, total histórico, último peso corporal y sesión activa; verificar que CI compila
- [x] 3.2 Agregar el test unitario de la agregación de estadísticas en `app/src/test/`; verificar en CI
- [x] 3.3 Crear `ui/HomeScreen.kt` con hero, acción primaria, tarjeta de sesión activa, bento de estadísticas y accesos directos; verificar el resultado en el APK
- [x] 3.4 Conectar Inicio a la sesión activa (continuar entrenamiento) y a las rutas de primer nivel; verificar el resultado en el APK

## 4. Web (fase posterior)

- [x] 4.1 Definir los tokens del sistema de diseño en `web/src/index.css` con `@theme` de Tailwind y el modo claro/oscuro; verificar `build` y `lint`
- [x] 4.2 Crear las primitivas de la web (`Card`, `StatTile`, `Button`, `SectionHeader`) sobre esos tokens; verificar `build` y `lint`
- [x] 4.3 Reemplazar el app shell de `App.tsx` por tab bar inferior en móvil y rail lateral en escritorio; verificar en el navegador
- [x] 4.4 Reescribir la vista de Inicio como panel y alinear las vistas restantes a las primitivas; verificar `build`, `lint` y tests

## 5. Verificación y cierre

- [x] 5.0 Reconectar las transiciones de elementos compartidos al nuevo Inicio (las claves `home-*` quedaron sin usar al retirar los botones planos)

- [ ] 5.1 Dejar `android.yml` en verde (tests y APK) tras el push a `main`
- [ ] 5.2 Probar en el APK: navegación por las cinco pestañas conservando estado, tema claro/oscuro/dinámico, panel de Inicio con y sin sesión activa, y Ajustes
- [x] 5.3 Verificar contraste y legibilidad en tema claro y oscuro, y que la app siga usable con animaciones del sistema en cero
- [ ] 5.4 Archivar el cambio y actualizar `openspec/specs` cuando el reshape esté cerrado

## 6. Seguimiento posterior al cierre

- [x] 6.1 Iniciar el entrenamiento desde el panel de Inicio en un solo toque (autoarranque de la sesión)
- [x] 6.2 Mostrar la sesión activa en el shell (punto en la pestaña Entrenar y acción Continuar en el encabezado)
- [x] 6.3 Agregar el resumen de la actividad de hoy al panel, con la medianoche local inyectable y sus tests
- [x] 6.4 Documentar el sistema de diseño en `docs/diseno.md` y enlazarlo desde el README
- [x] 6.6 Migrar las ocho pantallas restantes de Android al sistema de diseño (tarjetas, encabezados, tiles, acciones y estados)
- [x] 6.7 Unificar los errores de carga con `ErrorState` y reintento, y los estados de carga con `LoadingState`
- [ ] 6.5 Revisar el resultado en el APK y en la web con el usuario y ajustar lo que pida
