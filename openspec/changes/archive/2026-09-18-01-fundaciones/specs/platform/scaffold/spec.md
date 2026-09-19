## Purpose

Establece la base del monorepo FitLog: estructura de carpetas, proyectos Android y Web arrancables, y pipelines de CI que compilan, prueban y publican ambas plataformas de forma reproducible.

## ADDED Requirements

### Requirement: Estructura del monorepo

El repositorio SHALL organizarse con `android/` (cliente Android), `web/` (cliente web), `shared/` (esquema y casos de prueba comunes), `openspec/` (specs y cambios) y `docs/` (documentación y prototipos históricos).

#### Scenario: Un desarrollador clona el repositorio

- **WHEN** se clona el repositorio y se listan los directorios de la raíz
- **THEN** existen `android/`, `web/`, `shared/`, `openspec/`, `docs/` y `.github/workflows/`

#### Scenario: Archivos generados no se versionan

- **WHEN** se ejecuta `git status` tras un build local de la web
- **THEN** `node_modules/`, `dist/` y `.gradle/` aparecen ignorados por `.gitignore`

### Requirement: Cliente Android arrancable

El proyecto Android SHALL compilar una app Jetpack Compose con Material 3 que muestre una pantalla inicial "FitLog" y SHALL declarar sus versiones de herramientas en `gradle/libs.versions.toml`.

#### Scenario: Build de depuración exitoso

- **WHEN** se ejecuta `./gradlew :app:assembleDebug` en `android/`
- **THEN** el build termina con éxito y produce `app/build/outputs/apk/debug/app-debug.apk`

#### Scenario: Versiones centralizadas

- **WHEN** se busca la versión de Kotlin, Compose o Room en el proyecto
- **THEN** está declarada únicamente en `android/gradle/libs.versions.toml`

### Requirement: Cliente Web arrancable

El proyecto Web SHALL compilar con Vite y TypeScript estricto, mostrando una página inicial "FitLog" con Tailwind, y SHALL inicializar SQLite WASM con OPFS en un Web Worker.

#### Scenario: Build de producción exitoso

- **WHEN** se ejecuta `npm ci && npm run build` en `web/`
- **THEN** el build termina sin errores de TypeScript y genera `web/dist/index.html` con assets

#### Scenario: Base de datos web disponible

- **WHEN** la app web arranca en un navegador compatible y se consulta el estado de la conexión
- **THEN** la app reporta la base SQLite WASM inicializada con persistencia OPFS

### Requirement: Integración continua de ambas plataformas

El repositorio SHALL incluir workflows de GitHub Actions que en cada push y pull request compilen y prueben Android y Web, publiquen el APK de depuración como artifact, y en `main` desplieguen la web en GitHub Pages.

#### Scenario: Pull request verificado

- **WHEN** se abre un pull request que modifica `android/` o `web/`
- **THEN** los workflows correspondientes corren y el PR muestra el resultado de los tests de la plataforma afectada

#### Scenario: APK descargable

- **WHEN** el workflow de Android termina con éxito en `main`
- **THEN** el APK de depuración queda disponible como artifact del workflow

#### Scenario: Web publicada

- **WHEN** los workflows terminan con éxito en `main`
- **THEN** la web de FitLog queda accesible en la URL de GitHub Pages del repositorio
