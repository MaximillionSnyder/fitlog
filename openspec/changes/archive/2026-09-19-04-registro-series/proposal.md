## Why

El catálogo ya permite elegir ejercicios, pero todavía no se puede registrar lo que se entrena. Sin series guardadas no hay progreso, gráficas ni comparativas: este es el dato primario de la app.

## What Changes

- Iniciar y finalizar una sesión de entrenamiento con timestamps de inicio y fin.
- Registrar series sobre una sesión activa: ejercicio del catálogo, peso, repeticiones, RIR, notas y marca de calentamiento.
- Editar y eliminar series (borrado lógico) con numeración automática por ejercicio.
- Ver el historial de sesiones con resumen (duración, series efectivas, volumen total) y el detalle por sesión agrupado por ejercicio.
- Calcular el resumen de sesión en el dominio con reglas idénticas en ambas plataformas, verificadas por vectores compartidos.
- **Non-goals**: rutinas y plantillas (cambio 05), gráficas (06), comparativas y PRs (07), tips (08), export/import (09), temporizador de descanso, edición de sesiones pasadas más allá de las series.

## Capabilities

### New Capabilities

- `workout/logging`: sesiones de entrenamiento, registro y edición de series, historial y resumen de sesión.

### Modified Capabilities

<!-- Ninguna: el catálogo y el esquema no cambian de comportamiento -->

## Impact

- Datos: usa las tablas `session` y `set_entry` del esquema v1, sin migración nueva.
- Dominio: nueva función de resumen de sesión con vectores compartidos en `shared/test-vectors/session-summary.json`.
- Código: `workout/` de dominio, DAOs/consultas y UI de entrenamiento en `android/` y `web/`.
- UI: nueva sección "Entrenar" con sesión activa e historial en ambas plataformas.
