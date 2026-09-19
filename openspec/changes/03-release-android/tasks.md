## 1. Configuración de build

- [ ] 1.1 Establecer `versionName = "0.1"` y firmar el build type `release` con la configuración de depuración
- [ ] 1.2 Verificar que `gradle :app:assembleRelease` produce un APK instalable en CI

## 2. Workflow de release

- [ ] 2.1 Crear `.github/workflows/release.yml` (tags `v*` y `workflow_dispatch`) con JDK 17, SDK 35, tests y `assembleRelease`
- [ ] 2.2 Renombrar el APK a `fitlog-<tag>.apk` y publicar la Release con notas generadas y advertencia de firma de depuración
- [ ] 2.3 Verificar `permissions: contents: write` y que `gh release create` usa el tag correcto

## 3. Publicación y verificación

- [ ] 3.1 Documentar en el README la instalación desde Releases y la migración a keystore propio
- [ ] 3.2 Empujar el tag `v0.1`, esperar el workflow y confirmar que la Release existe con el APK adjunto
- [ ] 3.3 Descargar el APK de la Release y validar que es un APK firmado y no vacío (`unzip -l` / tamaño)
- [ ] 3.4 `openspec validate 03-release-android` y `openspec archive` del cambio
