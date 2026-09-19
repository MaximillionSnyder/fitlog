# data/schema-core Specification

## Purpose
Define el esquema SQL canónico v1 de FitLog, las reglas de identificadores, timestamps y unidades, el sistema de migraciones, y los casos de prueba compartidos que garantizan que Android y Web implementen exactamente el mismo modelo y las mismas fórmulas.

## Requirements

### Requirement: Esquema canónico único

`shared/schema/schema.sql` SHALL definir el esquema completo de la versión 1 con las tablas de dominio `muscle_group`, `exercise`, `routine`, `routine_exercise`, `session`, `set_entry`, `body_metric` y `app_setting`, más la tabla interna `schema_migration`, y SHALL ser la única fuente de verdad estructural para ambos clientes.

#### Scenario: Tablas del esquema v1

- **WHEN** se aplica `shared/schema/schema.sql` sobre una base SQLite vacía
- **THEN** existen las ocho tablas de dominio, la tabla interna `schema_migration`, y ninguna tabla adicional

#### Scenario: Columnas de una tabla de series

- **WHEN** se inspecciona `PRAGMA table_info(set_entry)`
- **THEN** aparecen al menos `id`, `session_id`, `exercise_id`, `set_index`, `weight_kg`, `reps`, `rir`, `notes`, `created_at`, `updated_at` y `deleted_at`

### Requirement: Identificadores, timestamps y unidades

Toda tabla de entidad SHALL usar `id TEXT` con ULID de 26 caracteres como clave primaria, SHALL almacenar timestamps como enteros epoch en milisegundos UTC, y SHALL usar kilogramos y centímetros como unidades sin conversión en v1.

#### Scenario: Identificador ULID

- **WHEN** se inserta una fila sin especificar `id` mediante la API de la aplicación
- **THEN** el `id` resultante es un ULID válido de 26 caracteres

#### Scenario: Borrado lógico

- **WHEN** se elimina una serie desde la aplicación
- **THEN** la fila permanece en la tabla con `deleted_at` distinto de nulo y deja de aparecer en las consultas de la aplicación

### Requirement: Migraciones numeradas

Las migraciones SHALL vivir en `shared/schema/migrations/` con nombres `NNN_descripcion.sql` aplicados en orden ascendente, y cada plataforma SHALL registrar las migraciones aplicadas para no repetirlas.

#### Scenario: Base nueva migra desde cero

- **WHEN** una base vacía abre la aplicación
- **THEN** se aplican todas las migraciones en orden y la versión de esquema registrada corresponde a la última migración disponible

#### Scenario: Base existente no repite migraciones

- **WHEN** una base ya migrada vuelve a abrir la aplicación
- **THEN** ninguna migración se aplica dos veces y no se produce error

### Requirement: Espejo del esquema en Android

El proyecto Android SHALL declarar entidades Room equivalentes al esquema canónico, con `room.schemaLocation` configurado para exportar el JSON de esquema, y SHALL incluir una prueba JVM que compare el JSON exportado contra `shared/schema/schema.sql` y falle ante cualquier divergencia de tablas o columnas.

#### Scenario: Divergencia detectada

- **WHEN** una entidad Room agrega o renombra una columna sin actualizar el esquema canónico y se ejecutan los tests de Android
- **THEN** la prueba de paridad falla indicando la tabla y columna divergente

### Requirement: Espejo del esquema en Web

El proyecto Web SHALL declarar un esquema Drizzle equivalente al canónico y SHALL incluir una prueba Vitest que compare, sobre una base en memoria con `shared/schema/schema.sql` aplicado, las tablas y columnas definidas por Drizzle contra las reales mediante introspección.

#### Scenario: Divergencia detectada en web

- **WHEN** el esquema Drizzle agrega o renombra una columna sin actualizar el esquema canónico y se ejecutan los tests de web
- **THEN** la prueba de paridad falla indicando la tabla y columna divergente

### Requirement: Casos de prueba compartidos de fórmulas

`shared/test-vectors/` SHALL contener archivos JSON con entradas y resultados esperados de las fórmulas de dominio, y tanto Android (JUnit) como Web (Vitest) SHALL ejecutarlos contra sus implementaciones.

#### Scenario: Fórmula de volumen verificada

- **WHEN** se ejecutan los casos de `test-vectors/formulas.json` en cualquier plataforma
- **THEN** el volumen calculado para cada caso coincide exactamente con el valor esperado

#### Scenario: 1RM estimado verificado

- **WHEN** se evalúa un caso de 1RM Epley con peso y repeticiones dados
- **THEN** el resultado coincide con `peso * (1 + reps / 30)` redondeado a un decimal

### Requirement: Valores derivados no persistidos

El esquema SHALL NOT almacenar volumen ni 1RM estimado como columnas; estos SHALL calcularse en la capa de dominio a partir de `set_entry`.

#### Scenario: Sin columnas derivadas

- **WHEN** se inspeccionan las columnas de `set_entry`
- **THEN** no existe ninguna columna de volumen ni de 1RM estimado
