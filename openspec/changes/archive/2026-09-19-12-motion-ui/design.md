## Context

Ver `proposal.md` - Why. La app Android navega con un `NavHost` único en `MainActivity.kt` y cada pantalla es un `composable` con `Scaffold`; los encabezados son un `Text` dentro de un `Row`. El detalle del catálogo es hoy un `AlertDialog` en `CatalogScreen.kt` y los filtros viven en `CatalogViewModel` (scope del back stack entry). El BOM de Compose es 2024.12.01 (Compose 1.7.6), que ya incluye `SharedTransitionLayout`; la migración a Compose 1.12 queda registrada como pendiente en `docs/pendiente-compose-bom-1.12.md`.

## Goals / Non-Goals

**Goals:**

- Dar continuidad visual a la navegación sin reescribir la arquitectura de navegación ni los ViewModels.
- Reutilizar un vocabulario de motion chico y consistente (`ui/motion/`) en todas las pantallas.
- Mantener el comportamiento y los datos intactos: solo cambia la capa de UI.

**Non-Goals:**

- Cambiar la web, el esquema, el dominio o los repositorios.
- Subir el BOM/AGP/Kotlin (se documenta como pendiente aparte).
- Animaciones de entrada/salida personalizadas por pantalla, transiciones con gestos o navegación con tabs.

## Decisions

### D1. `SharedTransitionLayout` en `MainActivity` + scopes por CompositionLocal

El `NavHost` se envuelve en `SharedTransitionLayout` y se provee `LocalSharedTransitionScope` una sola vez; cada destino envuelve su contenido en `NavEntryScopes(this)` para proveer el `AnimatedVisibilityScope` del `AnimatedContentScope` de Navigation 2. Las pantallas consumen dos extensiones componibles de `Modifier` (`sharedNavBounds(key)` y `sharedNavElement(key)`) que resuelven los scopes y evitan repetir `with(scope)` y opt-ins en cada archivo.

Alternativa descartada: pasar `sharedTransitionScope`/`animatedVisibilityScope` como parámetros a las 9 pantallas, que ensucia todas las firmas y los previews.

### D2. El detalle del catálogo pasa a pantalla

Compartir elementos entre una tarjeta y un `AlertDialog` no es posible (el diálogo es otra ventana), así que en Android el detalle se muda a la ruta `catalog/{exerciseId}` con `ExerciseDetailScreen` + `ExerciseDetailViewModel` (Hilt, `SavedStateHandle`). La web conserva su panel inline, que ya cumple el spec. La vuelta conserva búsqueda y filtros porque `CatalogViewModel` sigue vivo en el back stack, y la posición de scroll se guarda con `rememberSaveable`.

Alternativa descartada: mantener el diálogo y limitar los shared elements a Inicio→encabezados, que dejaba afuera la transición más vistosa.

### D3. `graphics-shapes:1.0.1` sin subir el BOM

`RoundedPolygon`/`Morph` viven en `androidx.graphics:graphics-shapes`, que no está en el BOM actual; se agrega explícito al version catalog. `MaterialShapes` (formas Material predefinidas) recién existe en material3 1.4+ y `MeshGradientPainter` en Compose 1.12, así que las formas y los gradientes se definen a mano por ahora; adoptarlos queda para el cambio de toolchain ya documentado.

### D4. `MorphPolygonShape` para el botón de sesión

Se implementa `MorphPolygonShape : Shape` (patrón oficial: `morph.toPath(progress).asComposePath()` + `Matrix.scale/translate`) y un `MorphActionButton` que usa `Modifier.clip(shape)` + `clickable(role = Role.Button)` en lugar de `Button`, porque `Button` no permite una forma animada. El icono play/stop es otro `Morph` entre un triángulo y un cuadrado dibujado en `Canvas`. El progreso sale de `animateFloatAsState`, que respeta `MotionDurationScale`.

### D5. Blobs deterministas y livianos

Cada blob es un `Morph` entre dos `RoundedPolygon` orgánicos generados con una semilla fija (vértices y radios estables entre recomposiciones), animado con `rememberInfiniteTransition` + `RepeatMode.Reverse`, y dibujado con `drawWithCache` sobre un `Path` reutilizado. Sin blur (minSdk 26) y con un máximo de 2-3 blobs por pantalla. Si `LocalMotionDurationScale` está en cero, se dibuja el progreso 0 sin animación.

### D6. `EmptyState` único para todas las listas vacías

Se reemplazan los mensajes/tarjetas vacías de catálogo, entrenamientos, rutinas, progreso, medidas, tips y comparativas por un composable `EmptyState(message)` con blob + texto centrado, para que la mejora sea consistente y no pantalla por pantalla.

## Risks / Trade-offs

- **APIs experimentales** (`ExperimentalSharedTransitionApi`): versión fija de Compose y opt-ins localizados en `ui/motion/`; si cambian, el impacto queda contenido ahí.
- **Shared bounds con contenedores con scroll**: puede requerir ajustar `boundsTransform`; se verifica en el APK y, si molesta, se degrada a `sharedElement` solo en el nombre.
- **CI-only Android**: no hay Gradle local; un error de compilación cuesta un ciclo de CI. Mitigación: mantener los archivos de motion chicos, con APIs confirmadas contra la documentación oficial.
- **Costo de dibujo de los blobs**: acotado a 2-3 por pantalla, con `drawWithCache` y sin blur; en gama baja el riesgo es bajo.
- **El botón morph pierde el ripple de Material**: se compensa con estado presionado (escala/alpha) en el propio botón.
