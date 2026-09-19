import { describe, expect, it } from 'vitest';

import { formatDuration, summarizeSession, type SessionSetInput } from '@/domain/workout';

import vectors from '@shared/test-vectors/session-summary.json';

interface VectorSet {
  exercise_id: string;
  weight_kg: number | null;
  reps: number | null;
  is_warmup: number;
}

interface VectorCase {
  name: string;
  sets: VectorSet[];
  expected: {
    total_sets: number;
    working_sets: number;
    total_volume_kg: number;
    volume_by_exercise: Record<string, number>;
  };
}

const cases: VectorCase[] = vectors.cases;

describe('summarizeSession', () => {
  it.each(cases)('$name', (testCase) => {
    const sets: SessionSetInput[] = testCase.sets.map((set) => ({
      exerciseId: set.exercise_id,
      weightKg: set.weight_kg,
      reps: set.reps,
      isWarmup: set.is_warmup === 1,
    }));

    const summary = summarizeSession(sets);

    expect(summary.totalSets).toBe(testCase.expected.total_sets);
    expect(summary.workingSets).toBe(testCase.expected.working_sets);
    expect(summary.totalVolumeKg).toBeCloseTo(testCase.expected.total_volume_kg, 9);

    const expectedVolume = Object.entries(testCase.expected.volume_by_exercise).sort();
    const actualVolume = Object.entries(summary.volumeByExercise).sort();
    expect(actualVolume).toHaveLength(expectedVolume.length);
    for (const [index, [exerciseId, volume]] of expectedVolume.entries()) {
      const actual = actualVolume[index];
      expect(actual?.[0]).toBe(exerciseId);
      expect(actual?.[1]).toBeCloseTo(volume, 9);
    }
  });
});

describe('formatDuration', () => {
  it('devuelve null si la sesion sigue activa', () => {
    expect(formatDuration(0, null)).toBeNull();
  });

  it('formatea minutos y horas', () => {
    expect(formatDuration(0, 45 * 60_000)).toBe('45 min');
    expect(formatDuration(0, 90 * 60_000)).toBe('1 h 30 min');
  });
});
