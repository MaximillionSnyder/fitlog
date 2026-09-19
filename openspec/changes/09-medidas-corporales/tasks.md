## 1. Vectores y dominio

- [ ] 1.1 Crear `shared/test-vectors/body-metrics.json` con casos de unidad por tipo, validaciones, series ordenadas, filtros por tipo y rango, y estadísticas (incluye una sola medición y sin mediciones)
- [ ] 1.2 Implementar tipos, unidades, validación, series y estadísticas en el dominio de Web con test contra los vectores
- [ ] 1.3 Implementar las mismas funciones en el dominio de Android con test contra los vectores

## 2. Datos (Web)

- [ ] 2.1 Repositorio de medidas sobre Drizzle: registrar, editar, borrado lógico y listar por tipo con rango
- [ ] 2.2 Tests con `node:sqlite`: validaciones, borrado lógico, orden y filtrado

## 3. Datos (Android)

- [ ] 3.1 `BodyMetricsDao` y repositorio con las mismas reglas que Web
- [ ] 3.2 Tests Robolectric de registro, validaciones, edición, borrado y consultas

## 4. UI

- [ ] 4.1 Web: sección "Medidas" con formulario de alta, selector de tipo, historial, estadísticas y gráfica de evolución
- [ ] 4.2 Web: edición y eliminación con confirmación, y estado vacío
- [ ] 4.3 Android: extraer la gráfica de líneas a `ui/LineChart.kt` y reutilizarla en Progreso y Medidas
- [ ] 4.4 Android: pantalla "Medidas" con formulario, historial, estadísticas, gráfica y navegación desde la pantalla inicial

## 5. Cierre

- [ ] 5.1 CI verde en Android y Web con los tests nuevos
- [ ] 5.2 Publicar release `v0.1.6` (versionCode 7) con medidas corporales
- [ ] 5.3 `openspec validate 09-medidas-corporales` y `openspec archive` del cambio
