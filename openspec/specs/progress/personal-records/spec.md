# progress/personal-records Specification

## Purpose
Define las marcas personales de FitLog por ejercicio, con la fecha en que se lograron y reglas idénticas en Android y Web.

## Requirements

### Requirement: Marcas personales por ejercicio

La aplicación SHALL calcular, para cada ejercicio con series efectivas, la mejor marca de peso, de 1RM estimado, de volumen de sesión y de repeticiones, junto con la fecha en que cada una se logró, excluyendo series de calentamiento, series eliminadas y series sin peso o sin repeticiones según corresponda.

#### Scenario: Mejor peso

- **WHEN** un ejercicio tiene series efectivas de 100 kg x 8 y 105 kg x 5 en sesiones distintas
- **THEN** la marca de mejor peso es 105 kg con la fecha de la sesión donde se logró

#### Scenario: Mejor 1RM estimado

- **WHEN** un ejercicio tiene series efectivas de 100 kg x 8 (1RM 126,7) y 105 kg x 5 (1RM 122,5)
- **THEN** la marca de mejor 1RM es 126,7 kg con la fecha de la primera serie

#### Scenario: Mejor volumen de sesión

- **WHEN** un ejercicio acumula 1400 kg en una sesión y 1600 kg en otra
- **THEN** la marca de mejor volumen es 1600 kg con la fecha de la segunda sesión

#### Scenario: Mejores repeticiones

- **WHEN** un ejercicio tiene series efectivas de 100 kg x 12 y 80 kg x 15
- **THEN** la marca de mejores repeticiones es 15 con la fecha de la serie correspondiente

#### Scenario: Empate de marcas

- **WHEN** dos sesiones alcanzan el mismo peso máximo
- **THEN** la marca conserva la fecha más antigua entre las que empatan

#### Scenario: Series de calentamiento y eliminadas

- **WHEN** una serie de calentamiento o una serie eliminada supera a las demás en peso
- **THEN** esa serie no se considera para ninguna marca personal

#### Scenario: Ejercicio sin series completas

- **WHEN** un ejercicio solo tiene series sin peso o sin repeticiones
- **THEN** sus marcas de peso, 1RM y volumen son 0 sin fecha, y la marca de repeticiones se calcula con las series que tengan repeticiones

### Requirement: Listado de marcas personales

La aplicación SHALL mostrar las marcas personales agrupadas por ejercicio, ordenadas por mejor 1RM descendente, indicando el valor y la fecha de cada marca.

#### Scenario: Orden del listado

- **WHEN** hay marcas de varios ejercicios
- **THEN** el listado ordena los ejercicios por mejor 1RM estimado de mayor a menor

#### Scenario: Sin marcas

- **WHEN** no hay series efectivas registradas
- **THEN** la interfaz muestra un estado vacío explícito

#### Scenario: Paridad de las marcas

- **WHEN** se ejecutan los casos de marcas de `shared/test-vectors/comparisons.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven los mismos valores y fechas
