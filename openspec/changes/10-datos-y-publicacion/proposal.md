## Why

FitLog guarda todo localmente y no hay forma de sacar los datos del dispositivo: un borrado accidental, un cambio de teléfono o querer usar lo mismo en Web y Android significa empezar de cero. Además, las releases 0.x están firmadas con la clave de depuración de CI, lo que impide considerarlas versiones de verdad.

## What Changes

- Exportar un respaldo JSON por secciones (ejercicios propios, rutinas, entrenamientos, medidas y ajustes) con auto-inclusión de los ejercicios referenciados.
- Importar un respaldo con **fusión por ULID**: upsert por `id` con last-write-wins por `updated_at`, propagación de borrados, remapeo de ejercicios con el mismo slug y resumen de insertados, actualizados, ignorados y remapeados.
- Validación previa completa (formato, versión, referencias) que aborta sin tocar la base si algo no cierra.
- Firmar las releases con un keystore propio guardado en secretos de GitHub (patrón ya usado en AppGasto) en lugar de la clave de depuración.
- Derivar `versionName` del tag y `versionCode` del historial de commits.
- Documentar respaldo, restauración, firma y rotación de keystore.
- **Non-goals**: sincronización automática en la nube, cuentas de usuario, exportar a CSV/Excel, importar desde otras apps, cifrado del archivo de respaldo.

## Capabilities

### New Capabilities

- `data/backup`: formato de respaldo, export por secciones, validación previa y fusión por ULID con resumen.

### Modified Capabilities

- `platform/scaffold`: las releases de Android pasan a firmarse con un keystore propio desde secretos y el versionado se deriva del tag y del historial de commits.

## Impact

- Datos: lee y escribe todas las tablas de dominio; sin migración de esquema.
- Dominio: funciones puras de serialización, validación y fusión con vectores en `shared/test-vectors/backup-merge.json`.
- Código: `backup/` en dominio, repositorios y UI en `android/` y `web/`; `keystore.yml` nuevo; `release.yml` y `app/build.gradle.kts` actualizados.
- Seguridad: cuatro secretos nuevos en el repositorio (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- Operación: la primera release firmada con la clave nueva no actualiza instalaciones previas; requiere desinstalar una vez.
