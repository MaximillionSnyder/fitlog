## Context

El progreso ya se calcula por ejercicio y sesión. Este cambio reutiliza las mismas reglas de "serie efectiva" (sin calentamiento, sin borrado, con peso y repeticiones para métricas numéricas) y añade agregaciones nuevas: por ejercicio (PRs), por periodo (comparación) y por grupo muscular (balance). El catálogo ya expone el grupo principal de cada ejercicio.

## Goals / Non-Goals

**Goals:**

- PRs por ejercicio con fecha de logro, sin ambigüedad en empates.
- Comparación de periodos contiguos con porcentajes y manejo explícito del periodo anterior vacío.
- Balance muscular del periodo con participación porcentual.
- Paridad Android/Web verificada por vectores.

**Non-Goals:**

- Tips automáticos, notificaciones, PRs por rango de repeticiones, comparación entre usuarios.

## Decisions

### D1. Agregaciones puras en el dominio, consultas crudas en la base

Igual que en el progreso, las consultas traen filas crudas (series + fecha de sesión + grupo del ejercicio) y todo el cálculo vive en funciones puras del dominio, especificadas por vectores. Esto evita duplicar lógica SQL en dos dialectos.

### D2. Empates de PR: gana la fecha más antigua

Cuando dos series alcanzan el mismo valor, la marca conserva la fecha más antigua ("primera vez lograda"), que es la semántica habitual de un PR. La comparación de fechas se hace con `>=` sobre la fecha existente para no reemplazar la más antigua.

### D3. Marcas sin fecha cuando no hay datos

Un ejercicio sin series completas reporta 0 en peso, 1RM y volumen, con fecha nula; las repeticiones se calculan solo con series que tengan repeticiones. La UI decide cómo mostrarlo (guion en lugar de fecha).

### D4. Rangos contiguos y sin solapamiento

Para un preset de N días y "ahora" T: actual = [T - N días, T]; anterior = [T - 2N días, T - N días - 1 ms]. Restar 1 ms evita que una serie caiga en ambos periodos. La función que calcula los rangos vive en el dominio y tiene vectores propios.

### D5. Porcentaje de cambio nulo cuando no hay base

Si el periodo anterior tiene 0 en una métrica, el cambio es nulo (se muestra "sin datos"), no 0 % ni infinito. El porcentaje se redondea a 1 decimal con la misma regla que el resto de la app.

### D6. Balance muscular por grupo principal, con clave "sin-grupo"

El volumen se agrupa por el grupo principal del ejercicio (el catálogo garantiza uno). Si un ejercicio no tiene grupo resoluble, su volumen va a la clave `sin-grupo` en lugar de descartarse, para que la suma del balance siempre coincida con el volumen total del periodo.

## Risks / Trade-offs

- **Cálculo en memoria de todos los sets del periodo**: aceptable para uso personal; si crece, se moverá a SQL agregado por plataforma con los mismos vectores.
- **PRs globales (no por periodo)**: el listado de PRs no filtra por fecha; es intencional (una marca personal no caduca) y el filtro de periodo aplica solo a comparación y balance.
- **Balance por grupo principal únicamente**: los músculos secundarios no reparten volumen; se documenta para no prometer un balance que no existe.
