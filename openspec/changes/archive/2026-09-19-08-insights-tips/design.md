## Context

El dominio ya calcula series de progreso, PRs, comparación de periodos y balance muscular. Los tips son una capa de reglas sobre esos mismos datos crudos: no necesitan información nueva ni esquema nuevo. El requisito crítico es la paridad: un mismo consejo debe leerse igual en Android y en Web.

## Goals / Non-Goals

**Goals:**

- Reglas con umbrales explícitos y explicables, sin caja negra.
- Mensajes idénticos en ambas plataformas, con formato numérico determinista.
- Orden estable y predecible.

**Non-Goals:**

- IA, planes automáticos, notificaciones, personalización por objetivo, consejos de nutrición o descanso.

## Decisions

### D1. Cinco reglas con umbrales fijos

- **Progreso**: ≥3 sesiones del ejercicio en el periodo y mejor 1RM de la última > mejor 1RM de la primera → logro con el % de mejora.
- **Estancamiento**: ≥4 sesiones y |variación| de 1RM ≤ 0,5 % → advertencia con la cantidad de sesiones.
- **Desbalance**: ≥2 grupos con volumen y el mayor con ≥50 % del total → advertencia con la participación.
- **Frecuencia**: promedio de sesiones por semana ≥3 → logro; <1,5 → advertencia.
- **Datos incompletos**: ≥5 series efectivas y ≥30 % sin peso o sin repeticiones → información con el porcentaje.

Los umbrales viven en constantes nombradas y en los vectores, no dispersos en la UI. Si un umbral cambia, cambia el vector y falla CI en ambas plataformas a la vez.

### D2. Mensaje generado en el dominio, nombre del sujeto en la UI

El dominio no conoce el catálogo: produce el mensaje con los números y deja el nombre del ejercicio o grupo para la interfaz, que ya tiene el catálogo cargado. Así el motor es puro y testeable sin base de datos, y los vectores comparan mensajes exactos.

### D3. Formato numérico determinista compartido

Los mensajes incluyen números redondeados a 1 decimal; si el valor es entero se imprime sin decimales (`25`), si no, con un decimal (`57.1`). La regla se implementa igual en Kotlin y TypeScript y se verifica en los vectores, evitando divergencias de `toString` entre lenguajes.

### D4. Orden por severidad, tipo y sujeto

Advertencias primero, luego información, luego logros; dentro de cada severidad por tipo (orden alfabético del identificador) y por sujeto. Es determinista y pone lo accionable arriba.

### D5. Una sesión cuenta si tiene series efectivas

El conteo de sesiones para frecuencia y reglas de progreso usa sesiones con al menos una serie efectiva del ejercicio o del periodo, igual que la comparación de periodos del cambio 07, para no inflar la frecuencia con sesiones vacías.

## Risks / Trade-offs

- **Consejos repetitivos**: un mismo tipo puede aparecer para varios ejercicios (varios estancamientos); se acepta y la UI limita la lista a los primeros N para no saturar.
- **Umbrales arbitrarios**: 50 % de desbalance o 3 sesiones/semana son heurísticas; quedan documentados y son fáciles de ajustar en un cambio futuro con sus vectores.
- **Sin personalización**: alguien que entrena 2 días por semana verá siempre la advertencia de frecuencia; se mitiga permitiendo ocultar la sección y se reevaluará con objetivos de usuario.
