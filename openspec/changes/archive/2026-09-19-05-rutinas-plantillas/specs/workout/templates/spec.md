## Purpose

Define las rutinas de entrenamiento de FitLog: plantillas reutilizables con ejercicios ordenados y objetivos, su edición y borrado, y el arranque de sesiones de entrenamiento desde una rutina.

## ADDED Requirements

### Requirement: Gestión de rutinas

La aplicación SHALL permitir crear, listar, editar y eliminar rutinas con nombre obligatorio y descripción opcional, usando borrado lógico al eliminar.

#### Scenario: Crear una rutina

- **WHEN** el usuario crea la rutina "Día de empuje" con descripción "Pecho, hombros y tríceps"
- **THEN** la rutina queda guardada con un ULID nuevo, timestamps de creación y actualización, y sin ejercicios

#### Scenario: Nombre obligatorio

- **WHEN** el usuario intenta crear o renombrar una rutina con nombre vacío o solo espacios
- **THEN** la operación falla con un mensaje de error y no modifica la base

#### Scenario: Editar una rutina

- **WHEN** el usuario cambia el nombre o la descripción de una rutina existente
- **THEN** los cambios quedan guardados con `updated_at` renovado

#### Scenario: Eliminar una rutina

- **WHEN** el usuario elimina una rutina
- **THEN** la rutina y sus ejercicios quedan con `deleted_at` distinto de nulo y dejan de aparecer en la lista

#### Scenario: Borrar una rutina no altera el historial

- **WHEN** el usuario elimina una rutina que tiene sesiones registradas
- **THEN** las sesiones y sus series siguen intactas y su detalle sigue siendo accesible

### Requirement: Ejercicios de la rutina

La aplicación SHALL permitir agregar a una rutina ejercicios del catálogo con series objetivo, repeticiones objetivo, peso objetivo, descanso y notas opcionales, SHALL permitir quitarlos y reordenarlos, y SHALL NOT permitir el mismo ejercicio dos veces en la misma rutina.

#### Scenario: Agregar un ejercicio

- **WHEN** el usuario agrega "Press banca con barra" a una rutina con 4 series y 8 repeticiones objetivo
- **THEN** el ejercicio queda guardado como parte de la rutina con posición siguiente, series 4 y repeticiones 8

#### Scenario: Ejercicio duplicado

- **WHEN** el usuario agrega a la rutina un ejercicio que ya está en ella
- **THEN** la operación falla indicando que el ejercicio ya está en la rutina y no modifica la base

#### Scenario: Ejercicio inexistente o eliminado

- **WHEN** el usuario agrega un ejercicio que no existe o fue eliminado del catálogo
- **THEN** la operación falla indicando que el ejercicio no está disponible

#### Scenario: Reordenar ejercicios

- **WHEN** el usuario mueve el tercer ejercicio de la rutina a la primera posición
- **THEN** el orden de la rutina queda con ese ejercicio primero y las posiciones se renumeran de 1 a N sin huecos ni repetidos

#### Scenario: Quitar un ejercicio

- **WHEN** el usuario quita un ejercicio de la rutina
- **THEN** el ejercicio deja de aparecer en la rutina y las posiciones de los restantes se renumeran de 1 a N

#### Scenario: Reordenamiento con índices inválidos

- **WHEN** se intenta mover un ejercicio con un índice fuera del rango de la rutina
- **THEN** la operación falla y el orden de la rutina no cambia

#### Scenario: Paridad del reordenamiento

- **WHEN** se ejecutan los casos de `shared/test-vectors/routine-order.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven el mismo orden y las mismas posiciones

### Requirement: Iniciar entrenamiento desde una rutina

La aplicación SHALL permitir iniciar una sesión de entrenamiento indicando la rutina de origen, y la sesión SHALL guardar la referencia a esa rutina; iniciar una sesión con una rutina inexistente o eliminada SHALL fallar sin crear la sesión.

#### Scenario: Iniciar sesión desde una rutina

- **WHEN** el usuario toca "Entrenar" en la rutina "Día de empuje" y no tiene sesión activa
- **THEN** se crea una sesión activa con `routine_id` apuntando a esa rutina

#### Scenario: Rutina inexistente

- **WHEN** el usuario intenta iniciar una sesión con una rutina que no existe o fue eliminada
- **THEN** la operación falla con un mensaje de error y no se crea ninguna sesión

#### Scenario: Una sola sesión activa

- **WHEN** el usuario ya tiene una sesión activa e intenta iniciar otra desde una rutina
- **THEN** la operación falla indicando que ya hay una sesión en curso
