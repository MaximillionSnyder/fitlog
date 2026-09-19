import { describe, expect, it } from 'vitest';

import {
  buildMetricSeries,
  buildMetricStats,
  formatMetricValue,
  isMetricKind,
  unitForKind,
  validateMetric,
  type BodyMetricPoint,
  type MetricKind,
} from '@/domain/body';

import vectors from '@shared/test-vectors/body-metrics.json';

interface VectorMetric {
  id: string;
  kind: string;
  measured_at_ms: number;
  value: number;
}

function toPoints(metrics: VectorMetric[]): BodyMetricPoint[] {
  return metrics.map((metric) => ({
    id: metric.id,
    kind: metric.kind as MetricKind,
    measuredAtMs: metric.measured_at_ms,
    value: metric.value,
    unit: unitForKind(metric.kind as MetricKind),
    notes: null,
  }));
}

describe('unitForKind', () => {
  it.each(vectors.unit_cases)('$kind -> $expected', (testCase) => {
    expect(unitForKind(testCase.kind as MetricKind)).toBe(testCase.expected);
  });
});

describe('validateMetric', () => {
  it.each(vectors.validation_cases)('$name', (testCase) => {
    const error = validateMetric(testCase.kind, testCase.value);
    if (testCase.valid) {
      expect(error).toBeNull();
    } else {
      expect(error).not.toBeNull();
    }
  });
});

describe('buildMetricSeries', () => {
  it.each(vectors.series_cases)('$name', (testCase) => {
    const points = buildMetricSeries(
      toPoints(testCase.metrics as VectorMetric[]),
      testCase.kind as MetricKind,
      'from_ms' in testCase && testCase.from_ms !== undefined
        ? { fromMs: testCase.from_ms, toMs: testCase.to_ms ?? Number.MAX_SAFE_INTEGER }
        : null
    );

    expect(points.map((point) => point.id)).toEqual(testCase.expected);
  });
});

describe('buildMetricStats', () => {
  it.each(vectors.stats_cases)('$name', (testCase) => {
    const points = testCase.values.map((value, index) => ({
      id: `m${index}`,
      kind: 'body_weight' as MetricKind,
      measuredAtMs: 1000 + index,
      value,
      unit: 'kg' as const,
      notes: null,
    }));

    const stats = buildMetricStats(points);
    const expected = testCase.expected;

    expect(stats.count).toBe(expected.count);
    expect(stats.first).toBe(expected.first);
    expect(stats.latest).toBe(expected.latest);
    expect(stats.min).toBe(expected.min);
    expect(stats.max).toBe(expected.max);
    expect(stats.deltaAbs).toBe(expected.delta_abs);
    expect(stats.deltaPct).toBe(expected.delta_pct);
  });
});

describe('isMetricKind y formato', () => {
  it('reconoce tipos validos y rechaza desconocidos', () => {
    expect(isMetricKind('body_weight')).toBe(true);
    expect(isMetricKind('altura')).toBe(false);
  });

  it('formatea valores con unidad', () => {
    expect(formatMetricValue(78.5, 'kg')).toBe('78.5 kg');
    expect(formatMetricValue(80, 'cm')).toBe('80 cm');
    expect(formatMetricValue(18.4, '%')).toBe('18.4 %');
  });
});
