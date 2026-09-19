## ADDED Requirements

### Requirement: Navegación de primer nivel con barra inferior

La app Android SHALL mostrar una barra de navegación inferior persistente con cinco destinos: Inicio, Entrenar, Progreso, Rutinas y Más. La barra SHALL indicar el destino activo y SHALL estar disponible en todas las pantallas de primer nivel.

#### Scenario: Destino activo

- **WHEN** el usuario está en la pantalla de Progreso
- **THEN** la barra inferior marca Progreso como destino activo

#### Scenario: Cambio de pestaña

- **WHEN** el usuario toca una pestaña de la barra inferior
- **THEN** la app muestra esa pantalla sin apilar una copia de la anterior

### Requirement: Conservación del estado por pestaña

La app Android SHALL conservar el estado de cada pestaña (posición de scroll, filtros y formularios) al cambiar de pestaña y volver.

#### Scenario: Volver a una pestaña con filtros

- **WHEN** el usuario filtra el catálogo, cambia a Entrenar y vuelve a la pestaña anterior
- **THEN** el catálogo conserva la búsqueda y los filtros que tenía

#### Scenario: Pila acotada

- **WHEN** el usuario alterna entre las cinco pestañas varias veces
- **THEN** la pila de navegación no acumula copias de las pantallas de primer nivel

### Requirement: Pantalla Más

La app Android SHALL agrupar en la pantalla Más los destinos que no son de primer nivel: Catálogo, Medidas, Comparativas, Tips, Respaldo y Ajustes. Cada entrada SHALL mostrar su nombre y una descripción breve.

#### Scenario: Abrir Catálogo desde Más

- **WHEN** el usuario toca la tarjeta de Catálogo en Más
- **THEN** se abre el catálogo con su encabezado y una acción para volver

#### Scenario: Volver desde una pantalla secundaria

- **WHEN** el usuario toca volver en una pantalla secundaria
- **THEN** vuelve a la pantalla desde la que la abrió, con su estado conservado

### Requirement: Encabezado de pantalla provisto por el shell

Las pantallas de la app Android SHALL usar el encabezado del shell con el título de la pantalla, una acción de volver en las secundarias y las acciones propias de cada pantalla.

#### Scenario: Encabezado de una pantalla secundaria

- **WHEN** el usuario abre Medidas desde Más
- **THEN** el encabezado muestra el título "Medidas" y una acción para volver

#### Scenario: Acción propia de la pantalla

- **WHEN** el usuario está en la pantalla de Entrenar con una sesión activa
- **THEN** el encabezado y la pantalla exponen la acción de finalizar la sesión

### Requirement: Panel de Inicio

La pantalla de Inicio SHALL funcionar como panel de resumen y no como índice de navegación. SHALL mostrar un hero con saludo y acción primaria de entrenamiento, un bento de estadísticas y accesos directos al contenido.

#### Scenario: Inicio sin sesión activa

- **WHEN** el usuario abre Inicio sin una sesión en curso
- **THEN** el hero ofrece iniciar un entrenamiento y el panel muestra las estadísticas de los últimos 7 días, la racha y el último peso corporal registrado

#### Scenario: Inicio con sesión activa

- **WHEN** el usuario abre Inicio con una sesión en curso
- **THEN** el hero ofrece continuar la sesión y la tarjeta de sesión activa muestra su duración, sus series y su volumen acumulado

#### Scenario: Comparación semanal

- **WHEN** el panel muestra las estadísticas de los últimos 7 días y existen datos de los 7 días anteriores
- **THEN** cada estadística con comparación muestra su variación con el token semántico que corresponde al signo

#### Scenario: Datos insuficientes

- **WHEN** el usuario todavía no registró entrenamientos
- **THEN** el panel muestra sus estadísticas en cero con un mensaje que invita a empezar, sin errores

### Requirement: Ajustes de la app

La app Android SHALL ofrecer una pantalla de Ajustes con el modo de tema, la opción de colores dinámicos y el estado de la base de datos (esquema, motor, archivo y grupos musculares).

#### Scenario: Cambiar el tema desde Ajustes

- **WHEN** el usuario elige el modo oscuro en Ajustes
- **THEN** la app cambia de tema sin reiniciarse

#### Scenario: Estado de la base de datos

- **WHEN** el usuario abre Ajustes
- **THEN** ve el estado de la base de datos con su versión de esquema, su motor, su archivo y la cantidad de grupos musculares
