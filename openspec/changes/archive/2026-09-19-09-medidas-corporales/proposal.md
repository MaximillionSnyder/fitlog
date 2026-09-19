## Why

El entrenamiento no se mide solo en kilos levantados: el peso corporal y las medidas (cintura, brazo, etc.) son el otro lado del progreso. La tabla `body_metric` existe desde el esquema v1 pero nunca se usó.

## What Changes

- Registrar medidas corporales con tipo (peso, grasa, cintura, pecho, brazo, muslo, cadera, cuello, otro), valor, unidad derivada del tipo, fecha y notas.
- Editar y eliminar medidas con borrado lógico.
- Ver el historial por tipo con estadísticas (primera, última, mínimo, máximo y variación) y la gráfica de evolución del tipo elegido.
- Reglas de unidad, validación y estadísticas compartidas y verificadas por vectores en ambas plataformas.
- **Non-goals**: fotos de progreso, composición corporal calculada (IMC, grasa estimada), objetivos o metas de peso, recordatorios de medición.

## Capabilities

### New Capabilities

- `health/body-metrics`: registro, edición, borrado lógico, historial y evolución de medidas corporales por tipo.

### Modified Capabilities

<!-- Ninguna: no cambia el comportamiento de entrenamiento, progreso ni tips -->

## Impact

- Datos: usa la tabla `body_metric` del esquema v1; sin migración.
- Dominio: tipos de medida, unidades, validación, series y estadísticas con vectores en `shared/test-vectors/body-metrics.json`.
- Código: `body/` en dominio, repositorios y UI en `android/` y `web/`; se extrae la gráfica de líneas de Android a un composable reutilizable.
- UI: nueva sección "Medidas" en ambas plataformas.
