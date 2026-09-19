## Why

El proyecto quedó fijado en herramientas de 2024/2025: Compose BOM `2024.12.01` (Compose 1.7.6, Material3 1.3.1), AGP 8.7.3, Gradle 8.11.1, Kotlin 2.1.0 y `compileSdk`/`targetSdk` 35. La BOM estable más reciente publicada es `2026.09.00` (Compose 1.12.1, Material3 1.4.0), y las librerías de Compose 1.12 no se pueden consumir con el toolchain actual: el `aar-metadata` de `androidx.compose.ui:ui-android:1.12.1` y `androidx.compose.foundation:foundation-android:1.12.1` exige `minCompileSdk=37` y `minAndroidGradlePluginVersion=9.1.0`.

El cambio `12-motion-ui` dejó esta migración registrada como pendiente en `docs/pendiente-compose-bom-1.12.md` y renunció a `MaterialShapes` y `MeshGradientPainter` por no poder subir el BOM. Sin esta actualización el proyecto no puede adoptar nada de Compose 1.12 ni recibir correcciones de las librerías.

## What Changes

- Subir el Compose BOM a `2026.09.00` (Compose 1.12.1, Material3 1.4.0), la versión estable más actual.
- Subir AGP a `9.4.1` y Gradle a `9.7.1` (AGP 9.4 exige Gradle ≥ 9.6.0 y soporta hasta API 37).
- Adoptar el Kotlin integrado de AGP 9: quitar el plugin `org.jetbrains.kotlin.android` del módulo, del build raíz y del version catalog; Kotlin/KGP queda en `2.4.20` a través del plugin de Compose, que es el único plugin de Kotlin que queda declarado.
- Subir KSP a `2.3.12` (esquema de versiones desacoplado de Kotlin) y el resto de dependencias a la última estable: Hilt `2.60.1`, Room `2.8.5`, Robolectric `4.17`, core-ktx `1.19.0`, lifecycle `2.11.0`, activity-compose `1.13.0`, navigation-compose `2.10.1`, hilt-navigation-compose `1.4.0`, coroutines `1.11.0`, kotlinx-serialization `1.11.0`, androidx.test:core `1.7.0`, graphics-shapes `1.1.0`, org.json `20260814`.
- Subir `compileSdk` y `targetSdk` a 37 y ajustar CI: `platforms;android-37`, `build-tools;36.0.0` (mínimo de AGP 9.4) y Gradle 9.7.1 en `android.yml` y `release.yml`.
- **Non-goals**: adoptar `MaterialShapes`, `MeshGradientPainter` o el Material3 expresivo (queda para un cambio posterior, como ya estaba previsto), cambiar el comportamiento visible de la app, tocar la web o el esquema de datos, y subir el JDK del CI por encima de 17.

## Capabilities

### New Capabilities

<!-- Ninguna: la migración no agrega capacidades de producto. -->

### Modified Capabilities

- `platform/scaffold`: se agrega el requirement de cadena de herramientas Android actualizada (versiones mínimas de BOM, AGP, Gradle, Kotlin/KSP y SDK) con su verificación en CI.

## Impact

- Android: `android/gradle/libs.versions.toml`, `android/build.gradle.kts`, `android/app/build.gradle.kts`.
- CI: `.github/workflows/android.yml` y `.github/workflows/release.yml` (SDK 37, build-tools 36, Gradle 9.7.1).
- Documentación: `README.md` (requisitos de build local) y `docs/pendiente-compose-bom-1.12.md` (pasa a registro de la migración hecha).
- Datos: sin cambios de esquema, seed ni vectores compartidos.
- Riesgo: AGP 9 estrena DSL nuevo y Kotlin integrado; los plugins de terceros (Hilt 2.60.1, KSP 2.3.12) son los que declaran soporte de AGP 9. La red de seguridad es `android.yml`: tests unitarios + APK de depuración.
