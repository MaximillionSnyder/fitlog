import { describe, expect, it } from 'vitest';

import {
  buildMuscleBalance,
  buildPersonalRecords,
  comparePeriods,
  comparisonRanges,
  formatDelta,
  type ComparisonSetInput,
} from '@/domain/comparisons';
import type { ProgressRange } from '@/domain/progress';

import vectors from '@shared/test-vectors/comparisons.json';

interface VectorSet {
  exercise_id: string;
  session_id: string;
  started_at_ms: number;
  weight_kg: number | null;
  reps: number | null;
  is_warmup: number;
}

function toInputs(sets: VectorSet[]): ComparisonSetInput[] {
  return sets.map((set) => ({
    exerciseId: set.exercise_id,
    sessionId: set.session_id,
    startedAtMs: set.started_at_ms,
    weightKg: set.weight_kg,
    reps: set.reps,
    isWarmup: set.is_warmup === 1,
  }));
}

function toRange(range: { from_ms: number | null; to_ms: number }): ProgressRange {
  return { fromMs: range.from_ms, toMs: range.to_ms };
}

describe('comparisonRanges', () => {
  it.each(vectors.range_cases)('$name', (testCase) => {
    const ranges = comparisonRanges(testCase.preset as '30d' | '90d', testCase.now_ms);
    expect(ranges.current).toEqual(toRange(testCase.expected.current));
    expect(ranges.previous).toEqual(toRange(testCase.expected.previous));
  });
});

describe('buildPersonalRecords', () => {
  it.each(vectors.pr_cases)('$name', (testCase) => {
    const records = buildPersonalRecords(toInputs(testCase.sets as VectorSet[]));

    expect(records).toHaveLength(testCase.expected.length);
    records.forEach((record, index) => {
      const expected = testCase.expected[index];
      expect(record.exerciseId).toBe(expected?.exercise_id);
      expect(record.bestWeightKg).toBeCloseTo(expected?.best_weight_kg ?? 0, 9);
      expect(record.bestWeightAtMs).toBe(expected?.best_weight_at_ms);
      expect(record.bestOneRepMaxKg).toBeCloseTo(expected?.best_one_rep_max_kg ?? 0, 9);
      expect(record.bestOneRepMaxAtMs).toBe(expected?.best_one_rep_max_at_ms);
      expect(record.bestSessionVolumeKg).toBeCloseTo(expected?.best_session_volume_kg ?? 0, 9);
      expect(record.bestSessionVolumeAtMs).toBe(expected?.best_session_volume_at_ms);
      expect(record.bestReps).toBe(expected?.best_reps);
      expect(record.bestRepsAtMs).toBe(expected?.best_reps_at_ms);
    });
  });
});

describe('comparePeriods', () => {
  it.each(vectors.comparison_cases)('$name', (testCase) => {
    const comparison = comparePeriods(
      toInputs(testCase.sets as VectorSet[]),
      toRange(testCase.current),
      toRange(testCase.previous)
    );

    expect(comparison.current.volumeKg).toBeCloseTo(testCase.expected.current.volume_kg, 9);
    expect(comparison.current.workingSets).toBe(testCase.expected.current.working_sets);
    expect(comparison.current.sessions).toBe(testCase.expected.current.sessions);
    expect(comparison.previous.volumeKg).toBeCloseTo(testCase.expected.previous.volume_kg, 9);
    expect(comparison.previous.workingSets).toBe(testCase.expected.previous.working_sets);
    expect(comparison.previous.sessions).toBe(testCase.expected.previous.sessions);
    expect(comparison.volumeDeltaPct).toBe(testCase.expected.volume_delta_pct);
    expect(comparison.setsDeltaPct).toBe(testCase.expected.sets_delta_pct);
    expect(comparison.sessionsDeltaPct).toBe(testCase.expected.sessions_delta_pct);
  });
});

describe('buildMuscleBalance', () => {
  it.each(vectors.balance_cases)('$name', (testCase) => {
    const balance = buildMuscleBalance(
      toInputs(testCase.sets as VectorSet[]),
      testCase.groups as Record<string, string>
    );

    expect(balance).toHaveLength(testCase.expected.length);
    balance.forEach((entry, index) => {
      const expected = testCase.expected[index];
      expect(entry.muscleGroupSlug).toBe(expected?.muscle_group_slug);
      expect(entry.volumeKg).toBeCloseTo(expected?.volume_kg ?? 0, 9);
      expect(entry.sharePct).toBeCloseTo(expected?.share_pct ?? 0, 9);
    });
  });
});

describe('formatDelta', () => {
  it('formatea porcentajes y ausencia de datos', () => {
    expect(formatDelta(25)).toBe('+25%');
    expect(formatDelta(-10.5)).toBe('-10.5%');
    expect(formatDelta(null)).toBe('sin datos');
  });
});
