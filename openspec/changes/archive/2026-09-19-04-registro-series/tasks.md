## 1. Vectores y dominio

- [x] 1.1 Crear `shared/test-vectors/session-summary.json` con casos de volumen total, series efectivas, volumen por ejercicio y series incompletas
- [x] 1.2 Implementar el resumen de sesión en el dominio de Web (TypeScript) con test contra los vectores
- [x] 1.3 Implementar el resumen de sesión en el dominio de Android (Kotlin) con test contra los vectores

## 2. Datos y lógica de entrenamiento (Web)

- [x] 2.1 Definir el repositorio de entrenamiento sobre Drizzle: sesión activa, iniciar, finalizar
- [x] 2.2 Implementar registro de series con índice automático y validaciones (peso, reps, ejercicio activo, sesión activa)
- [x] 2.3 Implementar edición y borrado lógico de series
- [x] 2.4 Implementar consultas de historial (sesiones + resumen) y detalle por sesión
- [x] 2.5 Tests con `node:sqlite`: inicio/fin, índice automático, validaciones, edición, borrado, historial y detalle

## 3. UI Web

- [x] 3.1 Sección "Entrenar" con sesión activa: selector de ejercicio del catálogo, campos de peso/reps/RIR/notas y marca de calentamiento
- [x] 3.2 Lista de series registradas con edición y eliminación, y botón de finalizar sesión
- [x] 3.3 Historial de sesiones con resumen y detalle por ejercicio
- [x] 3.4 Estados vacíos (sin sesión activa, sesión sin series) y mensajes de error de validación

## 4. Datos y UI Android

- [x] 4.1 `WorkoutDao` con sesiones y series (insertar, actualizar, borrado lógico, consultas de activa e historial)
- [x] 4.2 `WorkoutRepository` con las mismas reglas que Web (índice automático, validaciones, resumen)
- [x] 4.3 Tests Robolectric: sesión única activa, índice automático, validaciones, edición, borrado e historial
- [x] 4.4 Pantalla "Entrenar" con registro de series y finalización, y pantalla de historial con detalle
- [x] 4.5 Navegación desde la pantalla inicial a Entrenar e Historial

## 5. Cierre

- [x] 5.1 CI verde en Android y Web con los tests nuevos
- [x] 5.2 Publicar release `v0.1.1` (versionCode 2) con el registro de series
- [x] 5.3 `openspec validate 04-registro-series` y `openspec archive` del cambio
