import { describe, expect, it } from 'vitest';

import { buildSessionPace } from '@/domain/workout';

const minute = 60_000;

describe('buildSessionPace', () => {
  it('mide el rango entre la primera y la última serie efectiva', () => {
    const pace = buildSessionPace([
      { createdAtMs: 0, isWarmup: true },
      { createdAtMs: minute, isWarmup: false },
      { createdAtMs: 5 * minute, isWarmup: false },
      { createdAtMs: 11 * minute, isWarmup: false },
    ]);

    expect(pace.firstSetAtMs).toBe(minute);
    expect(pace.lastSetAtMs).toBe(11 * minute);
    expect(pace.spanMs).toBe(10 * minute);
    expect(pace.workingSets).toBe(3);
  });

  it('calcula el ritmo y el descanso promedio', () => {
    const pace = buildSessionPace([
      { createdAtMs: 0, isWarmup: false },
      { createdAtMs: 5 * minute, isWarmup: false },
      { createdAtMs: 10 * minute, isWarmup: false },
    ]);

    expect(pace.setsPerHour).toBeCloseTo(18, 5);
    expect(pace.averageRestMs).toBe(5 * minute);
  });

  it('sin series efectivas no inventa ritmo', () => {
    const pace = buildSessionPace([{ createdAtMs: minute, isWarmup: true }]);

    expect(pace.workingSets).toBe(0);
    expect(pace.setsPerHour).toBeNull();
    expect(pace.averageRestMs).toBeNull();
    expect(pace.firstSetAtMs).toBeNull();
  });

  it('una sola serie no tiene descanso promedio ni ritmo', () => {
    const pace = buildSessionPace([{ createdAtMs: minute, isWarmup: false }]);

    expect(pace.workingSets).toBe(1);
    expect(pace.spanMs).toBe(0);
    expect(pace.averageRestMs).toBeNull();
    expect(pace.setsPerHour).toBeNull();
  });
});
