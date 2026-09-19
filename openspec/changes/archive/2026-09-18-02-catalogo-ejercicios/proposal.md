## Why

Sin catálogo no se puede registrar nada: cada serie apunta a un ejercicio. Hoy la base de datos existe pero está vacía, así que el usuario tendría que crear todo a mano antes de entrenar.

## What Changes

- Añadir un catálogo base compartido (`shared/seed/catalog.json`) con grupos musculares y ejercicios iniciales, idéntico en Android y Web.
- Sembrar la base local en el primer arranque (solo si el catálogo está vacío) y registrar la siembra en `app_setting`.
- Permitir listar, buscar y filtrar ejercicios por grupo muscular, equipamiento y tipo en ambas plataformas.
- Permitir crear ejercicios personalizados (`is_custom = 1`) con slug único y borrarlos con borrado lógico.
- **Non-goals**: registro de series (cambio 03), rutinas (04), gráficas (05), edición masiva del catálogo, importación desde fuentes externas, imágenes de ejercicios.

## Capabilities

### New Capabilities

- `exercises/catalog`: catálogo base compartido, siembra idempotente, consulta con búsqueda y filtros, y gestión de ejercicios personalizados.

### Modified Capabilities

<!-- Ninguna: el esquema y el scaffold no cambian de comportamiento -->

## Impact

- Datos: usa las tablas `muscle_group`, `exercise` y `app_setting` del esquema v1 sin migración nueva.
- Código: `shared/seed/`, capa de datos y UI de catálogo en `android/` y `web/`.
- UI Android: nueva pantalla de catálogo con navegación desde la pantalla inicial.
- UI Web: nueva vista de catálogo con búsqueda, filtros y alta de ejercicios propios.
- Tests: integridad del seed y reglas de filtrado verificadas en ambas plataformas.
