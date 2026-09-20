## ADDED Requirements

### Requirement: Lectura de la exportación de Huawei Health

La app SHALL leer los archivos JSON de una exportación de Huawei Health y reconocer sus registros de entrenamiento sin depender del nombre del archivo ni de la carpeta, ignorando en silencio los archivos que no son entrenamientos.

#### Scenario: Archivos de entrenamientos

- **WHEN** el usuario elige la carpeta de la exportación con registros de actividad
- **THEN** la app reconoce cada entrenamiento con su fecha de inicio, su duración y su tipo de deporte

#### Scenario: Archivos ajenos

- **WHEN** entre los archivos elegidos hay datos de sueño, pasos o estrés
- **THEN** la app los ignora y no informa un error

#### Scenario: Registros repetidos

- **WHEN** el mismo entrenamiento aparece más de una vez en la exportación
- **THEN** la app lo cuenta una sola vez

#### Scenario: Registro sin fecha

- **WHEN** un registro no tiene una fecha de inicio reconocible
- **THEN** la app lo descarta y sigue con el resto

### Requirement: Vista previa antes de importar

La app SHALL mostrar una vista previa con lo que encontró antes de escribir en la base: cantidad de entrenamientos, rango de fechas, tipos de deporte y cuántos ya están registrados.

#### Scenario: Vista previa con entrenamientos

- **WHEN** la lectura termina y encontró entrenamientos
- **THEN** la pantalla muestra cuántos son, entre qué fechas, los tipos con su cantidad y cuántos se van a saltear por ya estar en FitLog

#### Scenario: Nada para importar

- **WHEN** la lectura no encuentra entrenamientos
- **THEN** la pantalla lo dice y no ofrece importar

#### Scenario: Sin escribir nada

- **WHEN** el usuario ve la vista previa y no confirma
- **THEN** la base queda sin cambios

### Requirement: Importación de entrenamientos como sesiones

La app SHALL convertir cada entrenamiento reconocido en una sesión con su fecha de inicio, su fecha de fin y una nota con el tipo de deporte y los datos disponibles (distancia, calorías, frecuencia cardíaca y pasos).

#### Scenario: Entrenamiento con datos completos

- **WHEN** se importa un entrenamiento con distancia, calorías y frecuencia cardíaca
- **THEN** la sesión queda en el historial con su duración y una nota que resume esos datos

#### Scenario: Entrenamiento sin datos extra

- **WHEN** se importa un entrenamiento que solo tiene tipo y duración
- **THEN** la sesión queda igual, con la nota limitada a lo que existe

#### Scenario: Sesión sin series

- **WHEN** el usuario abre una sesión importada
- **THEN** la ve sin series registradas, con la nota del origen y la posibilidad de agregarlas

### Requirement: Importación repetible sin duplicados

La importación SHALL saltear los entrenamientos cuya fecha de inicio ya exista en FitLog, de modo que repetirla no duplique sesiones.

#### Scenario: Segunda importación del mismo archivo

- **WHEN** el usuario importa dos veces la misma exportación
- **THEN** la segunda vez no agrega sesiones y el resultado informa cuántas se saltearon

#### Scenario: Exportación más nueva

- **WHEN** el usuario importa una exportación posterior que incluye entrenamientos ya importados y otros nuevos
- **THEN** solo se agregan los nuevos

### Requirement: Resultado de la importación

La app SHALL informar al terminar cuántas sesiones se agregaron y cuántas se saltearon, y SHALL dejar el historial actualizado.

#### Scenario: Resumen al terminar

- **WHEN** la importación termina
- **THEN** la pantalla muestra cuántas sesiones se agregaron y cuántas se saltearon

#### Scenario: Historial actualizado

- **WHEN** el usuario vuelve al historial después de importar
- **THEN** las sesiones importadas aparecen con su fecha y su nota de origen
