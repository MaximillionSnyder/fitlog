## ADDED Requirements

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
