## Contexto

FitLog guarda entrenamientos como sesiones con series (peso, reps, RIR). Huawei Health exporta **actividades**: un JSON por entrenamiento grabado, con tipo de deporte, inicio, fin, duración, calorías, distancia, pasos y frecuencia cardíaca, más un blob de ruta GPS. No exporta series con peso y reps.

La exportación se pide desde la app de Huawei Health (privacidad → solicitar tus datos), llega como ZIP y, al descomprimirla, queda una carpeta raíz con subcarpetas; los entrenamientos viven en `Motion path detail data & description`, un JSON por registro. El formato de los campos es estable (nombres en inglés) y está documentado por la comunidad: `recordId`, `startTime`/`endTime` (epoch en milisegundos, aunque algunas versiones exportan segundos), `sportType` (entero), `totalTime` (milisegundos), `totalCalories` (escalado por 1000), `totalDistance` (metros), `totalSteps`, `totalStoreys`, `avgHeartRate`, `maxHeartRate`, `minHeartRate`, `attribute` (ruta y sensores).

## Decisiones

### Leer carpetas, no un archivo

El usuario elige la **carpeta** de la exportación (o varios JSON a la vez) y la app lee todos los `.json` que encuentre. Elegir un solo archivo obligaría a saber cuál es, y la exportación reparte los entrenamientos en muchos archivos con nombres largos y localizados. Android usa el selector de árbol del sistema (`OpenDocumentTree`) y la web el selector de carpeta (`webkitdirectory`), sin dependencias nuevas.

### Lector tolerante, no un parser de formato cerrado

El lector recorre el JSON y reconoce un entrenamiento cuando el objeto tiene una fecha de inicio reconocible y algún indicio de duración o fin. Acepta:

- la raíz como lista, como objeto con la lista adentro, o como JSON por líneas (la exportación tiene archivos con un objeto por línea);
- `startTime`/`endTime` en milisegundos o en segundos (se distingue por magnitud);
- fechas en texto ISO o `yyyy-MM-dd HH:mm:ss` además de epoch;
- duración en `totalTime` (milisegundos) o calculada del fin menos el inicio;
- nombres alternativos de los campos (`sportType`, `sport_type`, `activityType`, `exerciseType`).

Todo lo que no se reconoce se ignora en silencio: la exportación trae carpetas de sueño, pasos y estrés que no son entrenamientos y no deben romper la lectura.

### Deduplicación por `recordId` y por fecha

La exportación **triplica** los registros (el mismo entrenamiento aparece en varios archivos). El lector deduplica por `recordId` y, si no hay id, por la tupla inicio + tipo. En la base se saltea toda sesión cuya `started_at` ya exista: dos entrenamientos no empiezan en el mismo milisegundo, así que es una clave natural suficiente y evita agregar una columna de origen externo al esquema.

### Los entrenamientos entran como sesiones sin series

Una sesión de FitLog sin series es válida (una sesión libre recién iniciada lo está). El resumen de esas sesiones queda en cero, lo cual es honesto: Huawei no registró peso ni reps. La información que sí existe se guarda en la nota de la sesión, en una línea legible:

```
Huawei Health · Running · 5.24 km · 320 kcal · FC 145/172 · 6 800 pasos
```

Así el historial muestra de dónde vino cada sesión y el usuario puede abrirla y agregar series si quiere completarla.

### Vista previa antes de escribir

La importación no toca la base hasta que el usuario confirma. La vista previa muestra cuántos entrenamientos se encontraron, el rango de fechas, los tipos con su cantidad y cuántos ya están en FitLog (se van a saltear). Es la diferencia entre "importar a ciegas" y saber qué va a pasar; además hace visible si el archivo elegido no era el correcto (por ejemplo, si dice "0 entrenamientos").

### Sin cambios de esquema

La importación escribe sesiones con las columnas existentes (`id`, `started_at`, `finished_at`, `notes`, `created_at`, `updated_at`). No hace falta una tabla de origen ni una columna de id externo: la idempotencia se resuelve por fecha y el origen queda en la nota. Esto mantiene la paridad Android↔Web y no obliga a migrar bases existentes.

## Riesgos y mitigaciones

- **Formato que cambia entre versiones de Huawei Health**: el lector es tolerante por diseño y la vista previa muestra el resultado antes de escribir; si un campo falta, se importa lo que sí se reconoce.
- **Exportaciones enormes** (cientos de archivos): la lectura corre fuera del hilo principal en Android (IO) y se procesa archivo por archivo, sin cargar todo en memoria a la vez; la vista previa solo conserva el resumen.
- **Fechas duplicadas legítimas**: dos entrenamientos distintos no comparten milisegundo de inicio; si ocurriera, se saltea el segundo y la vista previa lo informa como "ya estaba".

## Alternativas descartadas

- **Importar solo el archivo de rutas GPS**: deja afuera los entrenamientos sin GPS (piscina, indoor), que son la mitad de la historia.
- **Pedir un CSV**: Huawei no exporta CSV de entrenamientos.
- **Guardar el id de Huawei en el esquema**: obliga a migrar las dos plataformas y rompe la paridad por un dato que solo hace falta para deduplicar, y eso ya se resuelve con la fecha.
- **Importar también peso corporal y sueño**: es otra conversación (y otra pantalla); este cambio se limita a entrenamientos.
