## 1. Vectores y dominio

- [ ] 1.1 Crear `shared/test-vectors/session-summary.json` con casos de volumen total, series efectivas, volumen por ejercicio y series incompletas
- [ ] 1.2 Implementar el resumen de sesión en el dominio de Web (TypeScript) con test contra los vectores
- [ ] 1.3 Implementar el resumen de sesión en el dominio de Android (Kotlin) con test contra los vectores

## 2. Datos y lógica de entrenamiento (Web)

- [ ] 2.1 Definir el repositorio de entrenamiento sobre Drizzle: sesión activa, iniciar, finalizar
- [ ] 2.2 Implementar registro de series con índice automático y validaciones (peso, reps, ejercicio activo, sesión activa)
- [ ] 2.3 Implementar edición y borrado lógico de series
- [ ] 2.4 Implementar consultas de historial (sesiones + resumen) y detalle por sesión
- [ ] 2.5 Tests con `node:sqlite`: inicio/fin, índice automático, validaciones, edición, borrado, historial y detalle

## 3. UI Web

- [ ] 3.1 Sección "Entrenar" con sesión activa: selector de ejercicio del catálogo, campos de peso/reps/RIR/notas y marca de calentamiento
- [ ] 3.2 Lista de series registradas con edición y eliminación, y botón de finalizar sesión
- [ ] 3.3 Historial de sesiones con resumen y detalle por ejercicio
- [ ] 3.4 Estados vacíos (sin sesión activa, sesión sin series) y mensajes de error de validación

## 4. Datos y UI Android

- [ ] 4.1 `WorkoutDao` con sesiones y series (insertar, actualizar, borrado lógico, consultas de activa e historial)
- [ ] 4.2 `WorkoutRepository` con las mismas reglas que Web (índice automático, validaciones, resumen)
- [ ] 4.3 Tests Robolectric: sesión única activa, índice automático, validaciones, edición, borrado e historial
- [ ] 4.4 Pantalla "Entrenar" con registro de series y finalización, y pantalla de historial con detalle
- [ ] 4.5 Navegación desde la pantalla inicial a Entrenar e Historial

## 5. Cierre

- [ ] 5.1 CI verde en Android y Web con los tests nuevos
- [ ] 5.2 Publicar release `v0.1.1` (versionCode 2) con el registro de series
- [ ] 5.3 `openspec validate 04-registro-series` y `openspec archive` del cambio
