## Context

El esquema v1 ya tiene `routine` y `routine_exercise` (con `position` y un índice único por rutina+posición) y `session.routine_id`. El registro de series funciona y las sesiones se pueden consultar. Falta la plantilla reutilizable y el arranque desde ella.

## Goals / Non-Goals

**Goals:**

- Rutinas editables con orden estable y objetivos por ejercicio.
- Arrancar sesiones desde una rutina sin tocar el flujo de series ya existente.
- Reordenamiento idéntico en Android y Web, verificado por vectores.

**Non-Goals:**

- Progresión automática de cargas, plantillas del sistema, clonar/compartir rutinas, checklist de "ejercicio completado" durante la sesión.

## Decisions

### D1. Posiciones densas 1..N, reasignadas en cada mutación

Toda mutación de la lista (agregar, quitar, reordenar) reescribe las posiciones de 1 a N dentro de una transacción. El índice único `(routine_id, position)` garantiza consistencia; como las posiciones se reasignan al final, se actualizan primero a valores temporales negativos para evitar colisiones intermedias.

Alternativa descartada: posiciones con huecos (10, 20, 30) para inserts baratos. Se rechaza porque complica el cálculo del vector de orden y no aporta con listas de decenas de ejercicios.

### D2. Funciones puras de orden en el dominio

`moveItem(ids, fromIndex, toIndex)` y `assignPositions(ids)` viven en el dominio de cada plataforma y se especifican con `shared/test-vectors/routine-order.json`. El repositorio lee los ids ordenados, aplica la función pura y persiste las posiciones resultantes.

### D3. Un ejercicio no se repite dentro de la misma rutina

La restricción se aplica en el dominio (consulta previa) y se apoya en un índice único de la tabla. Repetir el mismo ejercicio en una rutina es un error de modelado: si se quieren dos bloques del mismo ejercicio, se distinguen por las series objetivo del bloque que ya existe. Se evita añadir una columna de "bloque" sin necesidad.

### D4. La sesión guarda la rutina, no la copia

`session.routine_id` referencia la rutina y no se duplican los ejercicios objetivo en la sesión. Si la rutina se elimina, la sesión conserva su `routine_id` (la fila sigue existiendo con borrado lógico) y el detalle muestra la rutina solo si sigue activa. Esto mantiene el historial íntegro y evita datos duplicados.

### D5. UI de rutinas independiente del registro

Las rutinas se gestionan en su propia sección. La sesión activa sigue funcionando igual (registrar series a mano); la rutina es el punto de partida y una guía visual, no un checklist obligatorio. Así el cambio no toca el flujo de series ya verificado.

## Risks / Trade-offs

- **Reasignar posiciones en cada mutación**: escrituras O(N) por operación; aceptable para listas pequeñas y simplifica la corrección.
- **Sin checklist de rutina durante la sesión**: el usuario puede desviarse de la rutina sin aviso; se acepta para v1 y se reevalúa con las gráficas de progreso.
- **Borrado lógico de rutinas con sesiones**: el nombre de la rutina no se muestra en sesiones históricas si la rutina fue eliminada; documentado en la spec para no prometer lo que no hay.
