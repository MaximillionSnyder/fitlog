import { describe, expect, it } from 'vitest';

import type { ComparisonSetInput } from '@/domain/comparisons';
import { buildTips, formatNumber, THRESHOLDS, type InsightsInput } from '@/domain/insights';

import vectors from '@shared/test-vectors/insights.json';

interface VectorSet {
  exercise_id: string;
  session_id: string;
  started_at_ms: number;
  weight_kg: number | null;
  reps: number | null;
  is_warmup: number;
}

interface VectorCase {
  name: string;
  sets: VectorSet[];
  groups: Record<string, string>;
  range: { from_ms: number | null; to_ms: number };
  period_days: number;
  expected: {
    kind: string;
    severity: string;
    subject: string | null;
    value: number;
    message: string;
  }[];
}

function toInput(testCase: VectorCase): InsightsInput {
  const sets: ComparisonSetInput[] = testCase.sets.map((set) => ({
    exerciseId: set.exercise_id,
    sessionId: set.session_id,
    startedAtMs: set.started_at_ms,
    weightKg: set.weight_kg,
    reps: set.reps,
    isWarmup: set.is_warmup === 1,
  }));

  return {
    sets,
    groupByExercise: testCase.groups,
    range: { fromMs: testCase.range.from_ms, toMs: testCase.range.to_ms },
    periodDays: testCase.period_days,
  };
}

describe('buildTips', () => {
  it.each(vectors.cases as VectorCase[])('$name', (testCase) => {
    const tips = buildTips(toInput(testCase));

    expect(tips).toHaveLength(testCase.expected.length);
    tips.forEach((tip, index) => {
      const expected = testCase.expected[index];
      expect(tip.kind).toBe(expected?.kind);
      expect(tip.severity).toBe(expected?.severity);
      expect(tip.subject).toBe(expected?.subject);
      expect(tip.value).toBeCloseTo(expected?.value ?? 0, 9);
      expect(tip.message).toBe(expected?.message);
    });
  });
});

describe('formatNumber', () => {
  it('imprime enteros sin decimales y el resto con un decimal', () => {
    expect(formatNumber(25)).toBe('25');
    expect(formatNumber(57.14)).toBe('57.1');
    expect(formatNumber(33.333)).toBe('33.3');
  });
});

describe('umbrales', () => {
  it('expone los umbrales documentados', () => {
    expect(THRESHOLDS.progressMinSessions).toBe(3);
    expect(THRESHOLDS.stagnationMaxVariationPct).toBe(0.5);
    expect(THRESHOLDS.imbalanceMinSharePct).toBe(50);
    expect(THRESHOLDS.consistencyHighPerWeek).toBe(3);
    expect(THRESHOLDS.consistencyLowPerWeek).toBe(1.5);
    expect(THRESHOLDS.incompleteMinSharePct).toBe(30);
  });
});
