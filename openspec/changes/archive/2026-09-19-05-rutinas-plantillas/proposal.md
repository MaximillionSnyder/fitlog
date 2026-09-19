## Why

Hoy cada sesión se arma eligiendo ejercicios de a uno. Con rutinas el usuario define una vez sus días de entrenamiento (ejercicios, orden, series y repeticiones objetivo) y arranca a entrenar desde ahí, que es como se entrena en la práctica.

## What Changes

- Crear, editar, listar y eliminar rutinas (borrado lógico) con nombre y descripción.
- Gestionar los ejercicios de una rutina: agregar, quitar y reordenar, con series objetivo, repeticiones objetivo, peso objetivo, descanso y notas por ejercicio.
- Iniciar una sesión de entrenamiento desde una rutina, guardando la rutina de origen en la sesión y mostrándola en el detalle del historial.
- Reglas de orden y posiciones compartidas y verificadas por vectores en ambas plataformas.
- **Non-goals**: sugerencias automáticas de progresión dentro de la rutina, plantillas predefinidas por la app, clonar rutinas, sesiones parciales desde rutina (se registran series a mano como hoy).

## Capabilities

### New Capabilities

- `workout/templates`: rutinas con ejercicios ordenados, objetivos por ejercicio, y arranque de sesiones desde una rutina.

### Modified Capabilities

- `workout/logging`: el detalle de una sesión muestra la rutina de origen cuando la sesión se inició desde una rutina.

## Impact

- Datos: usa `routine` y `routine_exercise` del esquema v1 y la columna `session.routine_id` ya existente, sin migración.
- Dominio: nueva lógica de reordenamiento y posiciones con vectores en `shared/test-vectors/routine-order.json`.
- Código: `routines/` en el dominio y repositorios/UI de rutinas en `android/` y `web/`; `startSession` acepta rutina.
- UI: nueva sección "Rutinas" en ambas plataformas y acceso a "Entrenar desde esta rutina".
