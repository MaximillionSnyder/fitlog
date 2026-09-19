## Why

Hoy el APK solo existe como artifact temporal de CI. Para instalar FitLog en un teléfono hace falta un canal de publicación con versiones descargables y trazables por tag.

## What Changes

- Añadir el workflow `release.yml` que al empujar un tag `v*` compila el APK de release, corre los tests y publica una GitHub Release con el APK adjunto.
- Firmar el APK de release con la clave de depuración mientras el proyecto no tenga keystore propio (instalable en cualquier teléfono; no apto para Play Store).
- Unificar `versionName` con el tag publicado (`0.1`).
- Documentar en el README cómo instalar la release y cómo migrar a un keystore propio.
- **Non-goals**: publicación en Play Store, keystore de producción, firma con secretos, builds de iOS, actualizaciones automáticas (todo eso queda para el cambio 09).

## Capabilities

### New Capabilities

<!-- Ninguna: es una extensión del capability existente platform/scaffold -->

### Modified Capabilities

- `platform/scaffold`: se agrega el requisito de publicación de releases de Android con tag, APK adjunto y trazabilidad de versión.
- `data/schema-core`: la prueba de paridad de esquema pasa a validar el esquema real que Room crea (introspección en memoria) en lugar del JSON exportado, que no se restaura cuando el cache de Gradle evita re-ejecutar KSP.

## Impact

- Nuevo workflow `.github/workflows/release.yml`; el workflow `android.yml` sigue igual para PRs y main.
- `android/app/build.gradle.kts`: configuración de firma del build type release y `versionName`.
- Permisos: el workflow necesita `contents: write` para crear la release.
- Ningún cambio de esquema, dominio ni UI.
