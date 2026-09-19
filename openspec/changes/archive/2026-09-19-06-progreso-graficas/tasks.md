## 1. Vectores y dominio

- [x] 1.1 Crear `shared/test-vectors/progress-series.json` con casos de puntos por sesión, calentamientos, series incompletas, orden y filtrado por rango
- [x] 1.2 Implementar la serie de progreso y el cálculo de rangos en el dominio de Web con test contra los vectores
- [x] 1.3 Implementar la serie de progreso y el cálculo de rangos en el dominio de Android con test contra los vectores

## 2. Datos (Web)

- [x] 2.1 Consulta de series efectivas por ejercicio con `started_at` de la sesión, ordenadas por fecha
- [x] 2.2 Tests con `node:sqlite`: exclusión de calentamientos y eliminadas, agrupación por sesión y orden

## 3. Datos (Android)

- [x] 3.1 Consulta Room de series efectivas por ejercicio con `started_at` de la sesión
- [x] 3.2 Tests Robolectric de la consulta y del repositorio de progreso

## 4. UI

- [x] 4.1 Web: sección "Progreso" con selector de ejercicio, selector de métrica, rango 30/90/todo, gráfica de líneas (Recharts) y valor del último punto
- [x] 4.2 Web: estados vacíos por ejercicio sin datos y por rango sin datos
- [x] 4.3 Android: pantalla "Progreso" con las mismas capacidades, gráfica dibujada con Compose Canvas y navegación desde la pantalla inicial

## 5. Cierre

- [x] 5.1 CI verde en Android y Web con los tests nuevos
- [x] 5.2 Publicar release `v0.1.3` (versionCode 4) con gráficas de progreso
- [x] 5.3 `openspec validate 06-progreso-graficas` y `openspec archive` del cambio
