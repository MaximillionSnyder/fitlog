## MODIFIED Requirements

### Requirement: Historial de sesiones

La aplicación SHALL mostrar el historial de sesiones ordenado por fecha de inicio descendente, incluyendo el resumen de cada sesión, y SHALL mostrar el detalle de una sesión con sus series agrupadas por ejercicio y la rutina de origen cuando la sesión se inició desde una rutina.

#### Scenario: Lista de historial

- **WHEN** el usuario abre el historial con varias sesiones registradas
- **THEN** las ve ordenadas de la más reciente a la más antigua con fecha, duración, cantidad de series efectivas y volumen total

#### Scenario: Detalle de sesión

- **WHEN** el usuario abre una sesión del historial
- **THEN** ve sus series agrupadas por ejercicio, con peso, repeticiones y RIR de cada una, y las de calentamiento identificadas

#### Scenario: Detalle de una sesión con rutina de origen

- **WHEN** el usuario abre una sesión que se inició desde la rutina "Día de empuje"
- **THEN** el detalle indica que la rutina de origen es "Día de empuje"

#### Scenario: Rutina de origen eliminada

- **WHEN** el usuario abre una sesión cuya rutina de origen fue eliminada
- **THEN** el detalle sigue funcionando y no muestra la rutina de origen

#### Scenario: Sesión sin series

- **WHEN** el usuario abre una sesión finalizada sin series
- **THEN** el detalle muestra un estado vacío explícito y el resumen con volumen 0

#### Scenario: Sesión activa en el historial

- **WHEN** el usuario tiene una sesión activa y abre el historial
- **THEN** la sesión activa aparece identificada como en curso y sin duración calculada
