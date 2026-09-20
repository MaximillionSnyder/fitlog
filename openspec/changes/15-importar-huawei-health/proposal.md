## Why

El usuario registra entrenamientos en Huawei Health desde hace tiempo y quiere traerlos a FitLog: hoy esa historia no existe en la app, así que el panel, el historial y las estadísticas arrancan vacíos aunque la persona entrene desde hace años. Huawei Health permite descargar una exportación completa de datos (privacidad → solicitar tus datos), que llega como un ZIP con carpetas de JSON; la carpeta `Motion path detail data & description` trae un JSON por entrenamiento grabado.

El usuario pidió además que, si hace falta, se arme una ventana nueva para hacerlo.

## What Changes

- **Nueva pantalla "Importar entrenamientos"** (Android y web), dentro de "Más": se elige la carpeta o los archivos JSON de la exportación de Huawei Health, la app los lee, muestra una **vista previa** de lo que encontró (cuántos entrenamientos, rango de fechas, tipos y cuántos ya están en FitLog) y recién entonces importa, con un resumen del resultado.
- **También acepta GPX**: además de la exportación de Huawei Health, la pantalla lee archivos GPX (los que exporta el reloj), de los que saca inicio, fin, duración, distancia, frecuencia cardíaca y desnivel. Se pueden importar los dos formatos juntos.
- **Lector tolerante de la exportación**: interpreta los registros de actividad de Huawei Health (`recordId`, `startTime`, `endTime`, `sportType`, `totalTime`, `totalCalories`, `totalDistance`, `totalSteps`, `avgHeartRate`/`maxHeartRate`), con las escalas que usa la exportación, sin depender del nombre del archivo ni del idioma de la carpeta. Los archivos que no son de entrenamientos se ignoran sin error.
- **Los entrenamientos entran como sesiones**: cada registro se convierte en una sesión de FitLog con su fecha, duración real y una nota con el tipo de actividad, la distancia, las calorías y la frecuencia cardíaca cuando existen. Huawei Health no exporta series con peso y reps, así que esas sesiones quedan sin series y se pueden completar después.
- **Importación idempotente**: se saltea todo entrenamiento cuya fecha de inicio ya exista en FitLog, así repetir la importación (o importar un export más nuevo) no duplica nada.
- **Non-goals**: no se importan series con peso/reps (ninguna de las fuentes las trae), no se guardan las rutas GPS ni mapas, no se tocan sueño, pasos, estrés ni peso corporal (queda para otra etapa), no se agregan dependencias nuevas, no se cambia el esquema de datos ni el formato de respaldo.

## Capabilities

### New Capabilities

- `health/huawei-import`: lectura de la exportación de Huawei Health, conversión de sus registros de actividad a sesiones de FitLog, vista previa antes de importar e importación idempotente en Android y web.

### Modified Capabilities

<!-- Ninguna: el esquema, el respaldo y las fórmulas de dominio no cambian. -->

## Impact

- Android: nuevos `domain/HuaweiHealth.kt` (lector puro), `ui/ImportScreen.kt` y `ui/ImportViewModel.kt`; `data/WorkoutRepository.kt` suma la importación de sesiones con fecha explícita; `MainActivity.kt` y `ui/destinations/FitLogDestination.kt` suman la ruta; `ui/MoreScreen.kt` la entrada.
- Web: nuevos `domain/huaweiHealth.ts`, `ui/ImportView.tsx` y el estado asociado; `data/workout.ts` suma la importación; `App.tsx` y `ui/destinations.tsx` la vista.
- Datos: sin migraciones ni cambios de esquema. Las sesiones importadas usan las columnas existentes (`started_at`, `finished_at`, `notes`).
- Tests: vectores propios del lector en ambas plataformas (registros válidos, repetidos, incompletos y archivos ajenos) y pruebas de la importación idempotente.
