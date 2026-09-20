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

### Requirement: Lectura de archivos GPX

La app SHALL leer archivos GPX además de la exportación de Huawei Health, y SHALL obtener de ellos la fecha de inicio, la fecha de fin, la duración, la distancia recorrida, la frecuencia cardíaca y el desnivel cuando estén presentes.

#### Scenario: GPX con puntos y tiempos

- **WHEN** el usuario elige un GPX con puntos que tienen fecha
- **THEN** la app reconoce un entrenamiento con su inicio, su fin y su duración

#### Scenario: GPX sin frecuencia cardíaca

- **WHEN** el GPX no trae lecturas de frecuencia cardíaca
- **THEN** el entrenamiento se importa igual y la nota omite ese dato

#### Scenario: GPX sin tiempos por punto

- **WHEN** el GPX no tiene al menos dos puntos con fecha
- **THEN** la app lo descarta y sigue con el resto

#### Scenario: Formatos mezclados

- **WHEN** el usuario elige una carpeta con la exportación de Huawei Health y archivos GPX
- **THEN** la app lee los dos formatos y los suma a la misma vista previa

### Requirement: Origen de cada sesión importada

La nota de una sesión importada SHALL empezar con el origen del dato, y el historial SHALL marcar esas sesiones como importadas.

#### Scenario: Origen Huawei Health

- **WHEN** se importa un entrenamiento de la exportación de Huawei Health
- **THEN** su nota empieza con "Huawei Health" y la sesión se marca como importada en el historial

#### Scenario: Origen GPX

- **WHEN** se importa un entrenamiento de un GPX cuyo creador no es Huawei
- **THEN** su nota empieza con "GPX" y la sesión se marca como importada en el historial
