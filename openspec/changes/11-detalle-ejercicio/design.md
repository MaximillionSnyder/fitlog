## Context

Ver `proposal.md` - Why. El catálogo ya carga en cada plataforma un snapshot con los grupos musculares y los ejercicios (`CatalogSnapshot` en Android, `CatalogSnapshot` en Web), y las tarjetas ya muestran nombre, grupo principal, equipamiento y tipo. Lo único que falta es exponer el detalle al seleccionar una tarjeta; no hay que consultar datos nuevos ni tocar el esquema.

## Goals / Non-Goals

**Goals:**

- Abrir un detalle legible con los datos que ya están en memoria, sin consultas adicionales.
- Mantener el estado de búsqueda y filtros al abrir y cerrar el detalle.
- Respetar las convenciones de UI de cada plataforma: `AlertDialog` en Android y modal con overlay en Web.

**Non-Goals:**

- Edición de ejercicios, navegación a una pantalla propia, historial de uso.
- Cambios en repositorios, ViewModels, hooks, dominio o esquema.

## Decisions

### D1. El detalle se resuelve en la capa de UI con el snapshot existente

El nombre del grupo principal y del secundario se resuelven con el mapa de grupos que la vista ya tiene (`state.groups` en Android, `groupNameById` en Web). No se agregan columnas, joins ni consultas: el detalle es una proyección de `CatalogExercise` + `MuscleGroup`.

Alternativa descartada: enriquecer `CatalogExercise` con los nombres de los grupos en la capa de datos, que obligaría a cambiar el dominio y los tests de ambas plataformas para un dato que la UI ya puede resolver.

### D2. Android: `Card(onClick)` + `AlertDialog`

En `CatalogScreen.kt` la tarjeta pasa a usar el overload clickeable de `Card` de Material 3 y se agrega `ExerciseDetailDialog`, siguiendo el patrón de diálogos existente en la pantalla (`NewExerciseDialog`, `AlertDialog` de borrado). El `TextButton` "Eliminar" interno conserva su comportamiento porque consume el click antes de que llegue a la tarjeta.

### D3. Web: tarjeta clickeable + panel de detalle inline

En `CatalogView.tsx` la zona de información del `<li>` se convierte en un `<button>` con semántica accesible (foco y teclado nativos) que abre el detalle; el botón "Eliminar" queda hermano dentro del `<li>`. El panel de detalle se renderiza como un `<li>` adicional inmediatamente después de la tarjeta seleccionada, siguiendo el patrón de detalle inline de `WorkoutView.tsx` y manteniéndolo a la vista sin importar la posición en la lista. Se cierra con un botón "Cerrar" y seleccionar otra tarjeta reemplaza el detalle.

Alternativa descartada: overlay modal, que no existe en el resto de la web y agregaría manejo de foco, `Escape` y click afuera para un dato de solo lectura.

### D4. El detalle es de solo lectura

No incluye acciones. La creación sigue en el formulario "Nuevo/Nuevo propio" y la eliminación de propios sigue en la tarjeta, tal como hoy. Así el cambio es puramente aditivo y no altera flujos existentes.

## Risks / Trade-offs

- **Card clickeable vs. botón Eliminar (Android)**: el click del `TextButton` podría abrir también el detalle → el `TextButton` consume el evento; se verifica manualmente en el APK.
- **Duplicación de etiquetas entre plataformas**: los textos del detalle se repiten en Kotlin y TSX; es el costo habitual de la paridad y no amerita un vector compartido porque no hay reglas de dominio.
