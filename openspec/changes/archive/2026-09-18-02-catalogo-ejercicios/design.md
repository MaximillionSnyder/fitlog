## Context

El esquema v1 ya está aplicado y verificado en ambas plataformas (cambio 01). El catálogo es el primer consumidor real del modelo: introduce datos compartidos (seed), reglas de negocio (búsqueda, filtros, alta de personalizados) y UI en dos plataformas. El catálogo es pequeño (decenas de filas) y cambia poco.

## Goals / Non-Goals

**Goals:**

- Un único archivo de seed versionado que ambas plataformas consumen sin transformaciones.
- Siembra idempotente y segura: nunca pisa datos del usuario ni duplica filas.
- Reglas de filtrado idénticas verificadas por vectores compartidos.
- UI de catálogo usable: buscar, filtrar, ver propios, crear y eliminar.

**Non-Goals:**

- Imágenes, videos o instrucciones de ejecución por ejercicio.
- Edición de ejercicios base o del seed en runtime.
- Búsqueda avanzada (FTS, sinónimos, ranking por relevancia).
- Compartir catálogo entre dispositivos (sync es no-goal de v1).

## Decisions

### D1. Formato y estabilidad del seed

`shared/seed/catalog.json` se genera una vez con un script (`shared/seed/generate-catalog.mjs`) que usa el ULID de `shared/` para producir IDs deterministas a partir de un timestamp fijo y una semilla. Los IDs quedan congelados: cambiarlos rompería referencias de series y rutinas ya guardadas. El archivo se valida en tests de ambas plataformas (ULIDs válidos, slugs únicos, referencias existentes).

### D2. Momento y mecanismo de siembra

La siembra ocurre al abrir la base, no en la UI: en Web dentro del worker después de aplicar migraciones y antes de emitir `ready`; en Android en un `CatalogSeeder` invocado una vez por el repositorio de catálogo (bajo un `Mutex`). Condición: `COUNT(muscle_group) == 0`. Además se escribe `app_setting.catalog_seeded_at`. El seed solo inserta; nunca actualiza ni borra, por lo que no puede pisar personalizaciones.

### D3. Filtrado en memoria con normalización explícita

La consulta a la base trae los ejercicios activos con su grupo principal y secundario; el filtrado (texto, grupo, equipamiento, tipo) se aplica en la capa de dominio sobre esa lista. Motivos: el catálogo es pequeño, el comportamiento queda idéntico en Kotlin y TypeScript, y es testeable sin base de datos. Normalización definida: `lowercase` + `NFD` sin diacríticos + colapso de espacios. Si el catálogo creciera a miles de filas, se migraría a FTS con un cambio propio.

### D4. Slug y unicidad

El slug se deriva del nombre con la misma normalización más reemplazo de todo lo no alfanumérico por `-`. La unicidad la garantiza el índice único de `exercise.slug`; la violación se traduce a un error de dominio con mensaje claro ("Ya existe un ejercicio con ese nombre").

### D5. Protección del catálogo base

`is_custom = 0` es inmutable desde la app: el repositorio rechaza borrar o editar ejercicios base. No se usan triggers en SQL para mantener el comportamiento idéntico y testeable en ambas plataformas.

### D6. Navegación

Android: `androidx.navigation-compose` con rutas `home` y `catalog`. Web: navegación por estado en `App` (sin router) mientras la app tenga una sola vista adicional; se reevaluará al llegar a 4+ vistas.

### D7. Tests de siembra

Android usa Robolectric para levantar Room en memoria en la JVM (sin emulador) y verificar la siembra completa, su idempotencia y la protección de personalizados. Web usa `node:sqlite` con el mismo runner que en producción. Los casos de filtrado viven en `shared/test-vectors/catalog-filters.json` y se ejecutan en ambas plataformas.

## Risks / Trade-offs

- **Robolectric añade peso y descargas en CI**: se acepta porque permite testear Room/DAO sin emulador; si se vuelve inestable, la alternativa es mover esos tests a instrumentación.
- **Seed con IDs congelados**: si hay que corregir un nombre de ejercicio base, se hace con una migración que actualice por `id`; nunca cambiando el `id`.
- **Filtrado en memoria**: no escala a catálogos enormes; aceptado por el tamaño real y anotado como deuda con umbral claro (miles de filas).
- **Duplicación de la lógica de filtrado en dos lenguajes**: mitigada con vectores compartidos; una divergencia falla CI en ambas plataformas.
