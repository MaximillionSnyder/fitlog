# FitLog

App personal de registro de entrenamiento, **local-first**, con dos clientes sobre el mismo modelo de datos:

| | Android | Web |
|---|---|---|
| UI | Kotlin + Jetpack Compose + Material 3 | React 19 + TypeScript + Vite + Tailwind |
| Datos | Room (SQLite) | SQLite WASM (OPFS) en Web Worker + Drizzle |
| Build | GitHub Actions → APK | GitHub Actions → GitHub Pages |

La fuente de verdad de datos y comportamiento vive en `shared/` y en las specs de OpenSpec (`openspec/`).

## Estructura

```
├── android/                 App Kotlin (Gradle + Compose + Hilt + Room)
├── web/                     App web (Vite + React + SQLite WASM + Drizzle)
├── shared/
│   ├── schema/              Esquema canónico (migraciones + schema.sql generado)
│   └── test-vectors/        Casos de prueba compartidos (formulas, ulid)
├── openspec/                Specs y cambios (spec-driven)
├── docs/prototipo-pwa/      Prototipo PWA histórico
└── .github/workflows/       CI de Android y Web
```

## Desarrollo

### Web (local)

```bash
cd web
npm ci
npm run dev        # servidor de desarrollo
npm run lint       # ESLint
npm run test       # Vitest (incluye paridad de esquema y vectores compartidos)
npm run build      # tsc + vite build
```

### Android

El build Android se ejecuta **solo en CI** (GitHub Actions), que publica el APK de depuración como artifact:

```bash
gh run list --workflow=android.yml        # ver ejecuciones
gh run download <run-id> -n fitlog-debug-apk
```

Localmente se puede compilar con JDK 17 + Android SDK 35 y Gradle 8.11.1 dentro de `android/`:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

### Esquema canónico

```bash
npm run schema:build     # regenera shared/schema/schema.sql desde las migraciones
npm run schema:check     # falla si schema.sql no coincide con las migraciones
```

Reglas: IDs ULID en texto, timestamps epoch ms UTC, kg/cm, borrado lógico (`deleted_at`), valores derivados (volumen, 1RM) nunca persistidos. La paridad Android↔Web se verifica por tests en ambas plataformas.

## Specs (OpenSpec)

```bash
openspec list                          # cambios activos
openspec show <cambio>                 # detalle de un cambio
openspec validate <cambio>             # validar
openspec archive <cambio>              # archivar y actualizar specs
```
