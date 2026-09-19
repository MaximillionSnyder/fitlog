## 1. Version catalog y build files

- [ ] 1.1 Subir en `android/gradle/libs.versions.toml` el BOM a `2026.09.00`, AGP a `9.4.1`, Kotlin a `2.4.20`, KSP a `2.3.12` y el resto de dependencias a su última estable; quitar la entrada `kotlin-android` de `[plugins]`
- [ ] 1.2 Quitar el plugin `org.jetbrains.kotlin.android` de `android/build.gradle.kts` y de `android/app/build.gradle.kts`
- [ ] 1.3 Subir `compileSdk` y `targetSdk` a 37 y eliminar el bloque `kotlin { compilerOptions { jvmTarget } }` (Kotlin integrado lo deriva de `compileOptions`)
- [ ] 1.4 Verificar que ninguna versión quedó declarada fuera del version catalog (`grep` de versiones en los `.kts`)

## 2. CI

- [ ] 2.1 Actualizar `.github/workflows/android.yml`: `platforms;android-37`, `build-tools;36.0.0` y Gradle 9.7.1
- [ ] 2.2 Aplicar los mismos números en `.github/workflows/release.yml`
- [ ] 2.3 Actualizar `README.md` con los requisitos de build local (SDK 37, Gradle 9.7.1)

## 3. Verificación

- [ ] 3.1 Empujar la rama y abrir PR; `android.yml` en verde con tests unitarios (Room + KSP + Robolectric) y APK de depuración
- [ ] 3.2 Confirmar en el log del workflow las versiones efectivas (Compose 1.12.1, AGP 9.4.1, Kotlin 2.4.20)
- [ ] 3.3 Descargar el APK de depuración del workflow y comprobar que instala en Android 8 o superior

## 4. Cierre

- [ ] 4.1 Actualizar `docs/pendiente-compose-bom-1.12.md` como registro de la migración completada
- [ ] 4.2 Validar el cambio con `openspec validate 13-compose-bom-2026-09-00` y archivar el cambio
- [ ] 4.3 Publicar la release con el flujo de la convención (tag anotado y `release.yml`) cuando el APK quede verificado
