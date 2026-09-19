## 1. Vectores y dominio

- [ ] 1.1 Crear `shared/test-vectors/insights.json` con casos por regla, caso sin datos, orden por severidad y formato numérico
- [ ] 1.2 Implementar el motor de consejos en el dominio de Web con test contra los vectores
- [ ] 1.3 Implementar el motor de consejos en el dominio de Android con test contra los vectores

## 2. Datos y UI Web

- [ ] 2.1 Reutilizar la consulta de series y grupos del cambio 07 para el motor de tips
- [ ] 2.2 Sección "Tips" con severidad por color, sujeto resuelto con el catálogo, límite de consejos visibles y estado vacío
- [ ] 2.3 Test de integración con `node:sqlite`: tips generados desde datos reales

## 3. Datos y UI Android

- [ ] 3.1 Reutilizar `ComparisonsDao` para las series y grupos del motor de tips
- [ ] 3.2 Pantalla "Tips" con las mismas capacidades y navegación desde la pantalla inicial
- [ ] 3.3 Tests Robolectric del motor aplicado a datos reales

## 4. Cierre

- [ ] 4.1 CI verde en Android y Web con los tests nuevos
- [ ] 4.2 Publicar release `v0.1.5` (versionCode 6) con tips e insights
- [ ] 4.3 `openspec validate 08-insights-tips` y `openspec archive` del cambio
