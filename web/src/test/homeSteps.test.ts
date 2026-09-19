import { describe, expect, it } from 'vitest';

import { buildHomeSteps } from '@/domain/home';

describe('buildHomeSteps', () => {
  it('arranca con los tres pasos pendientes', () => {
    const steps = buildHomeSteps(0, 0, 0);
    expect(steps.doneCount).toBe(0);
    expect(steps.total).toBe(3);
    expect(steps.isComplete).toBe(false);
    expect(steps.items.map((item) => item.id)).toEqual(['routine', 'workout', 'body_metric']);
  });

  it('marca cada paso con su dato', () => {
    const steps = buildHomeSteps(1, 0, 2);
    expect(steps.items.find((item) => item.id === 'routine')?.done).toBe(true);
    expect(steps.items.find((item) => item.id === 'workout')?.done).toBe(false);
    expect(steps.items.find((item) => item.id === 'body_metric')?.done).toBe(true);
    expect(steps.doneCount).toBe(2);
  });

  it('se completa cuando estan los tres', () => {
    expect(buildHomeSteps(1, 1, 1).isComplete).toBe(true);
  });
});
