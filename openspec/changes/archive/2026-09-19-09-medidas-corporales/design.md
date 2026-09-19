## Context

La tabla `body_metric` está en el esquema v1 desde el cambio 01 y nunca se usó. El dominio ya tiene el patrón de series temporales (progreso) y de estadísticas (comparativas), y Android ya tiene una gráfica de líneas propia que conviene reutilizar en lugar de duplicar.

## Goals / Non-Goals

**Goals:**

- Registro simple y rápido de medidas con unidad derivada del tipo (sin que el usuario elija unidad).
- Historial y estadísticas por tipo con paridad verificada.
- Reutilizar la gráfica de líneas de Android extrayéndola a un composable común.

**Non-Goals:**

- Fotos, IMC o composición corporal calculada, metas de peso, recordatorios.

## Decisions

### D1. La unidad se deriva del tipo, nunca la elige el usuario

`unitForKind` mapea peso corporal a `kg`, grasa a `%` y el resto a `cm`. El esquema ya tiene la columna `unit` con CHECK, así que la unidad se persiste para que las filas sean autocontenidas y exportables, pero la app no la pregunta.

### D2. Validación en el dominio con reglas explícitas

Valor finito y mayor que cero; grasa corporal además ≤ 100. Las reglas viven en `validateMetric` y se especifican por vectores, de modo que Android y Web rechacen exactamente lo mismo con los mismos mensajes.

### D3. Estadísticas sobre el periodo, no sobre todo el historial

Las estadísticas (primera, última, mínimo, máximo, variación) se calculan sobre el rango elegido (30/90 días/todo) para que sean comparables con el resto de la app. El historial, en cambio, muestra todas las mediciones del tipo y se pagina visualmente con un límite.

### D4. Serie de evolución ordenada por fecha ascendente

`buildMetricSeries` filtra por tipo y rango, ordena ascendente por fecha (desempate por id) y alimenta la gráfica. La variación se calcula entre el primer y el último punto del periodo, coherente con las estadísticas.

### D5. Gráfica de líneas extraída a `ui/LineChart.kt`

El composable que dibuja líneas con Compose Canvas en la pantalla de Progreso se mueve a un archivo propio y se reutiliza en Medidas, con un formateador de valores inyectable (kg con un decimal, cm con un decimal, % con un decimal). No se agrega ninguna dependencia.

## Risks / Trade-offs

- **Sin metas ni objetivos**: el usuario no recibe indicación de si un peso es bueno o malo; es intencional (FitLog no da consejo médico) y los tips del cambio 08 no cubren medidas.
- **Una sola medida no muestra tendencia**: la gráfica con un punto se ve plana; se acepta y las estadísticas lo informan con variación 0.
- **Tipos fijos**: agregar un tipo nuevo requiere tocar el esquema (CHECK) y los vectores; es el precio de la integridad del dato.
