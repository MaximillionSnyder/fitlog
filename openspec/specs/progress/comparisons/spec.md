# progress/comparisons Specification

## Purpose
Define la comparación de periodos de entrenamiento y el balance muscular por grupo de FitLog, con reglas idénticas en Android y Web.

## Requirements

### Requirement: Comparación de periodos

La aplicación SHALL comparar el periodo actual (últimos 30 o 90 días) contra el periodo inmediatamente anterior de igual duración, calculando volumen total, series efectivas y sesiones, con el porcentaje de cambio de cada métrica.

#### Scenario: Rangos contiguos sin solapamiento

- **WHEN** el usuario elige comparar los últimos 30 días con fecha actual T
- **THEN** el periodo actual cubre [T - 30 días, T] y el anterior cubre [T - 60 días, T - 30 días - 1 ms], sin solaparse

#### Scenario: Porcentaje de cambio

- **WHEN** el periodo actual tiene 2000 kg de volumen y el anterior 1600 kg
- **THEN** el cambio de volumen es +25 %

#### Scenario: Periodo anterior vacío

- **WHEN** el periodo anterior no tiene series efectivas
- **THEN** el cambio se informa como sin datos (nulo) y no como 0 % ni como infinito

#### Scenario: Conteo de sesiones

- **WHEN** un periodo tiene dos sesiones con series efectivas y una sesión sin series
- **THEN** el conteo de sesiones del periodo es 2

#### Scenario: Series de calentamiento y eliminadas

- **WHEN** un periodo contiene series de calentamiento o series eliminadas
- **THEN** no aportan a volumen, series efectivas ni sesiones

#### Scenario: Paridad de la comparación

- **WHEN** se ejecutan los casos de comparación de `shared/test-vectors/comparisons.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven los mismos totales y porcentajes

### Requirement: Balance muscular

La aplicación SHALL calcular, para el periodo elegido, el volumen efectivo por grupo muscular principal de cada ejercicio, su participación sobre el volumen total del periodo, y SHALL ordenar los grupos por volumen descendente.

#### Scenario: Volumen por grupo

- **WHEN** el periodo tiene 1200 kg de ejercicios de pecho y 800 kg de espalda
- **THEN** el balance muestra pecho con 1200 kg y 60 % y espalda con 800 kg y 40 %

#### Scenario: Ejercicio sin grupo conocido

- **WHEN** un ejercicio del periodo no tiene grupo muscular asociado
- **THEN** su volumen se agrupa bajo la clave "sin-grupo" en lugar de perderse

#### Scenario: Periodo sin volumen

- **WHEN** el periodo no tiene series efectivas completas
- **THEN** el balance es una lista vacía y la interfaz muestra un estado vacío explícito

#### Scenario: Paridad del balance

- **WHEN** se ejecutan los casos de balance de `shared/test-vectors/comparisons.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven los mismos grupos, volúmenes y participaciones
