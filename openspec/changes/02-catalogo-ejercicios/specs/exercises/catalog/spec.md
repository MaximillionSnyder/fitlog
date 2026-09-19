## Purpose

Define el catálogo de ejercicios de FitLog: los datos base compartidos, la siembra idempotente en la base local, las reglas de búsqueda y filtrado, y la gestión de ejercicios personalizados del usuario.

## ADDED Requirements

### Requirement: Catálogo base compartido

`shared/seed/catalog.json` SHALL contener los grupos musculares y ejercicios iniciales con ULID, slug y timestamps fijos, y SHALL ser la única fuente del catálogo base para Android y Web.

#### Scenario: Contenido mínimo del catálogo base

- **WHEN** se inspecciona `shared/seed/catalog.json`
- **THEN** contiene al menos 10 grupos musculares y 25 ejercicios, todos con `id` ULID válido, `slug` único y `kind` en `strength`, `cardio` o `mobility`

#### Scenario: Integridad referencial del catálogo

- **WHEN** se valida cada ejercicio del catálogo base
- **THEN** su `muscle_group_id` y su `secondary_muscle_group_id` (si existe) referencian grupos musculares presentes en el mismo archivo

### Requirement: Siembra idempotente

La base local SHALL sembrarse con el catálogo base en el primer arranque, solo cuando la tabla `muscle_group` esté vacía, y SHALL registrar la siembra en `app_setting` con la clave `catalog_seeded_at`.

#### Scenario: Primera ejecución

- **WHEN** la aplicación abre una base recién creada
- **THEN** inserta los grupos musculares y ejercicios del catálogo base y guarda `catalog_seeded_at` una sola vez

#### Scenario: Ejecuciones posteriores

- **WHEN** la aplicación vuelve a abrir una base ya sembrada
- **THEN** no duplica filas y el conteo de ejercicios permanece igual

#### Scenario: Datos del usuario preservados

- **WHEN** la base ya contiene ejercicios personalizados y se reinicia la siembra
- **THEN** los ejercicios personalizados siguen presentes y ningún ejercicio base se sobrescribe

### Requirement: Búsqueda y filtros del catálogo

La consulta del catálogo SHALL devolver solo ejercicios activos (`deleted_at` nulo), ordenados por nombre, y SHALL permitir buscar por texto en el nombre (insensible a mayúsculas y acentos) y filtrar por grupo muscular, equipamiento y tipo.

#### Scenario: Búsqueda por texto

- **WHEN** el usuario escribe "press" en el buscador
- **THEN** el resultado incluye los ejercicios cuyo nombre contiene "press" y excluye los que no

#### Scenario: Búsqueda sin acentos

- **WHEN** el usuario escribe "triceps" sin acento
- **THEN** el resultado incluye "Extensión de tríceps en polea"

#### Scenario: Filtro por grupo muscular

- **WHEN** el usuario filtra por el grupo muscular "pecho"
- **THEN** el resultado solo contiene ejercicios cuyo grupo muscular principal o secundario es "pecho"

#### Scenario: Filtros combinados

- **WHEN** el usuario combina grupo muscular "espalda" y equipamiento "barra"
- **THEN** el resultado contiene solo ejercicios que cumplen ambas condiciones

#### Scenario: Sin resultados

- **WHEN** ningún ejercicio cumple los criterios
- **THEN** la interfaz muestra un estado vacío explícito en lugar de una lista en blanco

### Requirement: Ejercicios personalizados

El usuario SHALL poder crear ejercicios personalizados marcados con `is_custom = 1` y SHALL poder eliminarlos con borrado lógico; los ejercicios del catálogo base SHALL NOT poder eliminarse.

#### Scenario: Crear un ejercicio personalizado

- **WHEN** el usuario crea el ejercicio "Remo en máquina" con grupo muscular "espalda" y equipamiento "máquina"
- **THEN** el ejercicio queda guardado con un ULID nuevo, `is_custom` en 1, slug único y timestamps de creación y actualización

#### Scenario: Nombre duplicado

- **WHEN** el usuario intenta crear un ejercicio cuyo slug ya existe
- **THEN** la operación falla con un mensaje de error y no modifica la base

#### Scenario: Validación de nombre y grupo

- **WHEN** el usuario intenta crear un ejercicio con nombre vacío o con un grupo muscular inexistente
- **THEN** la operación falla con un mensaje de error y no modifica la base

#### Scenario: Eliminar un ejercicio personalizado

- **WHEN** el usuario elimina un ejercicio personalizado
- **THEN** la fila conserva sus datos con `deleted_at` distinto de nulo y desaparece de la consulta del catálogo

#### Scenario: Proteger el catálogo base

- **WHEN** se intenta eliminar un ejercicio con `is_custom = 0`
- **THEN** la operación falla indicando que el catálogo base no se puede eliminar

### Requirement: Marcado visual de ejercicios propios

La interfaz del catálogo SHALL distinguir visualmente los ejercicios personalizados de los del catálogo base en ambas plataformas.

#### Scenario: Distinción en la lista

- **WHEN** la lista contiene un ejercicio personalizado y uno base
- **THEN** el personalizado muestra una etiqueta visible que lo identifica como propio del usuario

### Requirement: Paridad de filtrado entre plataformas

Las reglas de normalización de texto y de filtrado SHALL producir el mismo resultado en Android y Web para las mismas entradas, verificado con casos compartidos en `shared/test-vectors/catalog-filters.json`.

#### Scenario: Casos compartidos de filtrado

- **WHEN** se ejecutan los casos de `shared/test-vectors/catalog-filters.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven exactamente los slugs esperados para cada combinación de búsqueda y filtros
