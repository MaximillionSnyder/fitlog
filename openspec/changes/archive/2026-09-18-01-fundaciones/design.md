## Context

FitLog arranca de cero: el repositorio solo contiene un prototipo PWA roto archivado en `docs/prototipo-pwa/`. El entorno local de desarrollo es Termux (Android aarch64) sin JDK ni Android SDK, por lo que **todo build Android ocurre en GitHub Actions**; Node 24 sí está disponible para la web. Existen restricciones ya decididas aguas arriba: Kotlin nativo para Android, React para web, SQLite WASM con OPFS en web y datos local-first sin backend en v1.

## Goals / Non-Goals

**Goals:**

- Dejar ambas apps arrancables y verificadas por CI, con el esquema canónico v1 ya aplicado.
- Garantizar por test que Android (Room) y Web (Drizzle) no divergen del esquema SQL compartido.
- Sentar las reglas de datos (ULID, epoch ms, kg/cm, borrado lógico) que todos los cambios posteriores heredan.
- Que el APK de depuración y la web queden publicados automáticamente desde `main`.

**Non-Goals:**

- Sincronización en la nube, cuentas o backend.
- Features de producto (catálogo, registro, gráficas, rutinas, medidas, tips, export/import).
- Unificar UI entre plataformas (se acepta duplicación de UI; solo se comparte esquema y fórmulas).
- Room 3.0 / KMP: Android usa Room estable de la línea 2.x.

## Decisions

### D1. Estructura del monorepo

`android/` y `web/` son proyectos independientes con su propio build; `shared/` solo contiene datos: `schema/schema.sql`, `schema/migrations/` y `test-vectors/`. Nada de código fuente compartido entre Kotlin y TypeScript: se comparten **contratos** (SQL y vectores de prueba), no implementaciones.

Alternativa descartada: Kotlin Multiplatform con módulo `shared` de código. Rechazada porque la web es React y no consumiría artefactos Kotlin.

### D2. Versiones de herramientas Android

Gradle con Version Catalog (`libs.versions.toml`): AGP 8.x estable, Kotlin 2.x estable, Compose BOM reciente, KSP para Room (nunca kapt), Min SDK 26, Target/Compile SDK el último estable disponible en el runner de Actions. JDK 17 (`temurin`) en CI. Las versiones exactas se fijan en la tarea 01 y quedan congeladas hasta un cambio posterior.

### D3. Cómo consume Android el esquema canónico

Room genera el esquema desde entidades Kotlin declaradas a mano y exporta su JSON de esquema (`room.schemaLocation`). La equivalencia con `shared/schema/schema.sql` se garantiza por la prueba D4, no por copiar el SQL a assets.

Alternativas descartadas:
- `createFromAsset` con `schema.sql`: requeriría preconstruir un archivo `.db` binario y Room validaría el esquema copiado de forma estricta (FKs, índices, CHECK), con alto riesgo de fallar en runtime sin posibilidad de probarlo localmente.
- Dejar que Room sea la fuente de verdad y generar el SQL para web: Room no soporta CHECK ni controla el dialecto que usa la web.
- Consecuencia aceptada: la base Android no lleva los CHECK del SQL canónico; la validación equivalente vive en la capa de dominio y en los tests de paridad de columnas.

### D4. Prueba de paridad Android

Room exporta su JSON de esquema (`room.schemaLocation`) en tiempo de compilación. Una prueba JVM (sin emulador, sin Robolectric) parsea `shared/schema/schema.sql` y el JSON exportado y compara el conjunto de tablas y columnas; cualquier diferencia falla el build en CI. Esto convierte la paridad en un gate automático.

### D5. Persistencia web: SQLite WASM en Web Worker + Drizzle sqlite-proxy

`@sqlite.org/sqlite-wasm` corre en un Web Worker dedicado y persiste en OPFS (VFS `opfs`). El hilo principal habla con el worker por `postMessage` con un protocolo mínimo (`exec`, `query`). Drizzle usa el driver `drizzle-orm/sqlite-proxy` sobre ese protocolo, de modo que la app consulta con el esquema tipado sin bloquear el hilo principal.

Alternativa descartada: `wa-sqlite` o sql.js. El paquete oficial es el mantenido por el equipo de SQLite y ya soporta OPFS.

### D6. Migraciones web

Las migraciones se importan como strings (`?raw`) desde `shared/schema/migrations/`, se aplican en orden dentro del worker y se registran en la tabla `schema_migration` (creada por la migración 001). Reintentar la apertura no reaplica migraciones.

### D7. Paridad web

Prueba Vitest con `node:sqlite` (built-in de Node 24, sin dependencias nativas): aplica `schema.sql` en memoria, introspecciona tablas/columnas y las compara contra las definiciones de Drizzle (`getTableConfig`). Detecta divergencias en CI sin navegador.

### D8. ULID y fórmulas

Generador ULID propio y mínimo en cada plataforma (26 chars, Crockford Base32, timestamp + aleatorio), validado por los mismos `test-vectors`. Fórmulas de dominio (volumen, 1RM Epley) implementadas en `domain/` de cada plataforma y validadas contra `test-vectors/formulas.json`. Un test de paridad de casos garantiza resultados idénticos.

### D9. CI

Dos workflows separados con filtros de ruta: `android.yml` (`paths: android/**, shared/**`) hace `assembleDebug` + `testDebugUnitTest` + subida del APK como artifact; `web.yml` (`paths: web/**, shared/**`) hace `npm ci`, lint, `vitest run`, `build` y despliegue a Pages solo en `main`. Cache: `gradle/actions/setup-gradle` y `actions/setup-node` cache npm.

### D10. Repositorio y publicación

Repositorio GitHub público (`fitlog`) para tener Actions sin límite de minutos y Pages gratuito. La web se publica en `https://<usuario>.github.io/fitlog/`, por lo que Vite usa `base: '/fitlog/'` en producción.

## Risks / Trade-offs

- **Primer build Android en CI lento** (descarga de Gradle/SDK): mitigado con cache de Gradle y fijando versiones; ~5–10 min el primer run.
- **No hay verificación Android local**: cualquier iteración requiere push y esperar CI. Mitigado manteniendo la lógica de dominio en módulos JVM puros (tests de dominio rápidos) y usando CI como gate.
- **OPFS solo en navegadores modernos y contexto seguro**: aceptado para v1 (app personal); sin fallback a memoria para no dar falsa sensación de persistencia.
- **`node:sqlite` es experimental en Node 24**: solo se usa en tests; si molesta, se puede migrar a `better-sqlite3` en CI sin tocar la app.
- **Divergencia de esquema por migraciones manuales**: mitigado con las pruebas de paridad D4 y D7 en cada PR.
- **`app_setting` como tabla clave-valor** puede tentar a guardar JSON arbitrario: se documenta que solo almacena configuración simple (unidad, tema, versión de esquema).
