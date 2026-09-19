## Purpose

Define el respaldo de FitLog: el formato del archivo, el export por secciones, la validación previa y la fusión por ULID con remapeo de ejercicios, idénticos en Android y Web.

## ADDED Requirements

### Requirement: Formato de respaldo

El archivo de respaldo SHALL ser JSON con los campos `format` igual a `fitlog-backup`, `format_version`, `exported_at_ms`, `app_version` y `sections`, donde las claves de `sections` son nombres de tabla (`exercise`, `routine`, `routine_exercise`, `session`, `set_entry`, `body_metric`, `app_setting`) y cada valor es una lista de filas con los nombres de columna del esquema; la interfaz agrupa las tablas en Ejercicios propios, Rutinas, Entrenamientos, Medidas y Ajustes.

#### Scenario: Archivo válido

- **WHEN** se genera un respaldo
- **THEN** el archivo contiene `format` = `fitlog-backup`, `format_version` = 1 y las tablas exportadas con filas que usan los nombres de columna del esquema

#### Scenario: Formato desconocido

- **WHEN** se intenta importar un archivo cuyo `format` no es `fitlog-backup`
- **THEN** la validación falla con un mensaje claro y no se modifica la base

#### Scenario: Versión desconocida

- **WHEN** se intenta importar un archivo con `format_version` mayor a la soportada
- **THEN** la validación falla indicando que el archivo es de una versión más nueva

#### Scenario: JSON inválido

- **WHEN** se intenta importar un archivo que no es JSON válido
- **THEN** la validación falla con un mensaje claro y no se modifica la base

### Requirement: Export por secciones

La aplicación SHALL permitir elegir qué secciones exportar entre ejercicios propios, rutinas, entrenamientos, medidas y ajustes, y SHALL incluir automáticamente los ejercicios propios referenciados por las secciones elegidas.

#### Scenario: Secciones elegidas

- **WHEN** el usuario elige exportar solo entrenamientos y medidas
- **THEN** el archivo contiene esas dos secciones y ninguna otra

#### Scenario: Auto-inclusión de ejercicios

- **WHEN** el usuario exporta entrenamientos que referencian un ejercicio propio
- **THEN** el archivo incluye ese ejercicio propio aunque no se haya marcado la sección de ejercicios

#### Scenario: Ejercicios base fuera del respaldo

- **WHEN** se exporta cualquier sección
- **THEN** el archivo no incluye ejercicios con `is_custom = 0` porque el catálogo base se siembra igual en cualquier instalación

### Requirement: Validación previa del respaldo

La aplicación SHALL validar el archivo antes de aplicar cambios: campos requeridos de cada fila, referencias internas del archivo y, tras el remapeo de ejercicios, que toda referencia resuelva a una fila local o incluida en el archivo; si algo no cierra, SHALL abortar sin modificar la base.

#### Scenario: Referencia faltante

- **WHEN** el archivo incluye una serie que referencia un ejercicio que no está en el archivo ni en la base local
- **THEN** la importación falla indicando el ejercicio faltante y la base queda intacta

#### Scenario: Fila con campos faltantes

- **WHEN** una fila de la sección de medidas no tiene `measured_at`
- **THEN** la importación falla indicando la sección y el identificador de la fila

#### Scenario: Validación sin efectos

- **WHEN** la validación falla
- **THEN** ninguna fila se inserta, actualiza o elimina

### Requirement: Fusión por ULID

La importación SHALL fusionar por `id`: insertar filas nuevas, actualizar filas existentes solo si la fila importada tiene `updated_at` mayor, ignorar las más antiguas o empatadas, propagar los borrados lógicos más nuevos y dejar intactas las filas locales que no están en el archivo.

#### Scenario: Fila nueva

- **WHEN** el archivo trae una rutina con un `id` que no existe localmente
- **THEN** la rutina se inserta

#### Scenario: Fila más nueva en el archivo

- **WHEN** el archivo trae una medida con el mismo `id` y `updated_at` mayor que la local
- **THEN** la medida local se actualiza con los valores del archivo

#### Scenario: Fila más antigua en el archivo

- **WHEN** el archivo trae una serie con el mismo `id` y `updated_at` menor que la local
- **THEN** la fila local no cambia y la importada se cuenta como ignorada

#### Scenario: Empate de timestamps

- **WHEN** el archivo y la base local tienen la misma fila con el mismo `updated_at`
- **THEN** la fila local se conserva y la importada se cuenta como ignorada

#### Scenario: Borrado propagado

- **WHEN** el archivo trae una serie con `deleted_at` no nulo y `updated_at` mayor que la local
- **THEN** la serie local queda con ese `deleted_at` y deja de aparecer en la aplicación

#### Scenario: Filas locales fuera del archivo

- **WHEN** la base local tiene rutinas que no están en el archivo
- **THEN** esas rutinas siguen intactas después de la importación

### Requirement: Remapeo de ejercicios por slug

Cuando un ejercicio propio del archivo tiene el mismo `slug` que un ejercicio local con otro `id`, la importación SHALL NOT insertarlo y SHALL reescribir las referencias del archivo hacia el `id` local.

#### Scenario: Ejercicio propio duplicado por slug

- **WHEN** el archivo trae el ejercicio propio "Remo en máquina" con un `id` distinto al local pero el mismo `slug`
- **THEN** no se inserta un ejercicio nuevo, las series y rutinas del archivo que lo referencian apuntan al `id` local y el resumen lo informa como remapeado

#### Scenario: Ejercicio propio sin conflicto

- **WHEN** el archivo trae un ejercicio propio cuyo `slug` no existe localmente
- **THEN** el ejercicio se inserta y las referencias mantienen su `id`

### Requirement: Resumen de la importación

La aplicación SHALL informar el resultado de la importación con la cantidad de filas insertadas, actualizadas, ignoradas y remapeadas por sección.

#### Scenario: Resumen tras importar

- **WHEN** la importación termina correctamente
- **THEN** la interfaz muestra por sección cuántas filas se insertaron, actualizaron, ignoraron y remapearon

#### Scenario: Importación vacía

- **WHEN** el archivo no trae ninguna fila nueva ni más reciente
- **THEN** el resumen indica que todo se ignoró y la base no cambia

### Requirement: Paridad del respaldo

La serialización, la validación y la fusión SHALL ser idénticas en Android y Web según los casos compartidos en `shared/test-vectors/backup-merge.json`.

#### Scenario: Casos compartidos de fusión

- **WHEN** se ejecutan los casos de `shared/test-vectors/backup-merge.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas producen el mismo estado final y el mismo resumen

#### Scenario: Casos compartidos de validación

- **WHEN** se ejecutan los casos inválidos de `shared/test-vectors/backup-merge.json` en ambas plataformas
- **THEN** ambas fallan con el mismo código de error
