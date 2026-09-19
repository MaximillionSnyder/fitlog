## Context

Todas las tablas de dominio están pobladas y la app se usa de verdad, pero los datos solo viven en el dispositivo. El esquema ya tiene lo necesario para fusionar sin conflictos destructivos (ULID, `updated_at`, `deleted_at`). El patrón de firma con secretos ya está probado en el repositorio AppGasto del mismo autor.

## Goals / Non-Goals

**Goals:**

- Respaldo completo y restauración segura por secciones, con fusión no destructiva.
- Fusionar dos dispositivos (Android y Web) sin perder datos ni duplicar ejercicios.
- Releases firmadas con clave propia y versionado derivado del tag.

**Non-Goals:**

- Sync automática, cuentas, cifrado del archivo, export a CSV, importar desde otras apps.

## Decisions

### D1. Fusión por ULID con last-write-wins por `updated_at`

Es la única semántica que permite mezclar dispositivos sin decisiones manuales y sin pérdida silenciosa: cada fila se resuelve por su `id`, gana la de `updated_at` mayor y los empates conservan lo local. Los borrados viajan porque `deleted_at` forma parte de la fila y, si es más nuevo, pisa al local.

Alternativa descartada: reemplazar por sección (más simple, pero un import de un dispositivo viejo borraría datos nuevos del actual).

### D2. Remapeo de ejercicios por slug

El único caso donde dos dispositivos crean "la misma" entidad con distinto `id` es un ejercicio propio con el mismo nombre. Como `slug` es único, si el archivo trae un slug existente con otro `id` no se inserta y las referencias del archivo se reescriben al `id` local. Sin esto, la fusión fallaría por índice único o dejaría series huérfanas.

### D3. Validación previa completa y sin efectos

El import valida en tres pasos antes de tocar la base: formato/versión, campos requeridos por fila, y referencias (internas del archivo y contra la base local, ya aplicado el remap). Cualquier fallo aborta con un mensaje que identifica sección e id. La escritura ocurre después, en una sola transacción.

### D4. Funciones puras de fusión, persistencia aparte

`serializeBackup`, `parseBackup`, `validateBackup` y `mergeBackup(local, backup)` operan sobre arrays de filas, no sobre la base. Eso permite verificar la paridad Android/Web con vectores sobre el estado final y el resumen, sin depender del motor SQL de cada plataforma. El repositorio solo lee filas locales, llama a la función pura y persiste el resultado.

### D5. Secciones con nombres de columna del esquema

El JSON usa los nombres de columna reales (`set_entry.session_id`, `body_metric.measured_at`, etc.) en lugar de un modelo paralelo: el respaldo es legible, el mapeo es trivial y no hay que mantener dos vocabularios.

### D6. Keystore propio con el patrón de AppGasto

Cuatro secretos (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `keystore/fitlog.jks` ignorado por git, `signingConfigs.create("app")` solo si el archivo existe, y `release.yml` que decodifica el secreto antes de compilar. El keystore se genera una vez en un workflow `workflow_dispatch` que lo publica como artifact (nunca en logs) para cargarlo a secretos y guardar copia.

### D7. Versionado derivado

`versionName` desde `-PversionName=${GITHUB_REF_NAME#v}` y `versionCode` desde `git rev-list --count HEAD`, igual que AppGasto. Elimina el bump manual y hace que el APK siempre coincida con su tag.

## Risks / Trade-offs

- **Cambio de clave de firma**: la primera release firmada con la clave propia no actualiza instalaciones previas; hay que desinstalar una vez. Se documenta y se avisa en la release.
- **Sin cifrado**: el respaldo es JSON legible; es intencional para que el usuario pueda inspeccionarlo, y se documenta que contiene datos personales.
- **Resumen sin resolución interactiva**: los conflictos de slug se resuelven automáticamente (gana el local) y se informan; no hay UI para elegir fila por fila.
- **Fusión en memoria**: leer todas las filas para fusionar es aceptable para uso personal; si el historial crece mucho, se moverá a SQL con los mismos vectores.
