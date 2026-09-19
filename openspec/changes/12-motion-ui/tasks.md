## 1. Base

- [ ] 1.1 Agregar `androidx.graphics:graphics-shapes:1.0.1` al version catalog y al `build.gradle.kts` del módulo; verificar que CI compila
- [ ] 1.2 Crear `ui/motion/SharedTransitionScopes.kt` con los CompositionLocals, `NavEntryScopes` y las extensiones `sharedNavBounds`/`sharedNavElement`; verificar que CI compila
- [ ] 1.3 Envolver el `NavHost` en `SharedTransitionLayout` y aplicar `NavEntryScopes` a los 9 destinos; verificar que CI compila

## 2. Elementos compartidos

- [ ] 2.1 Aplicar `sharedNavBounds("home-<ruta>")` a los botones de Inicio y a los encabezados de las 8 pantallas destino; verificar la transición en el APK
- [ ] 2.2 Crear la ruta `catalog/{exerciseId}` en `MainActivity.kt` y el wiring de `onOpenDetail`; verificar que CI compila
- [ ] 2.3 Implementar `ExerciseDetailScreen.kt` y `ExerciseDetailViewModel.kt` con encabezado, PROPIO, tipo, grupos y equipamiento; verificar que CI compila
- [ ] 2.4 Retirar el `AlertDialog` de detalle de `CatalogScreen.kt`, navegar al detalle desde la tarjeta y aplicar `sharedNavBounds`/`sharedNavElement` a tarjeta y nombre; verificar la transición y la vuelta con filtros y scroll en el APK

## 3. Formas que mutan

- [ ] 3.1 Crear `ui/motion/MorphShapes.kt` con las formas base (píldora, cookie, play, stop) y `MorphPolygonShape`; verificar que CI compila
- [ ] 3.2 Crear `ui/motion/MorphActionButton.kt` y usarlo en `WorkoutScreen.kt` para Iniciar/Finalizar con mutación de contenedor e icono; verificar el cambio de estado en el APK

## 4. Blobs y estados vacíos

- [ ] 4.1 Crear `ui/motion/MorphingBlob.kt` con polígonos deterministas, animación infinita y gating por `MotionDurationScale`; verificar que CI compila
- [ ] 4.2 Crear `ui/motion/EmptyState.kt` y reemplazar los estados vacíos de catálogo, entrenamientos, rutinas, progreso, medidas, tips y comparativas; verificar que CI compila
- [ ] 4.3 Aplicar blobs al hero de Inicio y al fondo de la tarjeta de estado y del resumen de sesión; verificar el resultado visual en el APK

## 5. Verificación y cierre

- [ ] 5.1 Dejar `android.yml` en verde (compilación y tests) tras el push a `main`
- [ ] 5.2 Probar en el APK: transiciones Inicio→pantalla y Catálogo→detalle, vuelta con filtros, morph Iniciar/Finalizar, blobs en hero y vacíos, y app usable con animaciones del sistema en cero
- [ ] 5.3 Archivar el cambio y publicar la release `v0.1.9` con el flujo de la convención (tag anotado y `release.yml`)
