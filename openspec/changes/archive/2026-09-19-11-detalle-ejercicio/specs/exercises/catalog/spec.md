## ADDED Requirements

### Requirement: Detalle de ejercicio

La interfaz del catálogo SHALL abrir el detalle de un ejercicio al seleccionar su tarjeta, y SHALL mostrar el nombre, la marca de ejercicio propio, el tipo (`Fuerza`, `Cardio`, `Movilidad`), el grupo muscular principal, el grupo muscular secundario cuando exista y el equipamiento. El detalle SHALL estar disponible en Android y Web, y cerrarlo SHALL volver a la lista conservando la búsqueda y los filtros activos.

#### Scenario: Detalle de un ejercicio base

- **WHEN** el usuario selecciona la tarjeta de un ejercicio del catálogo base
- **THEN** el detalle muestra su nombre, tipo, grupo muscular principal y equipamiento, y no muestra la marca de ejercicio propio

#### Scenario: Detalle de un ejercicio propio

- **WHEN** el usuario selecciona la tarjeta de un ejercicio con `is_custom` en 1
- **THEN** el detalle muestra su nombre, tipo, grupo muscular principal, equipamiento y la marca de ejercicio propio

#### Scenario: Ejercicio con grupo muscular secundario

- **WHEN** el usuario selecciona un ejercicio cuyo grupo muscular secundario está definido
- **THEN** el detalle muestra tanto el grupo muscular principal como el secundario

#### Scenario: Ejercicio sin grupo muscular secundario

- **WHEN** el usuario selecciona un ejercicio cuyo grupo muscular secundario es nulo
- **THEN** el detalle muestra solo el grupo muscular principal

#### Scenario: Cerrar el detalle

- **WHEN** el usuario cierra el detalle de un ejercicio
- **THEN** vuelve a la lista del catálogo con la búsqueda y los filtros que tenía antes de abrirlo
