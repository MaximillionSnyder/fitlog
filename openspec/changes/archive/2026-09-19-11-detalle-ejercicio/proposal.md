## Why

En el catálogo, las tarjetas de ejercicio son un callejón sin salida: muestran el nombre y un resumen truncado, pero al tocarlas no pasa nada. El usuario no puede ver el grupo muscular secundario ni confirmar los datos del ejercicio sin crearlo o buscarlo dentro de una rutina.

## What Changes

- Permitir seleccionar una tarjeta del catálogo para abrir un detalle del ejercicio en Android y Web.
- El detalle muestra: nombre, marca de ejercicio propio, tipo (`Fuerza`, `Cardio`, `Movilidad`), grupo muscular principal, grupo muscular secundario (si existe) y equipamiento.
- Cerrar el detalle vuelve a la lista sin alterar búsqueda ni filtros.
- **Non-goals**: editar ejercicios, imágenes o descripciones, estadísticas de uso en rutinas/sesiones, y cualquier cambio de esquema, datos o siembra.

## Capabilities

### New Capabilities

<!-- Ninguna: el detalle es una vista derivada del catálogo existente -->

### Modified Capabilities

- `exercises/catalog`: se agrega el requirement de detalle de ejercicio al seleccionar una tarjeta, con paridad Android ↔ Web.

## Impact

- UI Android: `android/app/src/main/java/com/fitlog/app/ui/CatalogScreen.kt` (tarjeta clickeable y diálogo de detalle); sin cambios en repositorio, DAO ni ViewModel.
- UI Web: `web/src/ui/CatalogView.tsx` (tarjeta clickeable y modal de detalle); sin cambios en `data/`, `state/` ni `domain/`.
- Datos: sin migraciones, sin cambios en `shared/seed/catalog.json` ni en `shared/test-vectors/`.
- Tests: no cambia el dominio; la verificación es de UI y CI (lint, tests y build en ambas plataformas).
