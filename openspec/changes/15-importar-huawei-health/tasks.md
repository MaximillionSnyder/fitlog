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

## 3d. Archivos GPX

- [x] 3d.1 Modelo compartido `ImportedWorkout` para las dos fuentes, con la nota de origen
- [x] 3d.2 Lector de GPX propio: inicio, fin, duración, distancia por semiverseno, frecuencia cardíaca y desnivel
- [x] 3d.3 Tipo de deporte desde `<type>`, `<name>` o el nombre del archivo; origen Huawei si el `creator` lo dice
- [x] 3d.4 La pantalla acepta carpeta, ZIP o GPX, y se pueden mezclar en una misma importación
- [x] 3d.5 Tests del lector, de la nota y de los casos sin tiempos o sin frecuencia cardíaca

## 3e. Tolerancia a archivos reales

- [x] 3e.1 Leer archivos con marca de orden de bytes (BOM), que rompía el lector de JSON
- [x] 3e.2 Importar un GPX sin tiempos por punto usando la fecha del metadata, sin duración
- [x] 3e.3 Un GPX con una sola marca de tiempo tampoco alcanza: se usa el metadata o se descarta

## 3f. Cerrar el circuito

- [x] 3f.1 El historial muestra los datos de la nota en las sesiones importadas (sin "0 series · 0 kg")
- [x] 3f.2 La vista previa informa los archivos descartados, para diagnosticar un export inesperado
- [x] 3f.3 Acción "Ver el historial" al terminar la importación

## 3g. Métricas estructuradas

- [x] 3g.1 Migración 002 con distancia, calorías, frecuencia cardíaca, pasos, desnivel y origen en `session` (Android y web)
- [x] 3g.2 La importación guarda las métricas; una sesión propia las deja en NULL
- [x] 3g.3 El detalle muestra la sección Actividad y el historial usa los datos estructurados
- [x] 3g.4 Paridad de esquema y respaldo siguen en verde (el respaldo copia las filas completas)

## 3h. Gráficos de actividad

- [x] 3h.1 Dominio `Activity` con serie por rango, métrica elegida y totales, con tests
- [x] 3h.2 Progreso suma la sección "Actividad importada": chips de métrica, tiles de totales, gráfico y detalle
- [x] 3h.3 Replicar la sección en la web

## 3i. Compatibilidad con lo ya importado

- [x] 3i.1 Leer las métricas desde la nota cuando las columnas están vacías (Android y web)
- [x] 3i.2 Tests de ida y vuelta nota → métricas y del caso de una sesión importada por una versión anterior

## 4. Verificación y cierre

- [x] 4.1 Dejar `android.yml` y `web.yml` en verde (tests y build)
- [x] 4.2 Probar con los archivos reales del usuario: importados y visibles en el historial, el detalle y los gráficos
- [ ] 4.3 Verificar que una segunda importación del mismo archivo no duplique sesiones
- [ ] 4.4 Archivar el cambio y actualizar `openspec/specs`
