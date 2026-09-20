/**
 * Lectura de la exportacion de Huawei Health.
 *
 * Huawei exporta un JSON por entrenamiento grabado (carpeta `Motion path detail data`), con el tipo
 * de deporte, el inicio, el fin, la duracion, las calorias, la distancia y la frecuencia cardiaca.
 * No exporta series con peso y reps: esos entrenamientos entran como sesiones sin series.
 *
 * El lector es tolerante a proposito: la exportacion trae tambien archivos de sueno, pasos y
 * estres, y el formato varia entre versiones de la app. Todo lo que no se reconoce se ignora.
 */

/** Marca de origen en la nota de una sesión importada. */
export const HUAWEI_NOTE_PREFIX = 'Huawei Health';

/** `true` si la sesión vino de una importación (el origen queda en la nota). */
export function isHuaweiNote(notes: string | null | undefined): boolean {
  return typeof notes === 'string' && notes.startsWith(HUAWEI_NOTE_PREFIX);
}

export interface HuaweiWorkout {
  readonly recordId: string | null;
  readonly startedAtMs: number;
  readonly finishedAtMs: number | null;
  readonly sportType: number | null;
  readonly sportName: string;
  readonly durationMs: number | null;
  readonly distanceM: number | null;
  readonly calories: number | null;
  readonly steps: number | null;
  readonly averageHeartRate: number | null;
  readonly maxHeartRate: number | null;
}

export interface HuaweiParseResult {
  readonly workouts: readonly HuaweiWorkout[];
  readonly filesRead: number;
  readonly filesSkipped: number;
}

/** Nombres de los tipos de deporte que usa la exportacion. */
const SPORT_NAMES: Record<number, string> = {
  2: 'Senderismo',
  3: 'Bicicleta',
  4: 'Running',
  5: 'Caminata',
  102: 'Natación en pileta',
  103: 'Bicicleta fija',
  104: 'Natación en aguas abiertas',
  112: 'Remo',
  117: 'Entrenamiento',
  118: 'Running',
  147: 'Entrenamiento de fuerza',
  281: 'Caminata bajo techo',
  282: 'Senderismo',
};

/** Escala de la exportacion: las calorias vienen multiplicadas por mil. */
const CALORIE_SCALE = 1000;
/** Epoch en segundos del siglo XXI: por debajo se asume segundos, no milisegundos. */
const MAX_SECONDS_EPOCH = 4_102_444_800;
/** Segundos de un dia: separa una duracion en segundos de una en milisegundos. */
const SECONDS_PER_DAY = 86_400;

/** Lee varios archivos y devuelve los entrenamientos reconocidos, sin repetidos. */
export function parseHuaweiExport(contents: readonly string[]): HuaweiParseResult {
  const byKey = new Map<string, HuaweiWorkout>();
  let filesRead = 0;
  let filesSkipped = 0;

  for (const content of contents) {
    let workouts: HuaweiWorkout[];
    try {
      workouts = parseHuaweiFile(content);
    } catch {
      filesSkipped += 1;
      continue;
    }
    filesRead += 1;
    for (const workout of workouts) {
      // La exportacion triplica los registros: el primero que aparece gana.
      const key = workoutKey(workout);
      if (!byKey.has(key)) byKey.set(key, workout);
    }
  }

  return {
    workouts: [...byKey.values()].sort((a, b) => b.startedAtMs - a.startedAtMs),
    filesRead,
    filesSkipped,
  };
}

/** Clave natural para deduplicar dentro de la exportacion. */
export function workoutKey(workout: HuaweiWorkout): string {
  return workout.recordId ?? `${workout.startedAtMs}-${workout.sportName}`;
}

/**
 * Lee un archivo de la exportación.
 *
 * Primero intenta el camino normal (lista u objeto). Si no encuentra entrenamientos, recorre el
 * texto buscando objetos balanceados: así entran los archivos con varios objetos concatenados (la
 * exportación los parte con marcadores de resincronización) y los que traen una comilla suelta
 * dentro del blob de sensores, que rompe el JSON.
 */
export function parseHuaweiFile(content: string): HuaweiWorkout[] {
  const trimmed = content.trim();
  if (trimmed === '') return [];

  const workouts: HuaweiWorkout[] = [];
  try {
    collect(JSON.parse(trimmed), workouts);
  } catch {
    // Un archivo con varios objetos seguidos no es JSON válido entero: se recorre abajo.
  }
  if (workouts.length > 0) return workouts;

  for (const candidate of balancedObjects(repairAttributeQuotes(trimmed))) {
    try {
      collect(JSON.parse(candidate), workouts);
    } catch {
      // Un fragmento que no es JSON no aporta nada.
    }
  }
  return workouts;
}

/**
 * Objetos JSON balanceados del texto, respetando los literales de texto.
 *
 * Cubre archivos con varios objetos seguidos, con o sin saltos de línea, y listas: en todos los
 * casos los objetos se recortan por profundidad de llaves.
 */
export function balancedObjects(text: string): string[] {
  const objects: string[] = [];
  let depth = 0;
  let start = -1;
  let inString = false;
  let escaped = false;

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    if (inString) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === '"') inString = false;
      continue;
    }
    if (char === '"') {
      inString = true;
      continue;
    }
    if (char === '{') {
      if (depth === 0) start = index;
      depth += 1;
      continue;
    }
    if (char === '}') {
      depth -= 1;
      if (depth <= 0 && start >= 0) {
        objects.push(text.slice(start, index + 1));
        start = -1;
        depth = 0;
      }
    }
  }
  return objects;
}

/**
 * Quita las comillas sueltas dentro del campo `attribute`.
 *
 * La exportación guarda ahí la telemetría como texto y a veces aparece una comilla sin escapar, que
 * invalida el archivo entero. El valor real termina en la comilla seguida de coma o cierre.
 */
export function repairAttributeQuotes(text: string): string {
  if (!ATTRIBUTE_FIELD.test(text)) return text;
  ATTRIBUTE_FIELD.lastIndex = 0;

  let result = '';
  let index = 0;
  while (index < text.length) {
    ATTRIBUTE_FIELD.lastIndex = index;
    const match = ATTRIBUTE_FIELD.exec(text);
    if (match === null) break;

    const valueStart = match.index + match[0].length;
    result += text.slice(index, valueStart);

    let cursor = valueStart;
    let end = -1;
    while (cursor < text.length) {
      if (text[cursor] === '"') {
        let probe = cursor + 1;
        while (probe < text.length && /\s/.test(text[probe] ?? '')) probe += 1;
        const next = text[probe];
        if (next === undefined || next === ',' || next === '}' || next === ']') {
          end = cursor;
          break;
        }
      }
      cursor += 1;
    }
    if (end < 0) {
      index = valueStart;
      continue;
    }
    result += text.slice(valueStart, end).replaceAll('"', '');
    index = end;
  }
  result += text.slice(index);
  return result;
}

const ATTRIBUTE_FIELD = /"attribute"\s*:\s*"/gi;

function collect(node: unknown, out: HuaweiWorkout[]): void {
  if (Array.isArray(node)) {
    for (const item of node) collect(item, out);
    return;
  }
  if (node !== null && typeof node === 'object') {
    const record = node as Record<string, unknown>;
    const workout = toWorkout(record);
    if (workout !== null) out.push(workout);
    for (const value of Object.values(record)) {
      if (value !== null && typeof value === 'object') collect(value, out);
    }
  }
}

/**
 * Convierte un objeto en entrenamiento, o `null` si no lo parece.
 *
 * No alcanza con tener fecha: la exportacion tambien trae sueno, pasos y estres con sus propias
 * marcas de tiempo. Se pide ademas alguna senal de deporte (tipo, nombre, distancia, calorias o
 * frecuencia cardiaca), que es lo que distingue un entrenamiento del resto.
 */
function toWorkout(node: Record<string, unknown>): HuaweiWorkout | null {
  const startedAt = readTimestamp(node, ['startTime', 'start_time', 'beginTime', 'start']);
  if (startedAt === null || !looksLikeWorkout(node)) return null;

  const durationMs = readDuration(node);
  const explicitEnd = readTimestamp(node, ['endTime', 'end_time', 'finishTime', 'end']);
  // Un entrenamiento dura: los archivos por minuto traen fecha y calorías, pero no duración.
  if (durationMs === null && explicitEnd === null) return null;
  const finishedAt = explicitEnd ?? (durationMs === null ? null : startedAt + durationMs);

  const attribute = readString(node, ['attribute', 'attributes']);
  const summaryHeartRate = readNumber(node, ['avgHeartRate', 'averageHeartRate', 'avg_heart_rate']);
  const track = summaryHeartRate === null ? trackHeartRate(attribute) : null;

  const sportType = readNumber(node, ['sportType', 'sport_type', 'activityType', 'exerciseType']);
  const explicitName = readString(node, ['sportName', 'activityName', 'name']);
  const sportName =
    explicitName ?? (sportType === null ? null : SPORT_NAMES[sportType]) ?? 'Entrenamiento';
  const calories = readNumber(node, ['totalCalories', 'calories', 'total_calories']);

  return {
    recordId: readString(node, ['recordId', 'record_id', 'id']),
    startedAtMs: startedAt,
    finishedAtMs: finishedAt,
    sportType: sportType === null ? null : Math.round(sportType),
    sportName,
    durationMs: durationMs ?? (finishedAt === null ? null : Math.max(0, finishedAt - startedAt)),
    distanceM: readNumber(node, ['totalDistance', 'distance', 'total_distance']),
    calories: calories === null ? null : calories / CALORIE_SCALE,
    steps: readNumber(node, ['totalSteps', 'steps']),
    averageHeartRate: summaryHeartRate ?? track?.average ?? null,
    maxHeartRate: readNumber(node, ['maxHeartRate', 'max_heart_rate']) ?? track?.max ?? null,
  };
}

/**
 * Frecuencia cardiaca derivada del blob de sensores (`attribute`).
 *
 * La exportacion documenta `avgHeartRate` y `maxHeartRate`, pero en la practica vienen vacios: el
 * dato real esta en los segmentos `tp=h-r;k=<minuto>;v=<pulsaciones>;` del blob.
 */
export function trackHeartRate(
  attribute: string | null
): { average: number; max: number } | null {
  if (attribute === null || attribute.trim() === '') return null;

  const readings: number[] = [];
  const lower = attribute.toLowerCase();
  const tag = 'tp=h-r;';
  let index = 0;

  while (index < attribute.length) {
    const segmentStart = lower.indexOf(tag, index);
    if (segmentStart < 0) break;
    const contentStart = segmentStart + tag.length;
    const nextSegment = lower.indexOf('tp=', contentStart);
    const end = nextSegment < 0 ? attribute.length : nextSegment;

    for (const match of attribute.slice(contentStart, end).matchAll(HEART_RATE_PAIR)) {
      const value = Number(match[2]);
      if (Number.isFinite(value)) readings.push(value);
    }
    index = end;
  }

  if (readings.length === 0) return null;
  const total = readings.reduce((sum, value) => sum + value, 0);
  return { average: total / readings.length, max: Math.max(...readings) };
}

/** Lectura de frecuencia cardiaca dentro de un segmento `h-r`: `k=<minuto>;v=<pulsaciones>;`. */
const HEART_RATE_PAIR = /k=([-\d.]+);v=([-\d.]+);/gi;

/** Senales de que el objeto es un entrenamiento y no otra serie de datos de la exportacion. */
function looksLikeWorkout(node: Record<string, unknown>): boolean {
  if (readNumber(node, ['sportType', 'sport_type', 'activityType', 'exerciseType']) !== null) {
    return true;
  }
  if (readString(node, ['sportName', 'activityName']) !== null) return true;
  if (readNumber(node, ['totalDistance', 'distance', 'total_distance']) !== null) return true;
  if (readNumber(node, ['totalCalories', 'calories', 'total_calories']) !== null) return true;
  return (
    readNumber(node, ['avgHeartRate', 'averageHeartRate', 'maxHeartRate', 'max_heart_rate']) !== null
  );
}

/**
 * Lee una fecha: epoch en milisegundos o segundos, o texto ISO / `yyyy-MM-dd HH:mm:ss`.
 *
 * La exportacion usa milisegundos, pero algunas versiones exportan segundos; se distingue por
 * magnitud.
 */
function readTimestamp(node: Record<string, unknown>, keys: readonly string[]): number | null {
  for (const key of keys) {
    if (!(key in node)) continue;
    const raw = node[key];
    if (typeof raw === 'number' && Number.isFinite(raw)) return normalizeEpoch(raw);
    if (typeof raw === 'string') {
      const text = raw.trim();
      if (text === '') continue;
      if (/^\d+$/.test(text)) return normalizeEpoch(Number(text));
      // Sin zona explicita la fecha se interpreta como UTC, igual que en Android: si no, el
      // resultado depende de la zona del dispositivo.
      const iso = text.includes('T') ? text : text.replace(' ', 'T');
      const hasZone = /([zZ]|[+-]\d{2}:?\d{2})$/.test(iso);
      const parsed = Date.parse(hasZone ? iso : `${iso}Z`);
      if (!Number.isNaN(parsed)) return parsed;
    }
  }
  return null;
}

function normalizeEpoch(value: number): number {
  const rounded = Math.trunc(value);
  return rounded >= 1 && rounded <= MAX_SECONDS_EPOCH ? rounded * 1000 : rounded;
}

/**
 * Duracion en milisegundos.
 *
 * `totalTime` ya viene en milisegundos (la exportacion divide por mil para obtener segundos); las
 * claves alternativas suelen venir en segundos, asi que se distinguen por magnitud.
 */
function readDuration(node: Record<string, unknown>): number | null {
  const total = readNumber(node, ['totalTime', 'total_time']);
  if (total !== null) return Math.trunc(total);

  const explicitMs = readNumber(node, ['durationMs', 'duration_ms']);
  if (explicitMs !== null) return Math.trunc(explicitMs);

  const seconds = readNumber(node, ['duration', 'exerciseTime']);
  if (seconds !== null) {
    return seconds < SECONDS_PER_DAY ? Math.trunc(seconds * 1000) : Math.trunc(seconds);
  }
  return null;
}

function readString(node: Record<string, unknown>, keys: readonly string[]): string | null {
  for (const key of keys) {
    const value = node[key];
    if (typeof value === 'string' && value.trim() !== '') return value.trim();
    if (typeof value === 'number' && Number.isFinite(value)) return String(value);
  }
  return null;
}

function readNumber(node: Record<string, unknown>, keys: readonly string[]): number | null {
  for (const key of keys) {
    const value = node[key];
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string') {
      const parsed = Number(value.trim().replace(',', '.'));
      if (Number.isFinite(parsed)) return parsed;
    }
  }
  return null;
}

/**
 * Nota de la sesion importada: origen y los datos que Huawei si registro.
 *
 * Es lo unico que queda del entrenamiento ademas de las fechas, porque la exportacion no trae
 * series con peso y reps.
 */
export function huaweiNote(workout: HuaweiWorkout): string {
  const parts = [HUAWEI_NOTE_PREFIX, workout.sportName];

  if (workout.distanceM !== null && workout.distanceM > 0) {
    const km = workout.distanceM / 1000;
    parts.push(km >= 1 ? `${formatDistance(km)} km` : `${Math.round(workout.distanceM)} m`);
  }
  if (workout.calories !== null && workout.calories > 0) {
    parts.push(`${Math.round(workout.calories)} kcal`);
  }
  const average = workout.averageHeartRate;
  const max = workout.maxHeartRate;
  if (average !== null && max !== null) {
    parts.push(`FC ${Math.round(average)}/${Math.round(max)}`);
  } else if (average !== null) {
    parts.push(`FC media ${Math.round(average)}`);
  }
  if (workout.steps !== null && workout.steps > 0) parts.push(`${workout.steps} pasos`);

  return parts.join(' · ');
}

/** Hasta dos decimales, sin ceros de relleno: `5.24`, `20`. */
function formatDistance(value: number): string {
  return String(Math.round(value * 100) / 100);
}
