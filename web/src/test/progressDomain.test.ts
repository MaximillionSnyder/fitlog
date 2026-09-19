import { describe, expect, it } from 'vitest';

import {
  buildProgressSeries,
  metricValue,
  rangeFor,
  type ProgressPoint,
  type ProgressSetInput,
} from '@/domain/progress';

import vectors from '@shared/test-vectors/progress-series.json';

interface VectorSet {
  session_id: string;
  started_at_ms: number;
  weight_kg: number | null;
  reps: number | null;
  is_warmup: number;
}

interface VectorPoint {
  session_id: string;
  started_at_ms: number;
  max_weight_kg: number;
  volume_kg: number;
  best_one_rep_max_kg: number;
  working_sets: number;
}

interface SeriesCase {
  name: string;
  sets: VectorSet[];
  expected: VectorPoint[];
}

interface FilterCase {
  name: string;
  from_ms: number | null;
  to_ms: number | null;
  expected: string[];
}

function toInputs(sets: VectorSet[]): ProgressSetInput[] {
  return sets.map((set) => ({
    sessionId: set.session_id,
    startedAtMs: set.started_at_ms,
    weightKg: set.weight_kg,
    reps: set.reps,
    isWarmup: set.is_warmup === 1,
  }));
}

function toExpected(points: VectorPoint[]): ProgressPoint[] {
  return points.map((point) => ({
    sessionId: point.session_id,
    startedAtMs: point.started_at_ms,
    maxWeightKg: point.max_weight_kg,
    volumeKg: point.volume_kg,
    bestOneRepMaxKg: point.best_one_rep_max_kg,
    workingSets: point.working_sets,
  }));
}

describe('rangeFor', () => {
  it.each(vectors.range_cases)('$name', (testCase) => {
    const range = rangeFor(testCase.preset as '30d' | '90d' | 'all', testCase.now_ms);
    if (testCase.expected === null) {
      expect(range).toBeNull();
      return;
    }
    expect(range?.fromMs).toBe(testCase.expected.from_ms);
    expect(range?.toMs).toBe(testCase.expected.to_ms);
  });
});

describe('buildProgressSeries', () => {
  it.each(vectors.series_cases as SeriesCase[])('$name', (testCase) => {
    const points = buildProgressSeries(toInputs(testCase.sets));
    const expected = toExpected(testCase.expected);

    expect(points).toHaveLength(expected.length);
    points.forEach((point, index) => {
      const target = expected[index];
      expect(point.sessionId).toBe(target?.sessionId);
      expect(point.startedAtMs).toBe(target?.startedAtMs);
      expect(point.maxWeightKg).toBeCloseTo(target?.maxWeightKg ?? 0, 9);
      expect(point.volumeKg).toBeCloseTo(target?.volumeKg ?? 0, 9);
      expect(point.bestOneRepMaxKg).toBeCloseTo(target?.bestOneRepMaxKg ?? 0, 9);
      expect(point.workingSets).toBe(target?.workingSets);
    });
  });

  it.each(vectors.filter_cases as FilterCase[])('filtro: $name', (testCase) => {
    const points = buildProgressSeries(
      toInputs(vectors.filter_sets as VectorSet[]),
      { fromMs: testCase.from_ms, toMs: testCase.to_ms ?? Number.MAX_SAFE_INTEGER }
    );
    expect(points.map((point) => point.sessionId)).toEqual(testCase.expected);
  });
});

describe('metricValue', () => {
  it('devuelve la metrica pedida', () => {
    const point: ProgressPoint = {
      sessionId: 's1',
      startedAtMs: 1000,
      maxWeightKg: 100,
      volumeKg: 800,
      bestOneRepMaxKg: 126.7,
      workingSets: 2,
    };

    expect(metricValue(point, 'maxWeightKg')).toBe(100);
    expect(metricValue(point, 'volumeKg')).toBe(800);
    expect(metricValue(point, 'bestOneRepMaxKg')).toBe(126.7);
  });
});
