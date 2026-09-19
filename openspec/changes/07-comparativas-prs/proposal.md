## Why

Las gráficas muestran la evolución, pero no responden dos preguntas que el usuario se hace seguido: ¿estoy mejor que el mes pasado? y ¿cuál es mi mejor marca en cada ejercicio? Sin eso, el historial es informativo pero no accionable.

## What Changes

- Calcular marcas personales (PRs) por ejercicio: mejor peso, mejor 1RM estimado, mejor volumen de sesión y mejores repeticiones, con la fecha en que se lograron.
- Comparar periodos: últimos 30 o 90 días contra el periodo inmediatamente anterior, con volumen, series efectivas, sesiones y porcentaje de cambio.
- Mostrar el balance muscular del periodo: volumen por grupo muscular con su participación sobre el total.
- Reglas compartidas y verificadas por vectores en ambas plataformas.
- **Non-goals**: tips automáticos (cambio 08), notificaciones al batir un PR, PRs por repeticiones a un peso específico, comparación entre ejercicios distintos, exportar.

## Capabilities

### New Capabilities

- `progress/personal-records`: marcas personales por ejercicio (peso, 1RM, volumen de sesión y repeticiones) con fecha de logro.
- `progress/comparisons`: comparación de periodos (actual vs anterior) y balance muscular por grupo.

### Modified Capabilities

<!-- Ninguna: no cambia el comportamiento de las gráficas ni del registro -->

## Impact

- Datos: solo lectura sobre `set_entry`, `session`, `exercise` y `muscle_group`; sin migración.
- Dominio: funciones de PRs, comparación de periodos y balance muscular con vectores en `shared/test-vectors/comparisons.json`.
- Código: `comparisons/` en dominio, consultas y UI en `android/` y `web/`.
- UI: nueva sección "Comparativas" en ambas plataformas.
