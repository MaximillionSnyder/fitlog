import { describe, expect, it } from 'vitest';

import { estimatedOneRepMaxKg, volumeKg } from '@/domain/formulas';

import vectors from '@shared/test-vectors/formulas.json';

interface VolumeCase {
  weight_kg: number | null;
  reps: number | null;
  expected: number | null;
}

interface OneRepMaxCase {
  weight_kg: number | null;
  reps: number | null;
  expected: number | null;
}

const volumeCases: VolumeCase[] = vectors.volume;
const oneRepMaxCases: OneRepMaxCase[] = vectors.one_rep_max_epley;

describe('volumeKg', () => {
  it.each(volumeCases)(
    'peso $weight_kg x $reps repeticiones -> $expected',
    ({ weight_kg, reps, expected }) => {
      expect(volumeKg(weight_kg, reps)).toBe(expected);
    }
  );
});

describe('estimatedOneRepMaxKg (Epley)', () => {
  it.each(oneRepMaxCases)(
    'peso $weight_kg x $reps repeticiones -> $expected',
    ({ weight_kg, reps, expected }) => {
      const result = estimatedOneRepMaxKg(weight_kg, reps);
      if (expected === null) {
        expect(result).toBeNull();
        return;
      }
      expect(result).not.toBeNull();
      expect(result as number).toBeCloseTo(expected, 9);
    }
  );
});
