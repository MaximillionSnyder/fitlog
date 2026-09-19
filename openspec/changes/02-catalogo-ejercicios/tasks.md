## 1. Catálogo base y vectores compartidos

- [ ] 1.1 Crear `shared/seed/generate-catalog.mjs` que genere `shared/seed/catalog.json` con IDs ULID deterministas
- [ ] 1.2 Generar `shared/seed/catalog.json` con al menos 10 grupos musculares y 25 ejercicios de gimnasio en español
- [ ] 1.3 Crear `shared/test-vectors/catalog-filters.json` con casos de búsqueda normalizada y filtros combinados (incluye casos sin resultados)
- [ ] 1.4 Test en Web (Vitest) y Android (JUnit) que valida integridad del seed: ULIDs válidos, slugs únicos, referencias a grupos existentes
- [ ] 1.5 Exponer `shared/seed/` como assets de Android y como import de Web

## 2. Capa de datos y dominio (compartida por diseño)

- [ ] 2.1 Definir el modelo de catálogo (grupo muscular, ejercicio, filtros) en Android y Web
- [ ] 2.2 Implementar normalización de texto y slug (idéntica en ambas plataformas) con tests contra vectores
- [ ] 2.3 Implementar el filtro de catálogo (texto, grupo principal/secundario, equipamiento, tipo) con tests contra vectores
- [ ] 2.4 Implementar `CatalogSeeder` idempotente en Web (worker) y Android (repositorio con Mutex)
- [ ] 2.5 Implementar DAOs/consultas: listar activos ordenados, contar, insertar, borrado lógico
- [ ] 2.6 Implementar creación de ejercicio personalizado con validaciones (nombre, grupo existente, slug único)
- [ ] 2.7 Implementar protección del catálogo base (no borrar ni editar `is_custom = 0`)

## 3. Tests de datos

- [ ] 3.1 Test Android (Robolectric + Room en memoria): siembra completa, idempotencia, personalizados preservados
- [ ] 3.2 Test Android: alta de personalizado, slug duplicado, grupo inexistente, borrado protegido
- [ ] 3.3 Test Web (node:sqlite): siembra completa, idempotencia, alta, duplicado, borrado protegido
- [ ] 3.4 Verificar que las consultas excluyen filas con `deleted_at` no nulo

## 4. UI Android

- [ ] 4.1 Añadir `androidx.navigation-compose` y navegación `home` → `catalog`
- [ ] 4.2 Pantalla de catálogo con buscador, chips de filtros (grupo, equipamiento, tipo) y lista de ejercicios
- [ ] 4.3 Etiqueta visual "Propio" en ejercicios personalizados y acción de eliminar con confirmación
- [ ] 4.4 Formulario de alta de ejercicio propio con validaciones y mensajes de error
- [ ] 4.5 Estado vacío explícito cuando no hay resultados

## 5. UI Web

- [ ] 5.1 Vista de catálogo con buscador, filtros y lista, reutilizando el dominio compartido
- [ ] 5.2 Etiqueta visual "Propio" y eliminación con confirmación
- [ ] 5.3 Formulario de alta de ejercicio propio con validaciones y mensajes de error
- [ ] 5.4 Estado vacío explícito cuando no hay resultados

## 6. Cierre

- [ ] 6.1 CI verde en ambos workflows (Android y Web) con los nuevos tests
- [ ] 6.2 Verificar en el APK de CI y en Pages que el catálogo se siembra y filtra igual
- [ ] 6.3 `openspec validate 02-catalogo-ejercicios` sin errores y `openspec archive` del cambio
