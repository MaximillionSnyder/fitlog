## Purpose

Define el registro de entrenamiento de FitLog: sesiones con inicio y fin, series con peso, repeticiones, RIR, notas y calentamiento, edición y borrado lógico, historial con resumen y detalle por ejercicio.

## ADDED Requirements

### Requirement: Sesión de entrenamiento

La aplicación SHALL permitir iniciar una sesión con timestamp de inicio y SHALL permitir finalizarla registrando el timestamp de fin, y SHALL NOT permitir más de una sesión activa (sin finalizar) a la vez.

#### Scenario: Iniciar una sesión

- **WHEN** el usuario toca "Iniciar entrenamiento" sin sesión activa
- **THEN** se crea una sesión con `started_at` en el momento actual, `finished_at` nulo y un ULID nuevo

#### Scenario: Sesión única activa

- **WHEN** el usuario ya tiene una sesión activa
- **THEN** la aplicación muestra esa sesión en lugar de ofrecer iniciar otra

#### Scenario: Finalizar una sesión

- **WHEN** el usuario finaliza la sesión activa
- **THEN** la sesión queda con `finished_at` en el momento actual y deja de estar activa

#### Scenario: Series tras finalizar

- **WHEN** el usuario intenta registrar una serie en una sesión finalizada
- **THEN** la operación falla indicando que la sesión no está activa y no modifica la base

### Requirement: Registro de series

La aplicación SHALL permitir registrar series en la sesión activa con ejercicio del catálogo, peso en kg, repeticiones, RIR opcional, notas opcionales y marca de calentamiento, asignando el índice de serie automáticamente.

#### Scenario: Registrar una serie

- **WHEN** el usuario registra 100 kg por 8 repeticiones del ejercicio "Press banca con barra"
- **THEN** se guarda una serie con esos valores, `is_warmup` en 0, `set_index` 1 y timestamps de creación y actualización

#### Scenario: Índice automático

- **WHEN** el usuario registra tres series del mismo ejercicio en la misma sesión
- **THEN** los índices resultantes son 1, 2 y 3 en orden de registro

#### Scenario: Serie de calentamiento

- **WHEN** el usuario marca una serie como calentamiento
- **THEN** la serie se guarda con `is_warmup` en 1 y no cuenta como serie efectiva en el resumen

#### Scenario: Validación de la serie

- **WHEN** el usuario intenta registrar una serie con peso negativo, repeticiones negativas o sin ejercicio
- **THEN** la operación falla con un mensaje de error y no modifica la base

#### Scenario: Ejercicio inexistente

- **WHEN** el usuario registra una serie con un ejercicio que no existe o fue eliminado
- **THEN** la operación falla indicando que el ejercicio no está disponible

### Requirement: Edición y borrado de series

La aplicación SHALL permitir editar peso, repeticiones, RIR y notas de cualquier serie registrada, y SHALL permitir eliminar series con borrado lógico; las series eliminadas SHALL NOT aparecer en el historial ni en los cálculos.

#### Scenario: Editar una serie

- **WHEN** el usuario corrige el peso de una serie de 100 a 102,5 kg
- **THEN** la serie queda con 102,5 kg, `updated_at` renovado y el resto de los campos intactos

#### Scenario: Eliminar una serie

- **WHEN** el usuario elimina una serie
- **THEN** la fila conserva sus datos con `deleted_at` distinto de nulo y desaparece del historial y del resumen

#### Scenario: Borrado no afecta a otras series

- **WHEN** el usuario elimina la segunda de tres series
- **THEN** las otras dos series siguen presentes y sus índices no cambian

### Requirement: Historial de sesiones

La aplicación SHALL mostrar el historial de sesiones ordenado por fecha de inicio descendente, incluyendo el resumen de cada sesión, y SHALL mostrar el detalle de una sesión con sus series agrupadas por ejercicio.

#### Scenario: Lista de historial

- **WHEN** el usuario abre el historial con varias sesiones registradas
- **THEN** las ve ordenadas de la más reciente a la más antigua con fecha, duración, cantidad de series efectivas y volumen total

#### Scenario: Detalle de sesión

- **WHEN** el usuario abre una sesión del historial
- **THEN** ve sus series agrupadas por ejercicio, con peso, repeticiones y RIR de cada una, y las de calentamiento identificadas

#### Scenario: Sesión sin series

- **WHEN** el usuario abre una sesión finalizada sin series
- **THEN** el detalle muestra un estado vacío explícito y el resumen con volumen 0

#### Scenario: Sesión activa en el historial

- **WHEN** el usuario tiene una sesión activa y abre el historial
- **THEN** la sesión activa aparece identificada como en curso y sin duración calculada

### Requirement: Resumen de sesión

El resumen de una sesión SHALL calcular series totales, series efectivas (sin calentamiento), volumen total en kg y volumen por ejercicio, y SHALL ser idéntico en Android y Web según los casos compartidos en `shared/test-vectors/session-summary.json`.

#### Scenario: Volumen total

- **WHEN** una sesión tiene 100 kg x 8, 100 kg x 6 y 40 kg x 10 de calentamiento
- **THEN** el volumen total es 1400 kg, las series efectivas son 2 y las totales 3

#### Scenario: Volumen por ejercicio

- **WHEN** la sesión combina dos ejercicios
- **THEN** el volumen por ejercicio suma solo las series efectivas de cada ejercicio

#### Scenario: Series incompletas

- **WHEN** una serie no tiene peso o no tiene repeticiones
- **THEN** no aporta volumen y no rompe el cálculo del resumen

#### Scenario: Paridad entre plataformas

- **WHEN** se ejecutan los casos de `shared/test-vectors/session-summary.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven los mismos totales, series efectivas y volúmenes por ejercicio
