## 1. Dominio y vectores

- [ ] 1.1 Crear `shared/test-vectors/routine-order.json` con casos de mover elementos (inicio, fin, mismo índice, índices inválidos) y posiciones resultantes
- [ ] 1.2 Implementar `moveItem` y `assignPositions` en el dominio de Web con test contra los vectores
- [ ] 1.3 Implementar `moveItem` y `assignPositions` en el dominio de Android con test contra los vectores

## 2. Datos (Web)

- [ ] 2.1 Repositorio de rutinas: crear, listar con ejercicios, renombrar, editar descripción y borrado lógico
- [ ] 2.2 Gestión de ejercicios: agregar con objetivos, quitar y reordenar con posiciones densas en transacción
- [ ] 2.3 Validaciones: nombre obligatorio, ejercicio activo, sin duplicados, índices de reordenamiento válidos
- [ ] 2.4 Iniciar sesión desde rutina y exponer el nombre de la rutina en el detalle de sesión
- [ ] 2.5 Tests con `node:sqlite` de todas las operaciones y validaciones

## 3. Datos (Android)

- [ ] 3.1 `RoutinesDao` con rutinas y ejercicios de rutina (insertar, actualizar, borrado lógico, posiciones)
- [ ] 3.2 `RoutinesRepository` con las mismas reglas que Web, incluidas transacciones de reordenamiento
- [ ] 3.3 `startSession(routineId)` en el repositorio de entrenamiento y rutina de origen en el detalle
- [ ] 3.4 Tests Robolectric de CRUD, duplicados, reordenamiento, borrado y arranque de sesión

## 4. UI

- [ ] 4.1 Web: sección "Rutinas" con lista, alta/edición, gestión de ejercicios (agregar, quitar, subir/bajar) y botón "Entrenar"
- [ ] 4.2 Web: detalle de sesión mostrando la rutina de origen cuando existe
- [ ] 4.3 Android: pantalla "Rutinas" con las mismas capacidades y navegación desde la pantalla inicial
- [ ] 4.4 Android: detalle de sesión mostrando la rutina de origen cuando existe

## 5. Cierre

- [ ] 5.1 CI verde en Android y Web con los tests nuevos
- [ ] 5.2 Publicar release `v0.1.2` (versionCode 3) con rutinas
- [ ] 5.3 `openspec validate 05-rutinas-plantillas` y `openspec archive` del cambio
