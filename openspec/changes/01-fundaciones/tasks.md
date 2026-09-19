## 1. Esquema canónico y casos de prueba compartidos

- [ ] 1.1 Crear `shared/schema/schema.sql` con las tablas `muscle_group`, `exercise`, `routine`, `routine_exercise`, `session`, `set_entry`, `body_metric`, `app_setting` y `schema_migration`, con IDs ULID, timestamps epoch ms y columnas de borrado lógico
- [ ] 1.2 Crear `shared/schema/migrations/001_esquema_inicial.sql` como primera migración aplicable sobre base vacía
- [ ] 1.3 Crear `shared/test-vectors/formulas.json` con casos de volumen y 1RM Epley (incluye casos límite: reps=0, peso decimal, valores grandes)
- [ ] 1.4 Crear `shared/test-vectors/ulid.json` con patrones válidos e inválidos de ULID
- [ ] 1.5 Añadir `shared/README.md` explicando que el esquema y los vectores son la fuente de verdad y cómo se consumen desde cada plataforma

## 2. Cliente Android

- [ ] 2.1 Crear proyecto Gradle en `android/` con Kotlin 2.x, AGP 8.x, Compose BOM, Material 3 y `libs.versions.toml` como única fuente de versiones
- [ ] 2.2 Configurar KSP, Hilt y Room; Min SDK 26; `room.schemaLocation` exportado al directorio de esquemas
- [ ] 2.3 Configurar KSP con exportación de esquema Room a `app/schemas/` y exponer `shared/schema/schema.sql` y `shared/test-vectors/` como recursos de test
- [ ] 2.4 Implementar `domain/` con ULID y fórmulas (volumen, 1RM Epley) sin dependencias Android
- [ ] 2.5 Test JVM de casos compartidos: ejecutar `formulas.json` y `ulid.json` con JUnit
- [ ] 2.6 Test JVM de paridad de esquema: comparar JSON exportado por Room contra `shared/schema/schema.sql`
- [ ] 2.7 Pantalla inicial Compose "FitLog" que muestre el estado de la base (versión de esquema aplicada)

## 3. Cliente Web

- [ ] 3.1 Crear app Vite + React 19 + TypeScript estricto + Tailwind en `web/` (base `/fitlog/` en producción)
- [ ] 3.2 Definir esquema Drizzle que refleje el canónico y configuración de `drizzle-orm/sqlite-proxy`
- [ ] 3.3 Implementar Web Worker con `@sqlite.org/sqlite-wasm` (OPFS) y protocolo `exec`/`query` por `postMessage`
- [ ] 3.4 Aplicar migraciones `shared/schema/migrations/*.sql` (importadas con `?raw`) y registrar en `schema_migration`
- [ ] 3.5 Implementar `domain/` con ULID y fórmulas equivalentes a las de Android
- [ ] 3.6 Tests Vitest: casos compartidos, paridad de esquema con `node:sqlite` y generador ULID
- [ ] 3.7 Página inicial con estado de la base SQLite (persistencia OPFS activa) y aviso si el navegador no la soporta

## 4. Integración continua y publicación

- [ ] 4.1 Workflow `android.yml`: JDK 17, cache Gradle, `assembleDebug`, `testDebugUnitTest`, APK como artifact (filtros `android/**`, `shared/**`)
- [ ] 4.2 Workflow `web.yml`: Node 24, `npm ci`, lint, `vitest run`, build y despliegue a GitHub Pages en `main` (filtros `web/**`, `shared/**`)
- [ ] 4.3 Verificar en GitHub que ambos workflows corren verdes y que el APK y la URL de Pages funcionan
- [ ] 4.4 Crear repo GitHub `fitlog` público, push de `main` y comprobar Actions y Pages habilitados

## 5. Cierre del cambio

- [ ] 5.1 Ejecutar `openspec validate 01-fundaciones` sin errores
- [ ] 5.2 Actualizar `README.md` raíz con arquitectura, estructura y comandos de desarrollo
- [ ] 5.3 Archivar el cambio con `openspec archive 01-fundaciones` y verificar que `openspec/specs/` contiene las capabilities `platform/scaffold` y `data/schema-core`
