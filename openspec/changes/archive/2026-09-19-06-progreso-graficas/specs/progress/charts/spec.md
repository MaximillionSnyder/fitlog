## Purpose

Define las series de progreso de FitLog: agregación de las series registradas por sesión y ejercicio, filtro por rango temporal y visualización de la evolución de peso, volumen y 1RM estimado.

## ADDED Requirements

### Requirement: Serie de progreso por ejercicio

La aplicación SHALL calcular, para un ejercicio dado, un punto por cada sesión con series efectivas de ese ejercicio, ordenados por fecha de inicio ascendente, y SHALL excluir las series de calentamiento y las eliminadas.

#### Scenario: Un punto por sesión

- **WHEN** el ejercicio tiene series en tres sesiones distintas
- **THEN** la serie de progreso contiene exactamente tres puntos ordenados de la sesión más antigua a la más reciente

#### Scenario: Calentamientos y series eliminadas

- **WHEN** una sesión tiene una serie de calentamiento y una serie eliminada del ejercicio
- **THEN** ninguna de las dos aporta al punto de esa sesión

#### Scenario: Ejercicio sin datos

- **WHEN** el ejercicio no tiene series efectivas registradas
- **THEN** la serie de progreso es una lista vacía y la interfaz muestra un estado vacío explícito

### Requirement: Métricas por punto

Cada punto de la serie SHALL incluir la fecha de la sesión, el peso máximo en kg, el volumen total en kg, el mejor 1RM estimado (Epley) y la cantidad de series efectivas, calculados solo con series que tengan peso y repeticiones.

#### Scenario: Peso máximo y mejor 1RM

- **WHEN** una sesión tiene series efectivas de 100 kg x 8 y 105 kg x 5 del mismo ejercicio
- **THEN** el punto registra peso máximo 105 kg y mejor 1RM estimado 122,5 kg

#### Scenario: Volumen del punto

- **WHEN** una sesión tiene series efectivas de 100 kg x 8 y 100 kg x 6
- **THEN** el volumen del punto es 1400 kg

#### Scenario: Series incompletas

- **WHEN** una sesión tiene una serie efectiva sin peso y otra con peso y repeticiones
- **THEN** el punto cuenta ambas como series efectivas, el peso máximo y el 1RM salen de la serie completa, y el volumen solo suma la serie completa

#### Scenario: Sesión sin series completas

- **WHEN** todas las series efectivas de la sesión carecen de peso o de repeticiones
- **THEN** el punto tiene peso máximo 0, 1RM 0 y volumen 0, y mantiene la cantidad de series efectivas

### Requirement: Filtro por rango temporal

La aplicación SHALL permitir filtrar la serie por rango temporal con opciones de 30 días, 90 días y todo el historial, comparando contra la fecha de inicio de cada sesión con límites inclusivos.

#### Scenario: Últimos 30 días

- **WHEN** el usuario elige "30 días" y hay sesiones de hoy, de hace 30 días y de hace 31 días
- **THEN** la serie incluye las sesiones de hoy y de hace 30 días, y excluye la de hace 31 días

#### Scenario: Todo el historial

- **WHEN** el usuario elige "Todo"
- **THEN** la serie incluye todas las sesiones sin filtrar por fecha

#### Scenario: Rango sin datos

- **WHEN** el rango elegido no contiene sesiones del ejercicio
- **THEN** la interfaz muestra el estado vacío del rango en lugar de una gráfica vacía

### Requirement: Visualización de la gráfica

La aplicación SHALL mostrar la serie de progreso como gráfica de líneas en Android y Web, con selector de ejercicio y selector de métrica entre peso máximo, volumen y 1RM estimado, y SHALL mostrar el valor del último punto como referencia.

#### Scenario: Cambio de métrica

- **WHEN** el usuario cambia la métrica de peso máximo a volumen
- **THEN** la gráfica se redibuja con los valores de volumen de los mismos puntos

#### Scenario: Cambio de ejercicio

- **WHEN** el usuario elige otro ejercicio
- **THEN** la gráfica muestra la serie de progreso del nuevo ejercicio

#### Scenario: Valor de referencia

- **WHEN** la gráfica se muestra con al menos un punto
- **THEN** se indica el valor de la métrica en el punto más reciente

### Requirement: Paridad de la serie de progreso

El cálculo de la serie de progreso SHALL ser idéntico en Android y Web según los casos compartidos en `shared/test-vectors/progress-series.json`.

#### Scenario: Casos compartidos

- **WHEN** se ejecutan los casos de `shared/test-vectors/progress-series.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven los mismos puntos, métricas y filtrados por rango
