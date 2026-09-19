## Context

El registro de series y rutinas ya funciona. El historial se muestra como listas; no hay ninguna vista temporal. Los datos necesarios (`set_entry` con `weight_kg`, `reps`, `is_warmup`, `session.started_at`) están en el esquema v1 y el dominio ya tiene `estimatedOneRepMaxKg` (Epley) y `roundToTenth`.

## Goals / Non-Goals

**Goals:**

- Serie de progreso por ejercicio calculada en dominio, con paridad verificada.
- Gráfica legible y útil en Android y Web con selectores de ejercicio, métrica y rango.
- Cero dependencias nuevas en Android.

**Non-Goals:**

- Comparación entre periodos, PRs, tips, exportar imágenes, interactividad avanzada (tooltip con arrastre, zoom).

## Decisions

### D1. Un punto por sesión, no por día

Agrupar por sesión (no por fecha calendario) mantiene la relación con el historial ya existente y evita promedios artificiales cuando hay dos sesiones el mismo día. El eje X usa la fecha de inicio de la sesión.

### D2. Agregación solo sobre series efectivas completas

Solo las series con `is_warmup = 0` y `deleted_at` nulo participan. El peso máximo y el 1RM se calculan con series que tengan peso y repeticiones; el volumen suma peso × reps de esas series. La cantidad de series efectivas cuenta todas las efectivas, incluso las incompletas. Esto es coherente con el resumen de sesión del cambio 04 y con los vectores del cambio 06.

### D3. Métrica ausente = 0, no null

Un punto con series incompletas reporta 0 en las métricas numéricas en lugar de null: simplifica la gráfica (no hay huecos) y el caso está especificado. La UI puede indicar que el punto no tiene datos completos.

### D4. Filtro temporal en dominio con límites inclusivos

El rango se representa como `{ fromMs, toMs }` con ambos límites inclusivos, calculado desde "ahora" en el momento de consultar: 30 días = `now - 30*24h`, 90 días = `now - 90*24h`, todo = `null`. El cálculo de días vive en el dominio para que ambas plataformas coincidan; los vectores fijan los milisegundos exactos.

### D5. Android dibuja la gráfica con Compose Canvas

En lugar de incorporar Vico, la gráfica de líneas se dibuja con `Canvas` de Compose: ~80 líneas de código, sin dependencia nueva, sin riesgo de incompatibilidad de versiones y con control total del estilo. Vico queda como opción futura si hacen falta más tipos de gráfica (barras, apiladas). Web usa Recharts, que ya resuelve ejes, escalas y tooltips.

### D6. Consulta de datos por ejercicio

La consulta trae las series efectivas del ejercicio elegido junto con `started_at` de su sesión, ordenadas por fecha. El cálculo de puntos y el filtrado ocurren en el dominio (misma función que verifican los vectores), de modo que la base solo aporta filas crudas.

## Risks / Trade-offs

- **Gráfica propia en Android**: menos features que una librería (sin tooltips ni zoom). Aceptado para v1; el diseño deja el punto de extensión aislado en un composable.
- **Sin puntos para métricas incompletas**: una sesión con series sin peso aparece con 0 y puede distorsionar la línea; documentado y aceptado, se revisará con los tips del cambio 08.
- **Recharts añade ~100 KB al bundle web**: aceptado; la web ya carga SQLite WASM de ~870 KB.
