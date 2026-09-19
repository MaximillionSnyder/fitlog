## 1. Version catalog y build files

- [x] 1.1 Subir en `android/gradle/libs.versions.toml` el BOM a `2026.09.00`, AGP a `9.4.1`, Kotlin a `2.4.20`, KSP a `2.3.12` y el resto de dependencias a su última estable; quitar la entrada `kotlin-android` de `[plugins]`
- [x] 1.2 Quitar el plugin `org.jetbrains.kotlin.android` de `android/build.gradle.kts` y de `android/app/build.gradle.kts`
- [x] 1.3 Subir `compileSdk` y `targetSdk` a 37 y eliminar el bloque `kotlin { compilerOptions { jvmTarget } }` (Kotlin integrado lo deriva de `compileOptions`)
- [x] 1.4 Verificar que ninguna versión quedó declarada fuera del version catalog (`grep` de versiones en los `.kts`)

## 2. CI

- [x] 2.1 Actualizar `.github/workflows/android.yml`: `platforms;android-37.0`, `build-tools;36.0.0` y Gradle 9.7.1
- [x] 2.2 Aplicar los mismos números en `.github/workflows/release.yml`
- [x] 2.3 Actualizar `README.md` con los requisitos de build local (SDK 37, Gradle 9.7.1)

## 3. Verificación

- [x] 3.1 Empujar la rama y abrir PR; `android.yml` en verde con tests unitarios (Room + KSP + Robolectric) y APK de depuración (run 35437775860)
- [x] 3.2 Confirmar que el APK resuelve la BOM nueva: contiene `MeshGradientPainter` (clase exclusiva de Compose 1.12)
- [x] 3.3 Descargar el APK de depuración del workflow y verificar que es un zip válido con `classes.dex` y el manifest de FitLog

## 4. Cierre

- [x] 4.1 Actualizar `docs/pendiente-compose-bom-1.12.md` como registro de la migración completada
- [x] 4.2 Validar el cambio con `openspec validate 13-compose-bom-2026-09-00`
- [x] 4.3 Fusionar el PR a `main` y dejar `android.yml` en verde en `main` (run 35438026997)
- [ ] 4.4 Instalar el APK de depuración en un dispositivo Android 8 o superior y probar navegación, catálogo, entrenamiento y medidas
- [x] 4.5 Publicar la release con el flujo de la convención: tag anotado `v0.1.10` y `release.yml` en verde (run 35438368718)
- [x] 4.6 Archivar el cambio con `openspec archive 13-compose-bom-2026-09-00`
