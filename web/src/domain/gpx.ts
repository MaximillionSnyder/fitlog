import {
  GPX_SOURCE,
  HUAWEI_SOURCE,
  type ImportedWorkout,
} from '@/domain/importedWorkout';

/**
 * Lectura de archivos GPX.
 *
 * Un GPX es una ruta con puntos: no trae series con peso y reps, pero sí el inicio, el fin, la
 * distancia (sumando los tramos), la frecuencia cardíaca de cada punto y el desnivel. Se lee con un
 * recorrido propio de etiquetas, tolerante a espacios, atributos con comillas simples o dobles y
 * prefijos de namespace (`gpxtpx:hr`), porque los GPX los genera cada reloj a su manera.
 */

const HEAD_LENGTH = 2048;
const ELEVATION_NOISE_M = 2;
const EARTH_RADIUS_M = 6_371_000;

interface Point {
  readonly latitude: number | null;
  readonly longitude: number | null;
  readonly elevation: number | null;
  readonly timeMs: number | null;
  readonly heartRate: number | null;
}

interface Track {
  readonly points: readonly Point[];
  readonly sportName: string | null;
  readonly creator: string | null;
}

/** `true` si el texto parece un GPX y no un JSON de Huawei Health. */
export function looksLikeGpx(text: string): boolean {
  const head = text.slice(0, HEAD_LENGTH);
  return /<gpx/i.test(head) || head.trimStart().startsWith('<?xml');
}

/** Lee un archivo GPX; devuelve como mucho un entrenamiento. */
export function parseGpx(content: string, fileName?: string): ImportedWorkout[] {
  if (!looksLikeGpx(content)) return [];

  const track = readTrack(content);
  if (track === null || track.points.length === 0) return [];

  // Hacen falta al menos dos marcas de tiempo: sin fin no es un entrenamiento terminado y la sesión
  // quedaría como "en curso" en el historial.
  const times = track.points
    .map((point) => point.timeMs)
    .filter((time): time is number => time !== null)
    .sort((a, b) => a - b);
  if (times.length < 2) return [];

  const startedAt = times[0]!;
  const finishedAt = times[times.length - 1]!;
  const heartRates = track.points
    .map((point) => point.heartRate)
    .filter((rate): rate is number => rate !== null);

  const source =
    track.creator !== null && track.creator.toLowerCase().includes('huawei')
      ? HUAWEI_SOURCE
      : GPX_SOURCE;

  return [
    {
      recordId: null,
      startedAtMs: startedAt,
      finishedAtMs: finishedAt,
      sportType: null,
      sportName: track.sportName ?? (fileName ? nameWithoutExtension(fileName) : 'Entrenamiento'),
      durationMs: finishedAt - startedAt,
      distanceM: distanceOf(track.points) || null,
      calories: null,
      steps: null,
      averageHeartRate:
        heartRates.length === 0
          ? null
          : heartRates.reduce((sum, rate) => sum + rate, 0) / heartRates.length,
      maxHeartRate: heartRates.length === 0 ? null : Math.max(...heartRates),
      elevationGainM: elevationGainOf(track.points),
      source,
    },
  ];
}

/**
 * Recorre las etiquetas del GPX y arma el track.
 *
 * No es un parser XML completo: solo entiende lo que un GPX usa (etiquetas, atributos, texto y
 * comentarios), que es suficiente y evita depender de una librería.
 */
function readTrack(content: string): Track | null {
  const points: Point[] = [];
  let current: Point | null = null;
  let sportName: string | null = null;
  let creator: string | null = null;
  let textTarget: string | null = null;
  let buffer = '';
  let sawTrack = false;

  let index = 0;
  while (index < content.length) {
    const open = content.indexOf('<', index);
    if (open < 0) break;

    // Texto entre etiquetas: se guarda si estábamos leyendo un campo conocido.
    if (open > index && textTarget !== null) buffer += content.slice(index, open);

    if (content.startsWith('<!--', open)) {
      const end = content.indexOf('-->', open);
      index = end < 0 ? content.length : end + 3;
      continue;
    }
    if (content.startsWith('<![CDATA[', open)) {
      const end = content.indexOf(']]>', open);
      if (end > 0 && textTarget !== null) buffer += content.slice(open + 9, end);
      index = end < 0 ? content.length : end + 3;
      continue;
    }

    const close = content.indexOf('>', open);
    if (close < 0) break;
    const raw = content.slice(open + 1, close).trim();
    index = close + 1;
    if (raw === '') continue;

    const closing = raw.startsWith('/');
    const selfClosing = raw.endsWith('/');
    const body = raw.replace(/^\/+/, '').replace(/\/+$/, '').trim();
    const name = body.split(' ')[0]!.split('/')[0]!.toLowerCase();
    const localName = name.slice(name.lastIndexOf(':') + 1);
    const attributes = readAttributes(body);

    if (closing) {
      const text = buffer.trim();
      const point: Point | null = current;
      if (localName === 'trkpt' && point !== null) {
        points.push(point);
      } else if (localName === 'time' && textTarget === 'time' && text !== '' && point !== null) {
        const parsed = parseTime(text);
        if (parsed !== null) current = withPoint(point, { timeMs: parsed });
      } else if (localName === 'ele' && textTarget === 'ele' && text !== '' && point !== null) {
        const value = Number(text);
        if (Number.isFinite(value)) current = withPoint(point, { elevation: value });
      } else if (localName === 'hr' && textTarget === 'hr' && text !== '' && point !== null) {
        const value = Number(text);
        if (Number.isFinite(value)) current = withPoint(point, { heartRate: value });
      } else if (localName === 'name' && textTarget === 'name' && text !== '' && sportName === null) {
        sportName = text;
      } else if (localName === 'type' && textTarget === 'type' && text !== '') {
        sportName = sportLabel(text) ?? sportName;
      }
      if (textTarget === localName) textTarget = null;
      buffer = '';
      continue;
    }

    if (localName === 'gpx') creator = attributes.get('creator') ?? null;
    if (localName === 'trkpt') {
      sawTrack = true;
      current = {
        latitude: toNumber(attributes.get('lat')),
        longitude: toNumber(attributes.get('lon')),
        elevation: null,
        timeMs: null,
        heartRate: null,
      };
    }
    if (['time', 'ele', 'hr', 'name', 'type'].includes(localName)) {
      textTarget = localName;
      buffer = '';
    }
    if (selfClosing) {
      if (localName === 'trkpt') {
        points.push(current ?? { latitude: null, longitude: null, elevation: null, timeMs: null, heartRate: null });
        current = null;
      }
      if (textTarget === localName) textTarget = null;
    }
  }

  if (!sawTrack && points.length === 0) return null;
  return { points, sportName, creator };
}

/** Copia de un punto con los campos indicados cambiados. */
function withPoint(point: Point, changes: Partial<Point>): Point {
  return {
    latitude: changes.latitude ?? point.latitude,
    longitude: changes.longitude ?? point.longitude,
    elevation: changes.elevation ?? point.elevation,
    timeMs: changes.timeMs ?? point.timeMs,
    heartRate: changes.heartRate ?? point.heartRate,
  };
}

function readAttributes(body: string): Map<string, string> {
  const attributes = new Map<string, string>();
  let index = 0;
  while (index < body.length) {
    const equals = body.indexOf('=', index);
    if (equals < 0) break;
    const name = body.slice(index, equals).trim().split(' ').pop()!.toLowerCase();
    let cursor = equals + 1;
    while (cursor < body.length && /\s/.test(body[cursor] ?? '')) cursor += 1;
    if (cursor >= body.length) break;
    const quote = body[cursor];
    if (quote !== '"' && quote !== "'") {
      index = cursor;
      continue;
    }
    const end = body.indexOf(quote, cursor + 1);
    if (end < 0) break;
    if (name !== '') attributes.set(name, body.slice(cursor + 1, end));
    index = end + 1;
  }
  return attributes;
}

/** Fecha del GPX: ISO 8601 en UTC, con o sin milisegundos. */
function parseTime(text: string): number | null {
  const trimmed = text.trim();
  const iso = trimmed.includes('T') ? trimmed : trimmed.replace(' ', 'T');
  const hasZone = /([zZ]|[+-]\d{2}:?\d{2})$/.test(iso);
  const parsed = Date.parse(hasZone ? iso : `${iso}Z`);
  return Number.isNaN(parsed) ? null : parsed;
}

/** Tipo de deporte del GPX a partir de su etiqueta `type` o `name`. */
function sportLabel(raw: string): string | null {
  const value = raw.trim().toLowerCase();
  if (value === '') return null;
  if (value.includes('run')) return 'Running';
  if (value.includes('cycl') || value.includes('bike') || value.includes('bici')) return 'Bicicleta';
  if (value.includes('walk') || value.includes('camin') || value.includes('hik')) return 'Caminata';
  if (value.includes('swim') || value.includes('natac')) return 'Natación';
  if (value.includes('row') || value.includes('remo')) return 'Remo';
  return raw.trim();
}

/** Distancia acumulada entre puntos consecutivos con coordenadas. */
function distanceOf(points: readonly Point[]): number {
  let total = 0;
  let previous: Point | null = null;
  for (const point of points) {
    if (
      previous !== null &&
      point.latitude !== null &&
      point.longitude !== null &&
      previous.latitude !== null &&
      previous.longitude !== null
    ) {
      total += haversineM(previous.latitude, previous.longitude, point.latitude, point.longitude);
    }
    previous = point;
  }
  return total;
}

/** Desnivel positivo, ignorando el ruido del altímetro. */
function elevationGainOf(points: readonly Point[]): number | null {
  const elevations = points
    .map((point) => point.elevation)
    .filter((value): value is number => value !== null);
  if (elevations.length < 2) return null;

  let gain = 0;
  let reference = elevations[0]!;
  for (const elevation of elevations.slice(1)) {
    const difference = elevation - reference;
    if (difference > ELEVATION_NOISE_M) {
      gain += difference;
      reference = elevation;
    } else if (difference < -ELEVATION_NOISE_M) {
      reference = elevation;
    }
  }
  return gain > 0 ? gain : null;
}

/** Distancia entre dos coordenadas por la fórmula del semiverseno. */
function haversineM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRadians = (value: number) => (value * Math.PI) / 180;
  const deltaLat = toRadians(lat2 - lat1);
  const deltaLon = toRadians(lon2 - lon1);
  const a =
    Math.sin(deltaLat / 2) ** 2 +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(deltaLon / 2) ** 2;
  return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function toNumber(value: string | undefined): number | null {
  if (value === undefined) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function nameWithoutExtension(fileName: string): string {
  const base = fileName.slice(0, fileName.lastIndexOf('.') === -1 ? undefined : fileName.lastIndexOf('.'));
  const cleaned = base.replaceAll('_', ' ').replaceAll('-', ' ').trim();
  return cleaned === '' ? 'Entrenamiento' : cleaned[0]!.toUpperCase() + cleaned.slice(1);
}
