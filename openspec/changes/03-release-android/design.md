## Context

El build de Android ya corre en CI con Gradle 8.11.1, JDK 17 y SDK 35, y el APK de depuración se publica como artifact. No hay keystore propio ni JDK en el entorno local (Termux), y esta release es para uso personal ("ver la app"), no para tiendas.

## Goals / Non-Goals

**Goals:**

- Un tag `v*` produce una Release con un APK instalable en cualquier teléfono.
- Cero secretos que configurar para la primera release.
- Camino de migración claro a un keystore propio.

**Non-Goals:**

- Firma de producción, Play Store, versionado automático, canales beta.

## Decisions

### D1. Firmar el release con la clave de depuración

El build type `release` usa `signingConfig = signingConfigs.getByName("debug")`. AGP genera el keystore de depuración en el runner si no existe, así que no hay secretos. Trade-off: el APK no sirve para Play Store y no permite actualizar sobre una instalación previa firmada con otra clave; se acepta porque el objetivo es probar la app y la migración a keystore propio se documenta (secreto `KEYSTORE_BASE64` + `keytool`/`apksigner` o `signingConfigs` desde variables de entorno).

### D2. Workflow separado de `android.yml`

`android.yml` sigue validando push/PR con tests y APK debug (feedback rápido). `release.yml` se dispara solo con tags `v*` (y manualmente con `workflow_dispatch` + input `tag`), corre los mismos tests y publica con `gh release create --generate-notes`.

### D3. Nombre del asset y versión

`versionName = "0.1"` en Gradle y tag `v0.1`. El APK se renombra a `fitlog-v0.1.apk` antes de adjuntarlo para que el archivo descargado identifique la versión.

### D4. Paridad de esquema contra la base real, no contra el JSON exportado

El primer run de release falló porque la prueba de paridad leía `app/schemas/*.json` y, al restaurarse KSP desde el cache de Gradle, ese archivo no se regenera. La prueba pasa a abrir Room en memoria (Robolectric) e introspeccionar `PRAGMA table_info`, comparando contra `shared/schema/schema.sql`. Ventajas: no depende de artefactos de build, valida exactamente lo que la app crea en el dispositivo y sigue fallando ante cualquier divergencia de tablas, columnas, tipos, nulabilidad o PK. `exportSchema` se mantiene para el historial de migraciones.

## Risks / Trade-offs

- **Clave de depuración**: no hay continuidad de firma entre runners (AGP la regenera), por lo que las actualizaciones exigen desinstalar si cambia la clave. Mitigación: documentar y migrar a keystore propio en el cambio 09.
- **Releases con el mismo tag**: si hay que rehacer una release, `gh release create` falla; se resuelve con `gh release upload --clobber` en la documentación del README.
