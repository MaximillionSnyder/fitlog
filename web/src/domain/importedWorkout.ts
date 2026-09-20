/**
 * Entrenamiento importado desde otra app, ya normalizado.
 *
 * Es el modelo común de todas las fuentes (la exportación de Huawei Health y los archivos GPX),
 * para que la pantalla de importación y la importación en sí no sepan de formatos.
 */
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
