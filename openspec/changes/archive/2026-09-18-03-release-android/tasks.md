## 1. Configuración de build

- [x] 1.1 Establecer `versionName = "0.1"` y firmar el build type `release` con la configuración de depuración
- [x] 1.2 Verificar que `gradle :app:assembleRelease` produce un APK instalable en CI

## 2. Workflow de release

- [x] 2.1 Crear `.github/workflows/release.yml` (tags `v*` y `workflow_dispatch`) con JDK 17, SDK 35, tests y `assembleRelease`
- [x] 2.2 Renombrar el APK a `fitlog-<tag>.apk` y publicar la Release con notas generadas y advertencia de firma de depuración
- [x] 2.3 Verificar `permissions: contents: write` y que `gh release create` usa el tag correcto

## 3. Paridad de esquema robusta

- [x] 3.1 Reescribir `SchemaParityTest` para introspeccionar el esquema que Room crea en memoria (Robolectric) en lugar del JSON exportado
- [x] 3.2 Verificar que la comparación cubre tablas, columnas, tipos, nulabilidad y PK contra `shared/schema/schema.sql`

## 4. Publicación y verificación

- [x] 4.1 Documentar en el README la instalación desde Releases y la migración a keystore propio
- [x] 4.2 Empujar el tag `v0.1`, esperar el workflow y confirmar que la Release existe con el APK adjunto
- [x] 4.3 Descargar el APK de la Release y validar que es un APK firmado y no vacío (`unzip -l` / tamaño)
- [x] 4.4 `openspec validate 03-release-android` y `openspec archive` del cambio
