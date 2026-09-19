# platform/scaffold Specification

## Purpose
Establece la base del monorepo FitLog: estructura de carpetas, proyectos Android y Web arrancables, y pipelines de CI que compilan, prueban y publican ambas plataformas de forma reproducible.

## Requirements

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

### Requirement: Publicación de releases de Android

El repositorio SHALL publicar una GitHub Release al empujar un tag que empiece con `v`, con el APK de release adjunto firmado con el keystore propio del proyecto, y SHALL derivar `versionName` del tag y `versionCode` del historial de commits.

#### Scenario: Tag de versión dispara la release

- **WHEN** se empuja el tag `v0.1.7`
- **THEN** el workflow `release.yml` corre, compila el APK de release, lo firma con el keystore de los secretos y crea la Release con el APK adjunto

#### Scenario: Versión coherente con el tag

- **WHEN** se instala el APK de la Release `v0.1.7`
- **THEN** el sistema reporta la versión `0.1.7` de la aplicación

#### Scenario: Tests antes de publicar

- **WHEN** el workflow de release se ejecuta
- **THEN** corre los tests unitarios de Android y no publica la Release si fallan

#### Scenario: APK instalable

- **WHEN** se descarga el APK adjunto a la Release y se instala en un dispositivo Android 8 o superior
- **THEN** la instalación finaliza correctamente y la app abre mostrando la pantalla inicial de FitLog

#### Scenario: Firma con keystore propio

- **WHEN** el keystore existe en el runner porque el workflow decodificó `KEYSTORE_BASE64`
- **THEN** el APK de release se firma con esa clave y no con la clave de depuración

#### Scenario: Firma de depuración como respaldo

- **WHEN** el keystore no está disponible (por ejemplo en un build local)
- **THEN** el build de release no se firma con la clave de depuración y la aplicación avisa en el README que ese APK no es distribuible

#### Scenario: Trazabilidad de firma

- **WHEN** el APK se firma con el keystore propio
- **THEN** la Release indica la huella de la clave de firma para poder verificarla

### Requirement: Cadena de herramientas Android actualizada

El cliente Android SHALL usar la Compose BOM estable más reciente y un toolchain compatible con ella, con todas las versiones declaradas únicamente en `android/gradle/libs.versions.toml`, y SHALL compilar con AGP 9 o superior usando el Kotlin integrado (sin el plugin `org.jetbrains.kotlin.android`).

#### Scenario: Compose BOM al día

- **WHEN** se consulta la versión de `androidx.compose:compose-bom` en el version catalog
- **THEN** es la última estable publicada y las librerías de Compose resuelven a la línea 1.12 o superior

#### Scenario: Requisitos de la BOM satisfechos

- **WHEN** Gradle resuelve `androidx.compose.ui:ui-android` de la línea 1.12
- **THEN** el build compila con `compileSdk` 37 y AGP 9.4.1, que cumplen el `minCompileSdk` y el `minAndroidGradlePluginVersion` del artefacto

#### Scenario: Kotlin integrado

- **WHEN** se busca el plugin `org.jetbrains.kotlin.android` en los build files y en el version catalog
- **THEN** no aparece, y el proyecto compila Kotlin con el soporte integrado de AGP y el plugin de Compose

#### Scenario: Versiones centralizadas y verificadas en CI

- **WHEN** se abre un pull request que toca `android/`
- **THEN** `android.yml` corre los tests unitarios (Room + KSP + Robolectric) y `assembleDebug` con SDK 37, build-tools 36 y Gradle 9.7.1, y termina en verde

#### Scenario: APK del toolchain nuevo

- **WHEN** el workflow de Android termina con éxito
- **THEN** el artifact `fitlog-debug-apk` contiene un APK compilado con la BOM actualizada que instala en Android 8 o superior
