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

La forma más simple de tener la app en el teléfono es descargar el APK de la última Release:

```bash
gh release download --pattern '*.apk'     # o desde la pestaña Releases en GitHub
```

Instalación: abrir el APK en el teléfono y aceptar el aviso de "instalar apps de origen desconocido".

> **Firma**: las releases 0.x usan la clave de depuración de CI (sirven para instalar y probar, no para Play Store). Para migrar a un keystore propio hay que generar uno, guardarlo como secreto (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) y reemplazar `signingConfig` en `android/app/build.gradle.kts`.

El APK de depuración también se publica como artifact de cada push a `main`:

```bash
gh run list --workflow=android.yml        # ver ejecuciones
gh run download <run-id> -n fitlog-debug-apk
```

Localmente se puede compilar con JDK 17 + Android SDK 35 y Gradle 8.11.1 dentro de `android/`:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

### Publicar una release

```bash
git tag v0.1 && git push origin v0.1     # dispara release.yml y crea la Release
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
