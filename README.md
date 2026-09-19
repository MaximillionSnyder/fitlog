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
├── docs/diseno.md           Sistema de diseño (tokens, componentes y navegación)
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

> **Firma**: las releases se firman con el keystore propio del proyecto (`android/keystore/fitlog.jks`), que nunca se versiona: el workflow `release.yml` lo recrea desde los secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD`. La huella SHA-256 de la clave se imprime en el log del workflow y en las notas de la Release.
>
> **Cambio de firma**: las versiones 0.1 a 0.1.6 estaban firmadas con la clave de depuración. La primera release firmada con el keystore propio **no actualiza** esas instalaciones: hay que desinstalar la app una vez.

El APK de depuración también se publica como artifact de cada push a `main`:

```bash
gh run list --workflow=android.yml        # ver ejecuciones
gh run download <run-id> -n fitlog-debug-apk
```

Localmente se puede compilar con JDK 17 + Android SDK 37 y Gradle 9.7.1 dentro de `android/`:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
gradle :app:assembleRelease -PversionName=0.1.7 \
  -PFITLOG_STORE_PASSWORD=... -PFITLOG_KEY_ALIAS=fitlog -PFITLOG_KEY_PASSWORD=...
```

### Publicar una release

```bash
git tag v0.1.10 && git push origin v0.1.10   # dispara release.yml: tests, firma y Release
```

El `versionName` sale del tag (sin la `v`) y el `versionCode` del historial de commits, así que no hay que tocarlos a mano.

### Rotar el keystore

1. Ejecutar el workflow `keystore.yml` (`gh workflow run keystore.yml`), que genera un keystore nuevo y lo publica como artifact temporal con `credentials.txt`
2. Descargar el artifact, actualizar los 4 secretos con `gh secret set` y borrar el artifact
3. Guardar una copia del  en un lugar seguro: sin él no se pueden firmar actualizaciones de la misma app

> El keystore es PKCS12, que usa **una sola contraseña**: `KEY_PASSWORD` debe tener el mismo valor que `KEYSTORE_PASSWORD` (keytool ignora `-keypass` en PKCS12).

### Respaldo y restauración

En la app, sección **Respaldo**: se eligen las secciones (ejercicios propios, rutinas, entrenamientos, medidas, ajustes) y se descarga un JSON. Al importar, los datos se **fusionan** con los del dispositivo:

- gana la fila con `updated_at` más nuevo (los empates conservan lo local)
- los borrados más nuevos se propagan
- las filas locales que no están en el archivo no se tocan
- si un ejercicio propio del archivo ya existe local con el mismo nombre, se reutiliza el local y se reescriben las referencias

La validación es previa y sin efectos: si el archivo es inválido, tiene una versión más nueva o le falta una referencia, no se modifica nada.

### Checklist de uso

1. **Catálogo**: revisar los 36 ejercicios base y agregar los propios
2. **Rutinas**: armar los días de entrenamiento con series y reps objetivo
3. **Entrenar**: iniciar sesión (o desde una rutina), registrar series con peso, reps y RIR
4. **Progreso**: seguir la evolución por ejercicio (peso máximo, volumen, 1RM estimado)
5. **Comparativas**: revisar PRs, comparar el último mes contra el anterior y el balance muscular
6. **Tips**: leer las observaciones del periodo y ajustar
7. **Medidas**: registrar peso corporal y medidas
8. **Respaldo**: exportar antes de cambiar de teléfono y fusionar en el nuevo

### Sistema de diseño

La presentación de las dos apps sigue un mismo sistema: tokens de color con tema claro y oscuro,
tipografía con cifras tabulares para los datos, una biblioteca de componentes propia y una
navegación de cinco destinos de primer nivel con el resto agrupado en "Más". Está documentado en
[`docs/diseno.md`](docs/diseno.md).

En Android, el tema se elige en **Ajustes** (`sistema`, `claro`, `oscuro`, más los colores dinámicos
de Android 12+) y la preferencia se guarda en el dispositivo. En la web, el botón del encabezado
alterna claro/oscuro y el modo automático sigue al sistema.

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
