## MODIFIED Requirements

### Requirement: Transiciones de elementos compartidos

La app Android SHALL animar la continuidad visual al navegar: el acceso de Inicio SHALL transformarse en el encabezado de la pantalla destino, y la tarjeta de un ejercicio del catálogo SHALL transformarse en el encabezado del detalle del ejercicio.

#### Scenario: Inicio hacia una pantalla

- **WHEN** el usuario toca el acceso "Progreso" en Inicio
- **THEN** la tarjeta del acceso se transforma visualmente en el encabezado "Progreso" de la pantalla destino durante la transición

#### Scenario: Catálogo hacia el detalle

- **WHEN** el usuario toca la tarjeta de un ejercicio en el catálogo
- **THEN** la tarjeta y el nombre del ejercicio se transforman visualmente en el encabezado del detalle durante la transición

#### Scenario: Volver del detalle

- **WHEN** el usuario vuelve del detalle de un ejercicio
- **THEN** la tarjeta recupera su lugar en la lista y el catálogo conserva la búsqueda, los filtros y la posición de scroll
