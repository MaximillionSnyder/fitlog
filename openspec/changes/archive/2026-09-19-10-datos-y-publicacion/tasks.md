## 1. Vectores y dominio

- [x] 1.1 Crear `shared/test-vectors/backup-merge.json` con casos de formato válido/inválido, versión desconocida, referencias faltantes, fusión (nueva, más nueva, más antigua, empate, borrado propagado, filas locales intactas) y remapeo por slug
- [x] 1.2 Implementar serialización, parseo, validación y fusión en el dominio de Web con test contra los vectores
- [x] 1.3 Implementar las mismas funciones en el dominio de Android con test contra los vectores

## 2. Datos (Web)

- [x] 2.1 Export: leer las secciones elegidas y auto-incluir ejercicios propios referenciados
- [x] 2.2 Import: validar, fusionar y persistir en una transacción, devolviendo el resumen
- [x] 2.3 Tests con `node:sqlite`: export por secciones, auto-inclusión, fusión real, remapeo y rollback ante error

## 3. Datos (Android)

- [x] 3.1 Export con las mismas reglas y consultas de lectura por sección
- [x] 3.2 Import transaccional con `withTransaction` de Room y resumen
- [x] 3.3 Tests Robolectric: export, fusión, remapeo, borrado propagado y rollback

## 4. UI

- [x] 4.1 Web: sección "Respaldo" con checkboxes de secciones, descarga del archivo, carga con confirmación y resumen por sección
- [x] 4.2 Android: pantalla "Respaldo" con SAF (`CreateDocument` y `OpenDocument`), confirmación y resumen
- [x] 4.3 Estados de error claros (formato inválido, versión nueva, referencia faltante)

## 5. Keystore y versionado

- [x] 5.1 Crear `.github/workflows/keystore.yml` one-off que genera `fitlog.jks` y lo publica como artifact con `credentials.txt`
- [x] 5.2 Ejecutarlo, descargar el artifact, cargar los 4 secretos con `gh secret set` y borrar el artifact
- [x] 5.3 Adaptar `android/app/build.gradle.kts` al patrón de AppGasto (keystore propio, `versionName` por property, `versionCode` por commits)
- [x] 5.4 Actualizar `release.yml` para decodificar el secreto, firmar y publicar con la huella de la clave
- [x] 5.5 Agregar `/keystore/` al `.gitignore`

## 6. Documentación y cierre

- [x] 6.1 README: respaldo y restauración, firma con keystore propio, rotación de la clave y checklist de uso
- [x] 6.2 CI verde en Android y Web con los tests nuevos
- [x] 6.3 Publicar release `v0.1.7` firmada con la clave propia y verificar la huella
- [x] 6.4 `openspec validate 10-datos-y-publicacion` y `openspec archive` del cambio
