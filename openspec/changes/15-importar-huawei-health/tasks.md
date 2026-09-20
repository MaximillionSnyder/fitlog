## 1. Lector de la exportación

- [x] 1.1 Crear `domain/HuaweiHealth.kt` con el modelo del entrenamiento importado y el lector tolerante de JSON; verificar que CI compila
- [x] 1.2 Cubrir con tests: registros válidos, repetidos por `recordId`, sin fecha, con duración en milisegundos y archivos que no son entrenamientos
- [x] 1.3 Crear el equivalente web `web/src/domain/huaweiHealth.ts` con los mismos tests

## 2. Importación

- [x] 2.1 Sumar `importSessions` a `WorkoutRepository` (Android) con salteo por fecha de inicio ya existente; verificar con test
- [x] 2.2 Sumar el equivalente en `web/src/data/workout.ts`; verificar con test
- [x] 2.3 Armar la nota de cada sesión importada con tipo, distancia, calorías y frecuencia cardíaca

## 3. Pantalla de importación

- [x] 3.1 Crear `ui/ImportViewModel.kt` con los estados (sin archivos, leyendo, vista previa, importando, resultado) y la lectura de la carpeta elegida
- [x] 3.2 Crear `ui/ImportScreen.kt` con el selector de carpeta, la vista previa y el resumen, usando el sistema de diseño
- [x] 3.3 Sumar la ruta en `MainActivity.kt` y la entrada en `Más`
- [x] 3.4 Crear la vista web equivalente (`ImportView.tsx`) con selector de carpeta y la misma vista previa

## 3b. Datos que la exportación no resume

- [x] 3b.1 Derivar la frecuencia cardíaca promedio y máxima del blob de sensores (`tp=h-r`), con tests en Android y web
- [x] 3b.2 Aceptar el ZIP de la exportación sin descomprimir en Android (`ZipInputStream`, sin dependencias)

## 3c. Pruebas de punta a punta

- [x] 3c.1 Exportación de prueba (registros triplicados, sueño y pasos por minuto) que recorre leer → importar → historial, en Android y web
- [x] 3c.2 Un archivo por minuto con calorías pero sin duración ya no se confunde con una sesión
- [x] 3c.3 Marca `IMPORTADO` en el historial de las sesiones que vinieron de una importación

## 4. Verificación y cierre

- [x] 4.1 Dejar `android.yml` y `web.yml` en verde (tests y build)
- [ ] 4.2 Probar en el APK con una exportación real: elegir la carpeta, ver la vista previa, importar y comprobar el historial
- [ ] 4.3 Verificar que una segunda importación del mismo archivo no duplique sesiones
- [ ] 4.4 Archivar el cambio y actualizar `openspec/specs`
