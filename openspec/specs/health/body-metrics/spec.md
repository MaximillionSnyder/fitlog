# health/body-metrics Specification

## Purpose
Define el registro y seguimiento de medidas corporales de FitLog: tipos con su unidad, validaciones, historial con estadísticas y evolución temporal, idénticos en Android y Web.

## Requirements

### Requirement: Tipos de medida y unidad

La aplicación SHALL soportar los tipos de medida `body_weight`, `body_fat`, `waist`, `chest`, `arm`, `thigh`, `hip`, `neck` y `other`, y SHALL derivar la unidad del tipo: kilogramos para el peso corporal, porcentaje para la grasa corporal y centímetros para el resto.

#### Scenario: Unidad derivada

- **WHEN** se registra una medida de tipo `body_weight`
- **THEN** la medida queda con unidad `kg`

#### Scenario: Unidad de grasa corporal

- **WHEN** se registra una medida de tipo `body_fat`
- **THEN** la medida queda con unidad `%`

#### Scenario: Unidad de medidas de longitud

- **WHEN** se registra una medida de tipo `waist`
- **THEN** la medida queda con unidad `cm`

#### Scenario: Tipo desconocido

- **WHEN** se intenta registrar una medida con un tipo que no existe
- **THEN** la operación falla con un mensaje de error y no modifica la base

### Requirement: Registro de medidas

La aplicación SHALL registrar una medida con valor numérico mayor que cero, fecha de medición (por defecto el momento actual) y notas opcionales, y SHALL rechazar valores no finitos, negativos o cero y porcentajes de grasa mayores a 100.

#### Scenario: Registro válido

- **WHEN** el usuario registra 78,5 kg de peso corporal
- **THEN** la medida queda guardada con valor 78.5, unidad `kg`, fecha de medición y timestamps de creación y actualización

#### Scenario: Valor inválido

- **WHEN** el usuario intenta registrar 0 o un valor negativo
- **THEN** la operación falla con un mensaje de error y no modifica la base

#### Scenario: Grasa corporal fuera de rango

- **WHEN** el usuario intenta registrar 120 % de grasa corporal
- **THEN** la operación falla indicando que el porcentaje debe estar entre 0 y 100

#### Scenario: Fecha por defecto

- **WHEN** el usuario registra una medida sin indicar fecha
- **THEN** la fecha de medición es el momento del registro

### Requirement: Edición y borrado de medidas

La aplicación SHALL permitir editar el valor, la fecha y las notas de una medida existente, y SHALL permitir eliminarla con borrado lógico; las medidas eliminadas SHALL NOT aparecer en el historial ni en las estadísticas.

#### Scenario: Editar una medida

- **WHEN** el usuario corrige una medida de 78,5 a 78,2 kg
- **THEN** la medida queda con 78.2, `updated_at` renovado y el resto de los campos intactos

#### Scenario: Eliminar una medida

- **WHEN** el usuario elimina una medida
- **THEN** la fila conserva sus datos con `deleted_at` distinto de nulo y desaparece del historial y de las estadísticas

### Requirement: Historial y estadísticas por tipo

La aplicación SHALL mostrar el historial de medidas de un tipo ordenado por fecha descendente, y SHALL calcular sobre las medidas del tipo elegido en el periodo: cantidad, primera, última, mínimo, máximo y variación absoluta y porcentual entre la primera y la última.

#### Scenario: Historial ordenado

- **WHEN** el usuario abre el historial de peso corporal con tres mediciones
- **THEN** las ve de la más reciente a la más antigua con valor, unidad y fecha

#### Scenario: Estadísticas del periodo

- **WHEN** el periodo tiene mediciones de 80 kg, 79 kg y 78 kg
- **THEN** la primera es 80, la última 78, el mínimo 78, el máximo 80 y la variación es -2 kg (-2.5 %)

#### Scenario: Una sola medición

- **WHEN** el periodo tiene una sola medición
- **THEN** la variación absoluta y porcentual es 0 y el mínimo y el máximo coinciden con esa medición

#### Scenario: Sin mediciones

- **WHEN** el tipo elegido no tiene mediciones en el periodo
- **THEN** las estadísticas son nulas, la serie es vacía y la interfaz muestra un estado vacío explícito

#### Scenario: Serie de evolución

- **WHEN** el usuario elige un tipo con mediciones en el periodo
- **THEN** la aplicación muestra una gráfica con los puntos ordenados por fecha ascendente y la última medición como referencia

#### Scenario: Paridad de medidas

- **WHEN** se ejecutan los casos de `shared/test-vectors/body-metrics.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven las mismas unidades, validaciones, series y estadísticas
