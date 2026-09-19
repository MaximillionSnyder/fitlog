# Sistema de diseño de FitLog

Este documento es la fuente de verdad de la presentación: qué tokens existen, qué significan y cómo
se usan en Android (Compose) y en la web (Tailwind). Si un color, un radio o un espaciado no está
acá, no debería aparecer en una pantalla.

## Principios

1. **Un solo lugar para el color.** Ninguna pantalla define colores literales: Android los toma de
   `LocalFitLogColors` y del `ColorScheme`; la web, de los tokens de `web/src/index.css`.
2. **El shell provee el encabezado.** Las pantallas no dibujan su propio título: lo pone el shell
   (barra inferior + `TopAppBar` en Android, rail/tab bar + header en la web).
3. **Cifras tabulares para los datos.** Todo valor numérico (peso, reps, volumen, duración, rango)
   usa cifras tabulares para que las columnas se alineen.
4. **Los estados vacíos ofrecen salida.** Un estado vacío sin acción es un callejón: si hay un paso
   siguiente, se muestra.
5. **Movimiento con propósito, nunca obligatorio.** Todo se entiende con las animaciones apagadas
   (Android respeta `MotionDurationScale`; la web respeta `prefers-reduced-motion`).

## Tokens de color

| Token | Rol | Oscuro | Claro |
|---|---|---|---|
| `bg` | Fondo de la app | `#070B15` | `#F6F8FC` |
| `surface` | Tarjetas y superficies elevadas | `#0E1526` | `#FFFFFF` |
| `surface-low` | Campos y filas dentro de tarjetas | `#0B111F` | `#F1F4FA` |
| `surface-high` | Contenedores de icono, barras | `#16203A` | `#E9EEF8` |
| `line` / `line-strong` | Bordes suaves / marcados | `#1F2A42` / `#2A3655` | `#DCE3EF` / `#C6CFDF` |
| `ink` | Texto principal | `#E7ECF5` | `#101828` |
| `muted` / `faint` | Texto secundario / terciario | `#9BA8C0` / `#6B7893` | `#4A5568` / `#6B7280` |
| `accent` | Acción y foco (lima) | `#BEF264` | `#4D7C0F` |
| `accent-soft` | Relleno de contenedores de acento | `#243318` | `#EAF7C9` |
| `accent-ink` | Texto sobre el acento | `#14200A` | `#F8FEE8` |
| `accent-text` | Acento legible sobre superficie | `#BEF264` | `#4D7C0F` |
| `data` / `data-soft` | Series y métricas (cian) | `#67E8F9` / `#10303A` | `#0E7490` / `#DCF3F9` |
| `success` / `success-soft` | Confirmaciones | `#6EE7B7` / `#0F2E24` | `#047857` / `#DCF5EA` |
| `warning` / `warning-soft` | Avisos | `#FCD34D` / `#33280E` | `#92400E` / `#FDF0D5` |
| `danger` / `danger-soft` | Errores y borrados | `#FDA4AF` / `#3A1520` | `#BE123C` / `#FCE4E9` |

Notas de contraste: en tema claro el acento se usa como **relleno** (con `accent-ink` encima) y para
texto se usa `accent-text`; el aviso en claro es `#92400E` porque el ámbar original no llegaba a
4.5:1 sobre `warning-soft`. Todas las combinaciones de texto sobre superficie superan 4.5:1.

### Dónde vive cada token

- Android: `android/app/src/main/java/com/fitlog/app/ui/theme/Color.kt`
  (`FitLogDarkColors`/`FitLogLightColors` para Material y `FitLogDarkPalette`/`FitLogLightPalette`
  para los tokens de marca y semánticos, accesibles con `MaterialTheme.fitLogColors`).
- Web: `web/src/index.css` (variables `--fl-*` expuestas a Tailwind con `@theme inline`, así las
  utilidades `bg-surface`, `text-accent`, `border-line`, etc. siguen al tema sin variantes `dark:`).

## Tema

- Modos: `sistema` (por defecto), `claro`, `oscuro`.
- Android: `FitLogTheme(mode, dynamicColor)` en `ui/theme/Theme.kt`; la preferencia se guarda con
  `AppSettings` (`SharedPreferences`) y se elige en Ajustes. En Android 12+ se pueden usar los
  colores dinámicos del sistema (solo cambian los roles de Material; los semánticos se mantienen).
- Web: `useTheme()` en `web/src/ui/theme.ts` + la clase `.light`/`.dark` en `<html>`. El script
  inline de `index.html` aplica el tema antes de montar React para evitar el destello.

## Tipografía

Escala de Material 3 en Android (`ui/theme/Type.kt`) con tracking negativo en títulos y
`labelSmall` en versalitas espaciadas para los encabezados de sección. Las cifras tabulares se
aplican con `NumericStyle`/`TabularNumbers` (Android) y con la clase `fl-num` (web).

## Formas y espaciado

| Uso | Android | Web |
|---|---|---|
| Tarjeta | `shapes.large` (24dp) | `rounded-card` |
| Tile / fila | `shapes.medium` (18dp) | `rounded-tile` |
| Campo o botón | `shapes.medium`/`large` | `rounded-field` |
| Chip | píldora | `rounded-full` |
| Escala de espaciado | `Spacing` (4/8/12/16/24/32) | `gap-*`/`p-*` de Tailwind (4px base) |

## Componentes

| Componente | Android | Web |
|---|---|---|
| Tarjeta | `FitLogCard` | `Card` |
| Tile de estadística | `StatTile` | `StatTile` |
| Encabezado de sección | `SectionHeader` | `SectionHeader` |
| Acción primaria | `PrimaryAction` / `SecondaryAction` | `Button` (`primary`/`secondary`/`ghost`/`danger`) |
| Fila de navegación | `NavigationRow` | `NavigationRow` |
| Filtro | `FilterChip` de Material | `Chip` |
| Estado vacío / carga / error | `EmptyState` / `LoadingState` / `ErrorState` | `EmptyState` / `LoadingState` / `ErrorState` |
| Par etiqueta-valor | `LabeledValue` | `LabeledValue` |
| Iconos | `FitLogIcons` (`ui/components/BrandIcon.kt`) | `@/ui/icons` |

Los iconos son propios en ambas plataformas (lienzo 24×24, trazo 1.8, uniones redondeadas) para no
sumar `material-icons-extended` ni una librería de iconos en la web.

## Navegación

Cinco destinos de primer nivel (Inicio, Entrenar, Progreso, Rutinas, Más) y el resto agrupado en
Más (Catálogo, Medidas, Comparativas, Tips, Respaldo, Ajustes). Android usa `NavigationBar` con
`saveState`/`restoreState`; la web usa rail lateral en escritorio (`md:`) y barra inferior en móvil.
Cuando hay una sesión en curso, la pestaña Entrenar muestra un punto y el encabezado ofrece
"Continuar" desde cualquier pantalla de primer nivel.

## Cómo agregar una pantalla

1. Usá el shell: no dibujes encabezado propio (Android: el `TopAppBar` del shell; web: el header del
   `AppShell`).
2. Construí el contenido con las primitivas de la tabla de componentes.
3. Tomá los colores de los tokens; si falta un rol, agregalo primero acá y en los dos archivos de
   tokens.
4. Cubrí los estados de carga, error y vacío.
5. Verificá en tema claro y oscuro, y con las animaciones del sistema apagadas.
