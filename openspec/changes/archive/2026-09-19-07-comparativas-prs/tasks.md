## 1. Vectores y dominio

- [x] 1.1 Crear `shared/test-vectors/comparisons.json` con casos de PRs (incluye empates, calentamientos, series incompletas), comparación de periodos (incluye periodo anterior vacío) y balance muscular
- [x] 1.2 Implementar PRs, comparación de periodos, rangos contiguos y balance muscular en el dominio de Web con test contra los vectores
- [x] 1.3 Implementar las mismas funciones en el dominio de Android con test contra los vectores

## 2. Datos (Web)

- [x] 2.1 Consulta de series efectivas con fecha de sesión y grupo muscular del ejercicio
- [x] 2.2 Tests con `node:sqlite`: exclusión de calentamientos/eliminadas, agrupación por ejercicio, periodo y grupo

## 3. Datos (Android)

- [x] 3.1 Consulta Room de series efectivas con fecha de sesión y grupo del ejercicio
- [x] 3.2 Tests Robolectric del repositorio de comparativas

## 4. UI

- [x] 4.1 Web: sección "Comparativas" con listado de PRs, comparación de periodos (30/90 días) y barras de balance muscular
- [x] 4.2 Web: estados vacíos (sin PRs, sin datos en el periodo) y manejo de "sin datos" en los cambios
- [x] 4.3 Android: pantalla "Comparativas" con las mismas capacidades y navegación desde la pantalla inicial

## 5. Cierre

- [x] 5.1 CI verde en Android y Web con los tests nuevos
- [x] 5.2 Publicar release `v0.1.4` (versionCode 5) con comparativas y PRs
- [x] 5.3 `openspec validate 07-comparativas-prs` y `openspec archive` del cambio
