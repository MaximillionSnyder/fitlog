## Why

Los datos de entrenamiento ya se registran, pero hoy solo se ven como listas y totales. Sin gráficas es difícil responder la pregunta central del entrenamiento: ¿estoy progresando? Este cambio convierte el historial en series de tiempo comparables por ejercicio.

## What Changes

- Calcular series de progreso por ejercicio: un punto por sesión con peso máximo, volumen, mejor 1RM estimado y cantidad de series efectivas.
- Permitir elegir ejercicio y métrica, y filtrar por rango de tiempo (30 días, 90 días, todo).
- Mostrar la gráfica en ambas plataformas: Recharts en Web y una gráfica de líneas propia con Compose Canvas en Android (sin dependencia nueva).
- Reglas de agregación compartidas y verificadas por vectores en ambas plataformas.
- **Non-goals**: comparación entre periodos y PRs (cambio 07), tips (08), exportar gráficas como imagen, gráficas de medidas corporales (cambio 09), zoom/pan interactivo.

## Capabilities

### New Capabilities

- `progress/charts`: series de progreso por ejercicio con métricas agregadas por sesión, filtro temporal y visualización.

### Modified Capabilities

<!-- Ninguna: no cambia el comportamiento de catálogo, entrenamiento ni rutinas -->

## Impact

- Datos: solo lectura sobre `set_entry`, `session` y `exercise`; sin migración.
- Dominio: nueva función de series de progreso con vectores en `shared/test-vectors/progress-series.json`.
- Código: `progress/` en dominio y repositorios/UI de progreso en `android/` y `web/`.
- Dependencias: `recharts` en Web; Android dibuja la gráfica con Compose Canvas (sin dependencias nuevas).
- UI: nueva sección "Progreso" en ambas plataformas.
