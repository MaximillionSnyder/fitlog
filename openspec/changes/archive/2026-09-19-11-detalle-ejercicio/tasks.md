## 1. Android

- [x] 1.1 Hacer clickeable la tarjeta de ejercicio en `CatalogScreen.kt` (overload `onClick` de `Card`), guardando el ejercicio seleccionado en estado local; verificar que compila en CI (`android.yml`)
- [x] 1.2 Implementar `ExerciseDetailDialog` con nombre, marca `PROPIO` si `isCustom`, tipo (`kindLabel`), grupo principal, grupo secundario solo si existe y equipamiento; verificar que compila en CI
- [x] 1.3 Confirmar que el botón "Eliminar" de la tarjeta sigue abriendo el diálogo de borrado y no el detalle (revisión manual en APK de CI)

## 2. Web

- [x] 2.1 Convertir la zona de información de la tarjeta en `CatalogView.tsx` en un `<button>` que abre el detalle, dejando "Eliminar" como botón hermano sin propagación; verificar con `npm run lint`
- [x] 2.2 Implementar el panel de detalle inline inmediatamente después de la tarjeta seleccionada, mostrando los mismos campos que Android y cierre con botón "Cerrar"; verificar con `npm run typecheck`
- [x] 2.3 Verificar el comportamiento con `npm run dev`: abrir detalle de un ejercicio base, uno propio y uno sin grupo secundario, y confirmar que búsqueda y filtros se conservan al cerrar

## 3. Verificación

- [x] 3.1 Ejecutar `npm run lint`, `npm run typecheck` y `npm run test` en `web/` sin errores
- [x] 3.2 Dejar CI en verde: `web.yml` (tests, build y deploy) y `android.yml` (tests y APK debug) tras el push a `main`
