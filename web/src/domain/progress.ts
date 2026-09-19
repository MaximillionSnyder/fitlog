import { roundToTenth } from '@/domain/formulas';

export const DAY_MS = 86_400_000;

export type ProgressMetric = 'maxWeightKg' | 'volumeKg' | 'bestOneRepMaxKg';

export type RangePreset = '30d' | '90d' | 'all';

export interface ProgressRange {
  readonly fromMs: number | null;
  readonly toMs: number;
}

export interface ProgressSetInput {
  readonly sessionId: string;
  readonly startedAtMs: number;
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly isWarmup: boolean;
}

export interface ProgressPoint {
  readonly sessionId: string;
  readonly startedAtMs: number;
  readonly maxWeightKg: number;
  readonly volumeKg: number;
  readonly bestOneRepMaxKg: number;
  readonly workingSets: number;
}

export function rangeFor(preset: RangePreset, nowMs: number): ProgressRange | null {
  if (preset === 'all') {
    return null;
  }
  const days = preset === '30d' ? 30 : 90;
  return { fromMs: nowMs - days * DAY_MS, toMs: nowMs };
}

export function buildProgressSeries(
  sets: readonly ProgressSetInput[],
  range: ProgressRange | null = null
): ProgressPoint[] {
  const buckets = new Map<string, ProgressPoint>();

  for (const set of sets) {
    if (set.isWarmup) {
      continue;
    }
    if (range !== null) {
      if (range.fromMs !== null && set.startedAtMs < range.fromMs) {
        continue;
      }
      if (set.startedAtMs > range.toMs) {
        continue;
      }
    }

    const existing = buckets.get(set.sessionId);
    const workingSets = (existing?.workingSets ?? 0) + 1;
    const hasValues = set.weightKg !== null && set.reps !== null;

    const maxWeightKg = hasValues
      ? Math.max(existing?.maxWeightKg ?? 0, set.weightKg as number)
      : existing?.maxWeightKg ?? 0;

    const volumeKg = hasValues
      ? (existing?.volumeKg ?? 0) + (set.weightKg as number) * (set.reps as number)
      : existing?.volumeKg ?? 0;

    const bestOneRepMaxKg = hasValues
      ? Math.max(
          existing?.bestOneRepMaxKg ?? 0,
          roundToTenth((set.weightKg as number) * (1 + (set.reps as number) / 30))
        )
      : existing?.bestOneRepMaxKg ?? 0;

    buckets.set(set.sessionId, {
      sessionId: set.sessionId,
      startedAtMs: existing?.startedAtMs ?? set.startedAtMs,
      maxWeightKg,
      volumeKg,
      bestOneRepMaxKg,
      workingSets,
    });
  }

  return [...buckets.values()].sort((a, b) => {
    if (a.startedAtMs !== b.startedAtMs) {
      return a.startedAtMs - b.startedAtMs;
    }
    return a.sessionId < b.sessionId ? -1 : a.sessionId > b.sessionId ? 1 : 0;
  });
}

export function metricValue(point: ProgressPoint, metric: ProgressMetric): number {
  return point[metric];
}

export function formatMetric(value: number, metric: ProgressMetric): string {
  if (metric === 'volumeKg') {
    return `${Math.round(value).toLocaleString('es')} kg`;
  }
  return `${Math.round(value * 10) / 10} kg`;
}

export function formatDay(timestampMs: number): string {
  return new Date(timestampMs).toLocaleDateString('es', { day: '2-digit', month: '2-digit' });
}
