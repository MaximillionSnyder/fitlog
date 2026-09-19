## ADDED Requirements

### Requirement: Sistema de tokens de color

La app Android SHALL definir sus colores en un único sistema de tokens: los roles de Material 3 más un grupo de marca (`accent`, `accentSoft`, `accentText`, `data`, `dataSoft`) y un grupo semántico (`success`, `warning`, `danger` con sus variantes suaves). Las pantallas SHALL obtener el color de esos tokens y no de literales repetidos.

#### Scenario: Color de una métrica

- **WHEN** una pantalla muestra el valor de una serie o de un volumen
- **THEN** el color del valor proviene del token de datos del sistema y cambia con el tema

#### Scenario: Color de un aviso

- **WHEN** una pantalla muestra un error, un aviso o una confirmación
- **THEN** el color proviene del token semántico correspondiente (`danger`, `warning`, `success`) y no de un valor fijo

### Requirement: Tema claro y oscuro con elección del usuario

La app Android SHALL ofrecer los modos de tema `sistema`, `claro` y `oscuro`, con `sistema` como valor inicial, y SHALL aplicar el modo elegido en toda la interfaz. La app SHALL ofrecer además colores dinámicos del sistema en Android 12 o superior.

#### Scenario: Seguir el sistema

- **WHEN** el usuario no cambió el modo de tema y el teléfono está en modo oscuro
- **THEN** la app se muestra en tema oscuro

#### Scenario: Forzar tema claro

- **WHEN** el usuario elige el modo claro con el teléfono en modo oscuro
- **THEN** la app se muestra en tema claro en todas sus pantallas

#### Scenario: Colores dinámicos

- **WHEN** el usuario activa los colores dinámicos en Android 12 o superior
- **THEN** los roles de Material 3 toman la paleta del sistema y los tokens de marca y semánticos se mantienen legibles

#### Scenario: Preferencia persistente

- **WHEN** el usuario elige un modo de tema y vuelve a abrir la app
- **THEN** la app arranca con el modo elegido

### Requirement: Tipografía y formas del sistema

La app Android SHALL usar una escala tipográfica propia con cifras tabulares para los valores numéricos y una escala de formas y espaciado compartida, en lugar de tamaños y radios sueltos por pantalla.

#### Scenario: Valores numéricos alineados

- **WHEN** una pantalla muestra valores numéricos en una lista o en un tile
- **THEN** los valores usan cifras tabulares y conservan la alineación entre filas

### Requirement: Biblioteca de componentes de interfaz

La app Android SHALL ofrecer primitivas compartidas para tarjeta, tile de estadística, encabezado de sección, acción primaria e icono, y las pantallas SHALL construirse con ellas.

#### Scenario: Tile de estadística

- **WHEN** una pantalla muestra un dato con etiqueta y variación
- **THEN** usa el tile de estadística, que muestra etiqueta, valor, unidad y delta opcional con el token semántico que corresponde al signo

#### Scenario: Iconos

- **WHEN** una pantalla necesita un icono
- **THEN** usa el set de iconos propio de la app, sin agregar dependencias de iconos

#### Scenario: Radios y contenedores consistentes

- **WHEN** dos pantallas distintas muestran una tarjeta
- **THEN** ambas usan la misma primitiva de tarjeta con el mismo radio, borde y color de superficie

### Requirement: Estados de carga, error y vacío unificados

Las listas y paneles de la app Android SHALL mostrar estados de carga, error y vacío con las primitivas compartidas, y los estados vacíos SHALL ofrecer una acción cuando exista un paso siguiente.

#### Scenario: Lista vacía con acción

- **WHEN** el usuario abre una lista sin datos y existe una acción para crearla
- **THEN** el estado vacío muestra el mensaje y el botón de esa acción

#### Scenario: Error de carga

- **WHEN** una pantalla falla al cargar sus datos
- **THEN** muestra el mensaje de error con el token de peligro y una acción para reintentar
