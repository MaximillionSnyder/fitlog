/**
 * Entrenamiento importado desde otra app, ya normalizado.
 *
 * Es el modelo común de todas las fuentes (la exportación de Huawei Health y los archivos GPX),
 * para que la pantalla de importación y la importación en sí no sepan de formatos.
 */
import type { RoutePoint } from '@/domain/route';

export interface ImportedWorkout {
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
  readonly elevationGainM: number | null;
  /** Puntos del recorrido, cuando la fuente los trae (GPX). */
  readonly route?: readonly RoutePoint[];
  readonly source: string;
}

/** Marcas de origen de una sesión importada. */
export const HUAWEI_SOURCE = 'Huawei Health';
export const GPX_SOURCE = 'GPX';

const SOURCES = [HUAWEI_SOURCE, GPX_SOURCE];

/** Clave natural para deduplicar dentro de una importación. */
export function workoutKey(workout: ImportedWorkout): string {
  return workout.recordId ?? `${workout.startedAtMs}-${workout.sportName}`;
}

/** `true` si la sesión vino de una importación (el origen queda en la nota). */
export function isImportedNote(notes: string | null | undefined): boolean {
  return typeof notes === 'string' && SOURCES.some((source) => notes.startsWith(source));
}

/** Métricas recuperadas de la nota de una sesión importada. */
export interface ParsedActivity {
  readonly source: string;
  readonly distanceM: number | null;
  readonly calories: number | null;
  readonly averageHeartRate: number | null;
  readonly maxHeartRate: number | null;
  readonly steps: number | null;
  readonly elevationGainM: number | null;
}

/**
 * Lee las métricas de la nota de una sesión importada.
 *
 * Las sesiones importadas antes de que existieran las columnas propias solo tienen la nota; este
 * lector recupera sus datos para que se sigan viendo en el detalle y en los gráficos.
 */
export function parseImportedNote(notes: string | null | undefined): ParsedActivity | null {
  if (typeof notes !== 'string') return null;
  const source = SOURCES.find((candidate) => notes.startsWith(candidate));
  if (source === undefined) return null;

  const parts = notes
    .split(' · ')
    .map((part) => part.trim())
    .filter((part) => part !== '');

  let distanceM: number | null = null;
  let calories: number | null = null;
  let averageHeartRate: number | null = null;
  let maxHeartRate: number | null = null;
  let steps: number | null = null;
  let elevationGainM: number | null = null;

  for (const part of parts.slice(1)) {
    const number = (value: string) => {
      const parsed = Number(value.replace(',', '.'));
      return Number.isFinite(parsed) ? parsed : null;
    };

    if (part.endsWith(' km')) {
      const km = number(part.slice(0, -3));
      if (km !== null) distanceM = km * 1000;
    } else if (part.endsWith(' m') && !part.startsWith('desnivel')) {
      distanceM = number(part.slice(0, -2));
    } else if (part.endsWith(' kcal')) {
      calories = number(part.slice(0, -5));
    } else if (part.endsWith(' pasos')) {
      const value = Number(part.slice(0, -6).trim());
      if (Number.isFinite(value)) steps = Math.round(value);
    } else if (part.startsWith('desnivel ')) {
      elevationGainM = number(part.slice('desnivel '.length).replace(/ m$/, '').trim());
    } else if (part.startsWith('FC ')) {
      const value = part.slice(3).replace(/^media /, '').trim();
      const [average, max] = value.split('/');
      averageHeartRate = average === undefined ? null : number(average);
      maxHeartRate = max === undefined ? null : number(max);
    }
  }

  return { source, distanceM, calories, averageHeartRate, maxHeartRate, steps, elevationGainM };
}

/**
 * Parte de la nota sin el origen: los datos que registró la app de origen.
 *
 * La usa el historial para mostrar lo que sí tiene una sesión importada (deporte, distancia,
 * frecuencia cardíaca) en lugar de "0 series · 0 kg".
 */
export function importedDataSummary(notes: string | null | undefined): string | null {
  if (typeof notes !== 'string') return null;
  const source = SOURCES.find((candidate) => notes.startsWith(candidate));
  if (source === undefined) return null;
  const rest = notes.slice(source.length).replace(/^ · /, '').trim();
  return rest === '' ? null : rest;
}

/**
 * Nota de la sesión importada: origen y los datos que la fuente sí registró.
 *
 * Es lo único que queda del entrenamiento además de las fechas, porque ninguna de las fuentes
 * exporta series con peso y reps.
 */
export function importedWorkoutNote(workout: ImportedWorkout): string {
  const parts = [workout.source, workout.sportName];

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
  if (workout.elevationGainM !== null && workout.elevationGainM >= 5) {
    parts.push(`desnivel ${Math.round(workout.elevationGainM)} m`);
  }
  if (workout.steps !== null && workout.steps > 0) parts.push(`${workout.steps} pasos`);

  return parts.join(' · ');
}

/** Hasta dos decimales, sin ceros de relleno: `5.24`, `20`. */
function formatDistance(value: number): string {
  return String(Math.round(value * 100) / 100);
}
