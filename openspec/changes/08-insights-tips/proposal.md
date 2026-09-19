## Why

FitLog ya muestra qué pasó (historial, gráficas, comparativas), pero no dice qué hacer al respecto. Los tips convierten los datos en decisiones: seguir progresando, romper un estancamiento, corregir un desbalance o registrar mejor la información.

## What Changes

- Generar consejos por reglas deterministas a partir de los datos del periodo: progreso de 1RM, estancamiento, desbalance muscular, frecuencia de entrenamiento y datos incompletos.
- Cada consejo lleva tipo, severidad (logro, información, advertencia), sujeto (ejercicio o grupo muscular) y un valor numérico de referencia.
- Reglas, umbrales y textos idénticos en Android y Web, verificados por vectores compartidos.
- Nueva sección "Tips" en ambas plataformas, ordenada por severidad.
- **Non-goals**: recomendaciones con IA, planes automáticos de entrenamiento, notificaciones, tips sobre nutrición o sueño, personalización por objetivo del usuario.

## Capabilities

### New Capabilities

- `insights/coaching-tips`: reglas de consejos sobre progreso, estancamiento, balance, frecuencia y calidad de datos, con severidad y orden deterministas.

### Modified Capabilities

<!-- Ninguna: no cambia el comportamiento de las vistas existentes -->

## Impact

- Datos: solo lectura sobre `set_entry`, `session` y catálogo; sin migración.
- Dominio: motor de reglas con umbrales explícitos y vectores en `shared/test-vectors/insights.json`.
- Código: `insights/` en dominio, consultas y UI en `android/` y `web/`.
- UI: nueva sección "Tips" en ambas plataformas.
