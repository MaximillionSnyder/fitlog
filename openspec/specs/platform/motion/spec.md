# platform/motion Specification

## Purpose
Define el comportamiento de movimiento de la app Android de FitLog: transiciones de elementos compartidos al navegar, formas que mutan, fondos orgánicos animados y respeto por la preferencia de movimiento reducido del sistema.

## Requirements

### Requirement: Transiciones de elementos compartidos

La app Android SHALL animar la continuidad visual al navegar: el botón de Inicio SHALL transformarse en el encabezado de la pantalla destino, y la tarjeta de un ejercicio del catálogo SHALL transformarse en el encabezado del detalle del ejercicio.

#### Scenario: Inicio hacia una pantalla

- **WHEN** el usuario toca el botón "Progreso" en Inicio
- **THEN** el botón se transforma visualmente en el encabezado "Progreso" de la pantalla destino durante la transición

#### Scenario: Catálogo hacia el detalle

- **WHEN** el usuario toca la tarjeta de un ejercicio en el catálogo
- **THEN** la tarjeta y el nombre del ejercicio se transforman visualmente en el encabezado del detalle durante la transición

#### Scenario: Volver del detalle

- **WHEN** el usuario vuelve del detalle de un ejercicio
- **THEN** la tarjeta recupera su lugar en la lista y el catálogo conserva la búsqueda, los filtros y la posición de scroll

### Requirement: Detalle de ejercicio en pantalla

En Android, el detalle de un ejercicio SHALL abrirse como una pantalla propia con su encabezado y sus datos, en lugar de un diálogo.

#### Scenario: Datos del detalle

- **WHEN** el usuario abre el detalle de un ejercicio
- **THEN** la pantalla muestra nombre, marca de propio cuando corresponde, tipo, grupo muscular principal, grupo muscular secundario cuando existe y equipamiento

#### Scenario: Volver al catálogo

- **WHEN** el usuario toca "Volver" en el detalle
- **THEN** vuelve a la pantalla del catálogo en el mismo estado en que la dejó

### Requirement: Formas que mutan

El botón de sesión de la pantalla Entrenar SHALL mutar su forma y su icono entre el estado sin sesión (iniciar, icono de reproducción) y el estado con sesión activa (finalizar, icono de detención).

#### Scenario: Iniciar sesión

- **WHEN** el usuario toca "Iniciar" y comienza una sesión
- **THEN** el botón muta a la forma e icono de "Finalizar"

#### Scenario: Finalizar sesión

- **WHEN** el usuario finaliza la sesión activa
- **THEN** el botón vuelve a la forma e icono de "Iniciar"

### Requirement: Fondos orgánicos animados

La app Android SHALL mostrar blobs orgánicos animados en el hero de Inicio, como fondo de tarjetas de resumen y en los estados vacíos de las listas.

#### Scenario: Estado vacío

- **WHEN** una lista del catálogo, entrenamientos, rutinas, progreso, medidas, tips o comparativas no tiene datos
- **THEN** el estado vacío muestra un blob animado junto al mensaje en lugar de un texto o tarjeta sin ilustración

#### Scenario: Hero de Inicio

- **WHEN** el usuario abre la pantalla de Inicio
- **THEN** el encabezado "FitLog" se muestra sobre blobs animados de bajo contraste

### Requirement: Movimiento reducido

La app Android SHALL respetar la preferencia de movimiento reducido del sistema: con las animaciones desactivadas, las pantallas SHALL seguir siendo usables, la información SHALL mostrarse completa y los elementos animados SHALL renderizarse estáticos.

#### Scenario: Animaciones desactivadas

- **WHEN** la escala de animación del sistema está en cero
- **THEN** la app navega, muestra el catálogo, el detalle, el botón de sesión y los blobs sin depender de la animación y sin perder información
