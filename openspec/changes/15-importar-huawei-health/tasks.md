## 1. Lector de la exportación

- [ ] 1.1 Crear `domain/HuaweiHealth.kt` con el modelo del entrenamiento importado y el lector tolerante de JSON; verificar que CI compila
- [ ] 1.2 Cubrir con tests: registros válidos, repetidos por `recordId`, sin fecha, con duración en milisegundos y archivos que no son entrenamientos
- [ ] 1.3 Crear el equivalente web `web/src/domain/huaweiHealth.ts` con los mismos tests

## 2. Importación

- [ ] 2.1 Sumar `importSessions` a `WorkoutRepository` (Android) con salteo por fecha de inicio ya existente; verificar con test
- [ ] 2.2 Sumar el equivalente en `web/src/data/workout.ts`; verificar con test
- [ ] 2.3 Armar la nota de cada sesión importada con tipo, distancia, calorías y frecuencia cardíaca

## 3. Pantalla de importación

- [ ] 3.1 Crear `ui/ImportViewModel.kt` con los estados (sin archivos, leyendo, vista previa, importando, resultado) y la lectura de la carpeta elegida
- [ ] 3.2 Crear `ui/ImportScreen.kt` con el selector de carpeta, la vista previa y el resumen, usando el sistema de diseño
- [ ] 3.3 Sumar la ruta en `MainActivity.kt` y la entrada en `Más`
- [ ] 3.4 Crear la vista web equivalente (`ImportView.tsx`) con selector de carpeta y la misma vista previa

## 4. Verificación y cierre

- [ ] 4.1 Dejar `android.yml` y `web.yml` en verde (tests y build)
- [ ] 4.2 Probar en el APK con una exportación real: elegir la carpeta, ver la vista previa, importar y comprobar el historial
- [ ] 4.3 Verificar que una segunda importación del mismo archivo no duplique sesiones
- [ ] 4.4 Archivar el cambio y actualizar `openspec/specs`
