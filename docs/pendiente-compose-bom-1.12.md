# Compose BOM 2026.09.00 (Compose 1.12.1): migración aplicada

**Estado:** aplicado y verificado en CI el 2026-09-19 (el nombre del archivo quedó de cuando era un pendiente)
**Contexto:** al implementar el cambio `12-motion-ui` se evaluó subir el BOM y se decidió posponerlo para no mezclar la migración de toolchain con la mejora de UI. La migración se hizo en el cambio `13-compose-bom-2026-09-00`.

## Resultado

| | Antes | Ahora |
|---|---|---|
| Compose BOM | `2024.12.01` (Compose 1.7.6, Material3 1.3.1) | `2026.09.00` (Compose 1.12.1, Material3 1.4.0) |
| compileSdk / targetSdk | 35 | 37 |
| AGP | 8.7.3 | 9.4.1 |
| Gradle | 8.11.1 | 9.7.1 |
| Kotlin | 2.1.0 (plugin `kotlin-android`) | 2.4.20 (Kotlin integrado de AGP 9) |
| KSP | 2.1.0-1.0.29 | 2.3.12 |
| Hilt | 2.53.1 | 2.60.1 |
| Room | 2.6.1 | 2.8.5 |
| Robolectric | 4.14.1 | 4.17 |
| CI | SDK 35, build-tools 35.0.0, Gradle 8.11.1 | SDK 37 (`platforms;android-37.0`), build-tools 36.0.0, Gradle 9.7.1 |

También se subieron core-ktx 1.19.0, lifecycle 2.11.0, activity-compose 1.13.0, navigation-compose 2.10.1, hilt-navigation-compose 1.4.0, coroutines 1.11.0, kotlinx-serialization 1.11.0, androidx.test:core 1.7.0, graphics-shapes 1.1.0 y org.json 20260814.

## Cómo se verificó

- `android.yml` en verde (run 35437775860): tests unitarios con Room + KSP + Robolectric y `assembleDebug`.
- El APK de depuración contiene `MeshGradientPainter` (clase que recién existe en Compose 1.12) y `SharedTransitionLayout`, lo que confirma que la BOM resolvió a la línea 1.12.
- Requisito que obligó el salto: el `aar-metadata` de `androidx.compose.ui:ui-android:1.12.1` pide `minCompileSdk=37` y `minAndroidGradlePluginVersion=9.1.0`.
- Detalle del SDK que costó un intento de CI: desde API 37 el id del paquete lleva el minor (`platforms;android-37.0` es "Android SDK Platform 17"); `platforms;android-37` no existe y el `sdkmanager` falla con `Failed to find package`.

## Qué queda

- Adoptar `MaterialShapes` y `MeshGradientPainter` en la capa de motion (`ui/motion/`), en un cambio aparte.
- Publicar una release con el toolchain nuevo (tag anotado, `release.yml`) cuando se quiera distribuir el APK.

## Referencias

- Compose BOM 2026.09.00: https://developer.android.com/develop/ui/compose/bom
- Notas del release de agosto 2026: https://developer.android.com/blog/posts/what-s-new-in-the-jetpack-compose-august-26-release
- AGP 9.4: https://developer.android.com/build/releases/gradle-plugin
- Kotlin integrado en AGP 9: https://developer.android.com/build/migrate-to-built-in-kotlin
