## Why

La UI de Android es funcional pero plana: las pantallas aparecen y desaparecen sin continuidad visual, las tarjetas del catálogo no se conectan con su detalle y el botón de sesión es un botón más. El usuario pidió una mejora visual con técnicas de motion: transiciones de elementos compartidos, formas que mutan y blobs orgánicos.

## What Changes

- Animar la navegación con elementos compartidos: el botón de Inicio se transforma en el encabezado de la pantalla destino, y la tarjeta de un ejercicio se transforma en el encabezado de su detalle.
- Pasar el detalle de ejercicio de Android de diálogo a pantalla (`catalog/{exerciseId}`), conservando búsqueda, filtros y posición del catálogo al volver.
- Mutar la forma e icono del botón de sesión de Entrenar entre Iniciar (play) y Finalizar (stop) con `RoundedPolygon` + `Morph`.
- Agregar blobs orgánicos animados como fondo del hero de Inicio, de tarjetas de resumen y de los estados vacíos.
- Respetar la preferencia de movimiento reducido del sistema: sin animaciones, las pantallas siguen siendo usables y los elementos animados se muestran estáticos.
- **Non-goals**: cambios en la web (el detalle inline y el resto de la UI web quedan igual), migración del Compose BOM, edición de ejercicios, imágenes o descripciones, y animaciones de entrada/salida personalizadas por pantalla más allá de las que provee Navigation.

## Capabilities

### New Capabilities

- `platform/motion`: comportamiento de movimiento de la app Android (transiciones de elementos compartidos, formas que mutan, blobs orgánicos y respeto del movimiento reducido).

### Modified Capabilities

<!-- Ninguna: `exercises/catalog` ya cubre la apertura del detalle y la conservación de filtros al cerrarlo; el pasaje de diálogo a pantalla es implementación. -->

## Impact

- Dependencias Android: se agrega `androidx.graphics:graphics-shapes:1.0.1` al version catalog (el BOM de Compose no cambia).
- Android: `MainActivity.kt` (SharedTransitionLayout, ruta de detalle, encabezados de Inicio), `CatalogScreen.kt` (navegación al detalle y shared bounds), nuevas `ExerciseDetailScreen.kt`/`ExerciseDetailViewModel.kt`, `WorkoutScreen.kt` (botón morph y estado vacío), `ui/motion/` (scopes, morph, blobs, empty state) y encabezados de las pantallas destino.
- Web: sin cambios.
- Datos: sin migraciones, sin cambios de esquema, seed ni vectores compartidos.
- Tests: sin cambios de dominio; la verificación es de compilación en CI y prueba manual en APK.
