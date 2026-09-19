# Pendiente: subir Compose BOM a 1.12 (2026.08.00)

**Estado:** pendiente, no iniciado
**Fecha de registro:** 2026-09-19
**Contexto:** al implementar el cambio `12-motion-ui` se evaluó subir el BOM y se decidió posponerlo para no mezclar la migración de toolchain con la mejora de UI.

## Qué hay que subir

| | Actual | Objetivo |
|---|---|---|
| Compose BOM | `2024.12.01` (Compose 1.7.6, Material3 1.3.1) | `2026.08.00` (Compose 1.12, Material3 1.4.0) |
| compileSdk | 35 | 37 |
| AGP | 8.7.3 | 9.2.0 (las notas de Material3 piden ≥ 9.2.0; el blog de Compose dice ≥ 9.1.1) |
| Gradle | 8.11.1 | ≥ 9.3.1 (mínimo de AGP 9.1) |
| Kotlin | 2.1.0 | 2.2/2.3 |
| KSP | 2.1.0-1.0.29 | a juego con Kotlin |
| Hilt | 2.53.1 | 2.59.2 (2.53.1 no soporta AGP 9) |
| Room | 2.6.1 | revisar compatibilidad con KSP nuevo |
| Robolectric | 4.14.1 | versión con soporte de SDK 37 (o fijar `@Config(sdk = 35)` en los tests) |

## Requisitos del salto (breaking)

- **compileSdk 37 + AGP ≥ 9.1.2** (Compose 1.12 siempre compila contra el último SDK).
- **AGP 9 usa Kotlin integrado**: hay que quitar el plugin `org.jetbrains.kotlin.android` del módulo y del version catalog.
- **Gradle ≥ 9.3.1** y JDK 17+ (el CI ya usa JDK 17).
- **CI**: `android.yml` y `release.yml` instalan a mano `platforms;android-35`, `build-tools;35.0.0` y Gradle 8.11.1 → pasar a SDK 37, build-tools 36 y Gradle 9.x.
- `Modifier.onFirstVisible()` queda deprecado (no se usa en el proyecto).

## Qué desbloquea

- `MaterialShapes` y `toShape()` de material3 1.4: formas Material listas (Cookie, Clover, Burst, …) en lugar de definir polígonos a mano.
- `MeshGradientPainter` (nuevo en Compose 1.12): gradientes mesh ideales para los blobs orgánicos.
- Mejoras de shared elements (transiciones diferidas, herramientas de debug).
- Material3 1.4 expresivo (componentes y motion nuevos).

## Plan de migración sugerido

1. **Toolchain primero, BOM después**: subir AGP 9.2 + Gradle 9.3.1 + Kotlin/KSP/Hilt/Room/Robolectric y migrar a Kotlin integrado, manteniendo el BOM actual; dejar `android.yml` en verde.
2. Subir el BOM a `2026.08.00` y `compileSdk`/`targetSdk` a 37; ajustar CI (SDK 37, build-tools 36); dejar `android.yml` y `release.yml` en verde.
3. Adoptar `MaterialShapes` y `MeshGradientPainter` en la capa de motion (`ui/motion/`) en un cambio aparte.

## Verificación

- `android.yml` (tests + APK debug) y `release.yml` (firma + Release) en verde.
- `gradle :app:testDebugUnitTest` con Robolectric y Room funcionando en SDK 37.
- Prueba manual del APK: navegación, catálogo, entrenamiento y medidas sin regresiones.

## Referencias

- Compose BOM 2026.08.00: https://developer.android.com/develop/ui/compose/bom
- Notas del release de agosto 2026: https://developer.android.com/blog/posts/what-s-new-in-the-jetpack-compose-august-26-release
- AGP 9.1: https://developer.android.com/build/releases/agp-9-1-0-release-notes
- Kotlin integrado en AGP 9: https://developer.android.com/build/migrate-to-built-in-kotlin
