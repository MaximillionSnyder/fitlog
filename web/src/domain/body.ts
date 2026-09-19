import { roundToTenth } from '@/domain/formulas';
import type { ProgressRange } from '@/domain/progress';

export const METRIC_KINDS = [
  'body_weight',
  'body_fat',
  'waist',
  'chest',
  'arm',
  'thigh',
  'hip',
  'neck',
  'other',
] as const;

export type MetricKind = (typeof METRIC_KINDS)[number];

export type MetricUnit = 'kg' | 'cm' | '%';

export interface BodyMetricPoint {
  readonly id: string;
  readonly kind: MetricKind;
  readonly measuredAtMs: number;
  readonly value: number;
  readonly unit: MetricUnit;
  readonly notes: string | null;
}

export interface MetricStats {
  readonly count: number;
  readonly first: number | null;
  readonly latest: number | null;
  readonly min: number | null;
  readonly max: number | null;
  readonly deltaAbs: number | null;
  readonly deltaPct: number | null;
}

export function isMetricKind(value: string): value is MetricKind {
  return (METRIC_KINDS as readonly string[]).includes(value);
}

export function unitForKind(kind: MetricKind): MetricUnit {
  if (kind === 'body_weight') return 'kg';
  if (kind === 'body_fat') return '%';
  return 'cm';
}

export function validateMetric(kind: string, value: number): string | null {
  if (!isMetricKind(kind)) {
    return `Tipo de medida desconocido: ${kind}`;
  }
  if (!Number.isFinite(value) || value <= 0) {
    return 'El valor debe ser un número mayor que 0';
  }
  if (kind === 'body_fat' && value > 100) {
    return 'El porcentaje de grasa debe estar entre 0 y 100';
  }
  return null;
}

export function buildMetricSeries(
  metrics: readonly BodyMetricPoint[],
  kind: MetricKind,
  range: ProgressRange | null = null
): BodyMetricPoint[] {
  return metrics
    .filter((metric) => {
      if (metric.kind !== kind) return false;
      if (range === null) return true;
      if (range.fromMs !== null && metric.measuredAtMs < range.fromMs) return false;
      return metric.measuredAtMs <= range.toMs;
    })
    .slice()
    .sort((a, b) => {
      if (a.measuredAtMs !== b.measuredAtMs) return a.measuredAtMs - b.measuredAtMs;
      return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
    });
}

export function buildMetricStats(points: readonly BodyMetricPoint[]): MetricStats {
  if (points.length === 0) {
    return {
      count: 0,
      first: null,
      latest: null,
      min: null,
      max: null,
      deltaAbs: null,
      deltaPct: null,
    };
  }

  const values = points.map((point) => point.value);
  const first = values[0] as number;
  const latest = values[values.length - 1] as number;
  const deltaAbs = roundToTenth(latest - first);

  return {
    count: points.length,
    first,
    latest,
    min: Math.min(...values),
    max: Math.max(...values),
    deltaAbs,
    deltaPct: first === 0 ? null : roundToTenth((deltaAbs / first) * 100),
  };
}

export function formatMetricValue(value: number, unit: MetricUnit): string {
  const rounded = roundToTenth(value);
  const text = Number.isInteger(rounded) ? String(rounded) : String(rounded);
  return `${text} ${unit}`;
}

export function metricKindLabel(kind: MetricKind): string {
  const labels: Record<MetricKind, string> = {
    body_weight: 'Peso corporal',
    body_fat: 'Grasa corporal',
    waist: 'Cintura',
    chest: 'Pecho',
    arm: 'Brazo',
    thigh: 'Muslo',
    hip: 'Cadera',
    neck: 'Cuello',
    other: 'Otra',
  };
  return labels[kind];
}
