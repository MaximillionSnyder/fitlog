import type { ProgressRange } from '@/domain/progress';

/**
 * Actividad importada de otra app: la serie de entrenamientos que traen distancia, duración y
 * frecuencia cardíaca (Huawei Health o GPX).
 *
 * Es distinta del progreso por ejercicio: una sesión importada no tiene series con peso y reps, así
 * que sus gráficos son de actividad (distancia, tiempo, pulso) y no de volumen.
 */

export const ACTIVITY_METRICS = ['distance', 'duration', 'heart_rate'] as const;

export type ActivityMetric = (typeof ACTIVITY_METRICS)[number];

export const ACTIVITY_METRIC_LABELS: Record<ActivityMetric, string> = {
  distance: 'Distancia',
  duration: 'Duración',
  heart_rate: 'FC media',
};

export interface ActivityInput {
  readonly startedAtMs: number;
  readonly finishedAtMs: number | null;
  readonly distanceM: number | null;
  readonly averageHeartRate: number | null;
}

export interface ActivityPoint {
  readonly startedAtMs: number;
  readonly distanceM: number;
  readonly durationMs: number;
  readonly averageHeartRate: number | null;
}

export interface ActivityTotals {
  readonly sessions: number;
  readonly distanceM: number;
  readonly durationMs: number;
}

/** Serie de actividad dentro del rango, de la más vieja a la más nueva. */
export function buildActivitySeries(
  inputs: readonly ActivityInput[],
  range: ProgressRange | null = null
): ActivityPoint[] {
  return inputs
    .filter((input) => inRange(input.startedAtMs, range))
    .slice()
    .sort((a, b) => a.startedAtMs - b.startedAtMs)
    .map((input) => ({
      startedAtMs: input.startedAtMs,
      distanceM: input.distanceM ?? 0,
      durationMs:
        input.finishedAtMs === null
          ? 0
          : Math.max(0, input.finishedAtMs - input.startedAtMs),
      averageHeartRate: input.averageHeartRate,
    }));
}

/** Valor de la métrica elegida para un punto, o `null` si el entrenamiento no la trae. */
export function activityValue(point: ActivityPoint, metric: ActivityMetric): number | null {
  if (metric === 'distance') return point.distanceM > 0 ? point.distanceM : null;
  if (metric === 'duration') return point.durationMs > 0 ? point.durationMs : null;
  return point.averageHeartRate;
}

/** Puntos con valor para la métrica elegida: los que no la traen no se grafican. */
export function activityValues(
  points: readonly ActivityPoint[],
  metric: ActivityMetric
): number[] {
  return points
    .map((point) => activityValue(point, metric))
    .filter((value): value is number => value !== null);
}

export function activityTotals(points: readonly ActivityPoint[]): ActivityTotals {
  return {
    sessions: points.length,
    distanceM: points.reduce((total, point) => total + point.distanceM, 0),
    durationMs: points.reduce((total, point) => total + point.durationMs, 0),
  };
}

function inRange(timestampMs: number, range: ProgressRange | null): boolean {
  if (range === null) return true;
  if (range.fromMs !== null && timestampMs < range.fromMs) return false;
  return timestampMs <= range.toMs;
}
