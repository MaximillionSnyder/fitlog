## Context

Ver `proposal.md` - Why. El proyecto compila Android exclusivamente en GitHub Actions (ubuntu-latest, JDK 17) porque el entorno local es Termux sin JDK ni SDK de Android, así que la verificación real de esta migración es el workflow `android.yml`.

Las versiones objetivo se eligieron contra los metadatos publicados, no contra supuestos:

| Artefacto | Última estable | Requisito que impone |
|---|---|---|
| `androidx.compose:compose-bom` | `2026.09.00` (Compose 1.12.1, Material3 1.4.0) | — |
| `androidx.compose.ui:ui-android` 1.12.1 | — | `minCompileSdk=37`, `minAndroidGradlePluginVersion=9.1.0` |
| `com.android.tools.build:gradle` | `9.4.1` | Gradle ≥ 9.6.0, build-tools ≥ 36.0.0, JDK ≥ 17, API máxima 37 |
| Gradle | `9.7.1` | — |
| Kotlin (KGP) | `2.4.20` | AGP 9.4 integra KGP 2.2.10 y permite subirlo |
| KSP | `2.3.12` | Versionado desacoplado de Kotlin desde KSP2 |

## Goals / Non-Goals

- **Goals**: consumir la BOM más actual, dejar el toolchain en AGP 9 + Kotlin integrado, `compileSdk`/`targetSdk` 37, y `android.yml`/`release.yml` en verde.
- **Non-Goals**: adoptar APIs nuevas de Compose 1.12 o Material3 1.4, cambiar comportamiento, tocar la web o los datos.

## Decisions

### D1. BOM `2026.09.00` y no `2026.08.00`

`docs/pendiente-compose-bom-1.12.md` fijaba `2026.08.00` (Compose 1.12.0). Al momento de migrar ya está publicada `2026.09.00` (Compose 1.12.1, Material3 1.4.0), que es la más actual y trae los parches de la línea 1.12 sin cambios de API respecto de lo planificado.

### D2. AGP 9.4.1 + Gradle 9.7.1

AGP 9.4 es la última estable, soporta API 37 (el techo del toolchain) y su mínimo de Gradle es 9.6.0; se fija 9.7.1, la última estable, para no quedar pegados al mínimo. El JDK del CI se mantiene en 17 porque es el mínimo y el default de AGP 9.4.

### D3. Kotlin integrado, con Kotlin 2.4.20

AGP 9 habilita el Kotlin integrado por defecto y el plugin `org.jetbrains.kotlin.android` ya no es compatible con el DSL nuevo: se elimina del módulo, del build raíz y del version catalog. AGP 9.4 trae KGP 2.2.10; declarar el plugin de Compose en `2.4.20` arrastra KGP 2.4.20 por resolución de dependencias del classpath de plugins, sin duplicar versiones fuera del version catalog (el requirement de `platform/scaffold` exige que las versiones vivan solo ahí). Se elimina el bloque `kotlin { compilerOptions { jvmTarget } }` del módulo porque con Kotlin integrado `jvmTarget` toma el valor de `compileOptions.targetCompatibility` (17).

### D4. Versiones no-BOM al día

La BOM solo gobierna las librerías de Compose. El resto se sube a la última estable para no quedar con pins viejos que Gradle terminaría subiendo igual por dependencias transitivas: core-ktx 1.19.0, lifecycle 2.11.0, activity-compose 1.13.0, navigation-compose 2.10.1, hilt-navigation-compose 1.4.0, Hilt 2.60.1 (necesario para AGP 9), Room 2.8.5, coroutines 1.11.0, kotlinx-serialization 1.11.0, Robolectric 4.17, androidx.test:core 1.7.0, graphics-shapes 1.1.0 y org.json 20260814.

Los tests fijan `@Config(sdk = [34])`, así que Robolectric no necesita el SDK 37 para correr; la subida a 4.17 es por compatibilidad con el toolchain nuevo.

### D5. Verificación solo en CI

En Termux no hay JDK ni SDK de Android, y publicar aapt2 para linux-arm64 no está soportado, así que el build local es inviable por diseño del proyecto. La migración se verifica en una rama con PR: `android.yml` corre tests unitarios (incluido Robolectric + Room con KSP) y `assembleDebug`. `release.yml` se actualiza con los mismos números pero solo se puede probar publicando un tag, así que se deja idéntico a `android.yml` en toolchain.

### D6. El paquete del SDK de API 37 lleva el minor en el id

Desde API 37 el repositorio del SDK nombra las plataformas con minor (`platforms;android-37.0` es "Android SDK Platform 17", `platforms;android-37.1`, `platforms;android-37.2`); no existe un `platforms;android-37` a secas y el primer intento de CI falló con `Failed to find package 'platforms;android-37'`. `compileSdk = 37` sin `compileSdkMinor` resuelve contra la plataforma `android-37.0`, así que el CI instala `platforms;android-37.0` junto con `build-tools;36.0.0` (el default de AGP 9.4).

## Risks / Trade-offs

- **AGP 9 DSL nuevo**: el script del módulo usa bloques estándar (`namespace`, `defaultConfig`, `signingConfigs`, `buildTypes`, `compileOptions`, `buildFeatures`, `testOptions`, `sourceSets`), que siguen existiendo; los cambios rotos de AGP 9 afectan a plugins que usan `applicationVariants` o `CommonExtension` parametrizado, que el proyecto no usa.
- **KGP 2.4.20 sobre AGP 9.4.1**: AGP documenta que se puede subir KGP; si apareciera una incompatibilidad, la caída natural es quedarse en el KGP que AGP integra (2.2.10) bajando `kotlin` en el version catalog.
- **KSP 2.3.12**: es la última publicada y su versionado ya no sigue a Kotlin; si rechazara KGP 2.4.20, la alternativa es 2.3.21/2.2.10 en Kotlin.
