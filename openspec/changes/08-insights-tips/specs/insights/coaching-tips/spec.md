## Purpose

Define el motor de consejos de FitLog: reglas deterministas sobre los datos del periodo, con severidad, sujeto y valor de referencia, idénticas en Android y Web.

## ADDED Requirements

### Requirement: Consejos por reglas deterministas

La aplicación SHALL generar consejos a partir de las series efectivas del periodo elegido aplicando reglas con umbrales fijos, y SHALL NOT inventar consejos cuando no se cumple ninguna regla.

#### Scenario: Sin datos suficientes

- **WHEN** el periodo no tiene series efectivas
- **THEN** la lista de consejos es vacía y la interfaz muestra un estado vacío explícito

#### Scenario: Consejo de progreso

- **WHEN** un ejercicio tiene al menos 3 sesiones en el periodo y su mejor 1RM estimado de la última sesión supera al de la primera
- **THEN** se genera un consejo de progreso con severidad de logro, sujeto el ejercicio y el porcentaje de mejora

#### Scenario: Consejo de estancamiento

- **WHEN** un ejercicio tiene al menos 4 sesiones en el periodo y la variación de su mejor 1RM entre la primera y la última sesión es menor o igual a 0,5 %
- **THEN** se genera un consejo de estancamiento con severidad de advertencia, sujeto el ejercicio y la cantidad de sesiones

#### Scenario: Consejo de desbalance muscular

- **WHEN** el periodo tiene volumen en al menos dos grupos musculares y el grupo principal concentra el 50 % o más del volumen total
- **THEN** se genera un consejo de desbalance con severidad de advertencia, sujeto el grupo y su participación

#### Scenario: Consejo de buena frecuencia

- **WHEN** el periodo tiene al menos 3 sesiones por semana en promedio
- **THEN** se genera un consejo de frecuencia con severidad de logro y el promedio semanal

#### Scenario: Consejo de frecuencia baja

- **WHEN** el periodo tiene menos de 1,5 sesiones por semana en promedio
- **THEN** se genera un consejo de frecuencia con severidad de advertencia y la cantidad de sesiones

#### Scenario: Consejo de datos incompletos

- **WHEN** el periodo tiene al menos 5 series efectivas y el 30 % o más de ellas no tiene peso o no tiene repeticiones
- **THEN** se genera un consejo de calidad de datos con severidad informativa y el porcentaje de series incompletas

### Requirement: Estructura y orden de los consejos

Cada consejo SHALL incluir tipo, severidad, sujeto (ejercicio, grupo muscular o nulo), valor numérico de referencia y un mensaje en español con los números formateados de forma determinista, y la lista SHALL ordenarse por severidad (advertencias, luego información, luego logros) y, dentro de cada severidad, por tipo y sujeto.

#### Scenario: Orden por severidad

- **WHEN** la lista contiene un logro y una advertencia
- **THEN** la advertencia aparece antes que el logro

#### Scenario: Formato de números

- **WHEN** un consejo referencia 25,0 y otro 57,14
- **THEN** los mensajes muestran "25" y "57.1" respectivamente, iguales en Android y Web

#### Scenario: Sujeto del consejo

- **WHEN** el consejo es de desbalance muscular
- **THEN** su sujeto es el slug del grupo muscular y la interfaz muestra el nombre del grupo

### Requirement: Paridad del motor de consejos

Las reglas, umbrales, textos y orden SHALL ser idénticos en Android y Web según los casos compartidos en `shared/test-vectors/insights.json`.

#### Scenario: Casos compartidos

- **WHEN** se ejecutan los casos de `shared/test-vectors/insights.json` en Android (JUnit) y en Web (Vitest)
- **THEN** ambas plataformas devuelven exactamente los mismos consejos, en el mismo orden y con los mismos mensajes
