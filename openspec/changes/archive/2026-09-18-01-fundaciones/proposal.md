## Why

FitLog necesita una base común para que Android (Kotlin/Compose) y Web (React/SQLite WASM) compartan el mismo modelo de datos y comportamiento desde el primer día. Sin un esquema canónico y un pipeline de compilación reproducible, las dos plataformas divergirían y las specs no serían verificables.

## What Changes

- Crear el monorepo `~/appsalud` con `android/`, `web/`, `shared/` y `openspec/`.
- Definir el esquema SQL canónico v1 en `shared/schema/schema.sql` con migraciones numeradas en `shared/schema/migrations/`.
- Definir los casos de prueba compartidos en `shared/test-vectors/` para fórmulas de dominio (volumen, 1RM Epley) que ambas plataformas deben cumplir.
- Crear el proyecto Android (Gradle + Kotlin 2.x + Compose + Material 3 + Hilt + Room) con pantalla inicial mínima y base de datos vacía migrable.
- Crear el proyecto Web (Vite + React 19 + TypeScript + Tailwind + SQLite WASM con OPFS en Web Worker vía Drizzle).
- Configurar CI en GitHub Actions: build/test Android y build/test Web en cada push y PR.
- Publicar el APK de depuración como artifact descargable y desplegar la web en GitHub Pages.
- **Non-goals**: sin backend ni sincronización en la nube; sin funcionalidades de catálogo, registro de series, gráficas, rutinas, medidas, tips ni export/import (cambios 02–09).

## Capabilities

### New Capabilities

- `platform/scaffold`: estructura del monorepo, proyectos base Android y Web arrancables, y pipelines de CI que compilan y prueban ambas plataformas.
- `data/schema-core`: esquema SQL canónico v1 (ejercicios, grupos musculares, rutinas, sesiones, series, medidas corporales, ajustes), reglas de identificadores y timestamps, sistema de migraciones y casos de prueba compartidos.

### Modified Capabilities

<!-- Ninguna: es el primer cambio del proyecto -->

## Impact

- Código: crea `android/`, `web/`, `shared/`, `.github/workflows/`.
- Dependencias: Gradle/Kotlin/Compose/Hilt/Room (Android); React/Vite/Tailwind/Drizzle/sqlite-wasm (Web).
- Sistemas: GitHub Actions y GitHub Pages; el repo se crea en GitHub y requiere Actions habilitado.
- Riesgo: el primer build Android en Actions fija la versión de herramientas; cualquier cambio posterior de SDK/AGP se hace vía cambio OpenSpec.
