## MODIFIED Requirements

### Requirement: Espejo del esquema en Android

El proyecto Android SHALL declarar entidades Room equivalentes al esquema canónico, con `room.schemaLocation` configurado para exportar el JSON de esquema, y SHALL incluir una prueba JVM que compare el esquema que Room crea realmente (introspección de la base en memoria) contra `shared/schema/schema.sql` y falle ante cualquier divergencia de tablas o columnas.

#### Scenario: Divergencia detectada

- **WHEN** una entidad Room agrega o renombra una columna sin actualizar el esquema canónico y se ejecutan los tests de Android
- **THEN** la prueba de paridad falla indicando la tabla y columna divergente

#### Scenario: Paridad robusta ante el cache de Gradle

- **WHEN** la tarea de KSP se restaura desde el cache de Gradle en CI y el JSON exportado no se regenera
- **THEN** la prueba de paridad sigue ejecutándose contra el esquema real creado por Room y no falla por archivos ausentes
