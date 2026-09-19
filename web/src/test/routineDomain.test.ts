import { describe, expect, it } from 'vitest';

import { assignPositions, moveItem, RoutineOrderError } from '@/domain/routines';

import vectors from '@shared/test-vectors/routine-order.json';

interface MoveCase {
  name: string;
  ids: string[];
  from_index: number;
  to_index: number;
  expected: string[];
}

interface InvalidCase {
  ids: string[];
  from_index: number;
  to_index: number;
}

interface PositionCase {
  name: string;
  ids: string[];
  expected: { id: string; position: number }[];
}

describe('moveItem', () => {
  it.each(vectors.move_cases as MoveCase[])('$name', (testCase) => {
    const original = [...testCase.ids];
    const result = moveItem(testCase.ids, testCase.from_index, testCase.to_index);

    expect(result).toEqual(testCase.expected);
    expect(testCase.ids).toEqual(original);
  });

  it.each(vectors.invalid_cases as InvalidCase[])(
    'rechaza mover $from_index -> $to_index',
    (testCase) => {
      expect(() => moveItem(testCase.ids, testCase.from_index, testCase.to_index)).toThrow(
        RoutineOrderError
      );
    }
  );
});

describe('assignPositions', () => {
  it.each(vectors.position_cases as PositionCase[])('$name', (testCase) => {
    expect(assignPositions(testCase.ids)).toEqual(testCase.expected);
  });
});
