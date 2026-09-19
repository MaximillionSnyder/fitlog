## Context

El esquema v1 ya tiene `session` y `set_entry`, y el catálogo está sembrado y consultable en ambas plataformas. Falta la capa que convierte esos datos en entrenamientos registrados. Ambas plataformas deben calcular los mismos resúmenes para que el historial y, más adelante, las gráficas y comparativas sean consistentes.

## Goals / Non-Goals

**Goals:**

- Registrar entrenamientos con una sesión activa única y series completas.
- Resumen de sesión calculado en dominio con paridad verificada.
- Historial navegable en Android y Web.
- Sin migración de esquema: el modelo del cambio 01 alcanza.

**Non-Goals:**

- Rutinas (se guarda `routine_id` nulo por ahora), temporizador de descanso, edición de timestamps de sesiones pasadas, gráficas y comparativas.

## Decisions

### D1. Una sesión activa, detectada por consulta

La sesión activa es la última con `finished_at` nulo y `deleted_at` nulo. No se añade una columna de estado ni una restricción parcial: la unicidad la garantiza el dominio (crear solo si no existe activa) y el test lo verifica. Si en el futuro hay multi-dispositivo, la sync resolverá conflictos.

### D2. `set_index` por ejercicio dentro de la sesión

El índice se calcula como `max(set_index) + 1` para el mismo par (sesión, ejercicio) entre las series no eliminadas, y se asigna dentro de la transacción de inserción. Así se evita la carrera entre el cálculo y el insert en cada plataforma.

### D3. Resumen en dominio con vectores compartidos

`session-summary.json` define entradas (listas de series con peso, reps, calentamiento, ejercicio) y salidas esperadas (series totales, efectivas, volumen total, volumen por ejercicio). La implementación vive en `domain/` de cada plataforma y se ejecuta contra los mismos casos. Reglas: una serie aporta volumen solo si es efectiva y tiene peso y repeticiones; los valores nulos no suman ni rompen el cálculo.

### D4. Reutilizar el catálogo para elegir ejercicio

La UI de registro usa el catálogo ya cargado (mismo filtro y búsqueda) en lugar de duplicar un selector. El ejercicio elegido se pasa como `exercise_id`; el repositorio valida que siga activo.

### D5. Historial en dos niveles

Lista de sesiones con resumen (calculado en dominio sobre las series traídas en una sola consulta por sesión) y detalle con series agrupadas por ejercicio. Para el historial se cargan todas las series de las sesiones visibles de una vez (pocas sesiones, app personal); se revisará con paginación si el volumen crece.

## Risks / Trade-offs

- **Sesiones activas huérfanas** (usuario olvida finalizar): el historial las marca "en curso" y el resumen no calcula duración; se acepta para v1, con posible auto-cierre en un cambio futuro.
- **Cálculo de resumen en memoria**: igual que el filtrado del catálogo, no escala a miles de series; aceptado y anotado.
- **Sin bloqueo de concurrencia real en SQLite web** (un solo worker): el cálculo de `set_index` es seguro porque el worker serializa las operaciones.
