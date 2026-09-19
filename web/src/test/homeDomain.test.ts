import { describe, expect, it } from 'vitest';

import { buildHomeSummary, type HomeSessionInput } from '@/domain/home';
import { DAY_MS } from '@/domain/progress';

const NOW = 1_800_000_000_000;

function session(daysAgo: number, volumeKg: number, workingSets = 4): HomeSessionInput {
  return {
    id: `s-${daysAgo}`,
    startedAt: NOW - daysAgo * DAY_MS,
    finishedAt: NOW - daysAgo * DAY_MS + 3_600_000,
    workingSets,
    volumeKg,
  };
}

describe('buildHomeSummary', () => {
  it('separa la ventana actual de la anterior', () => {
    const summary = buildHomeSummary(
      [session(1, 1000), session(6, 500), session(9, 300)],
      [],
      NOW
    );

    expect(summary.current.sessions).toBe(2);
    expect(summary.current.volumeKg).toBe(1500);
    expect(summary.previous.sessions).toBe(1);
    expect(summary.previous.volumeKg).toBe(300);
    expect(summary.totalSessions).toBe(3);
  });

  it('calcula la variación de volumen contra la ventana anterior', () => {
    const summary = buildHomeSummary([session(1, 1500), session(8, 1000)], [], NOW);
    expect(summary.volumeDeltaPercent).toBeCloseTo(50, 5);
  });

  it('sin ventana anterior no inventa una variación', () => {
    const summary = buildHomeSummary([session(2, 900)], [], NOW);
    expect(summary.volumeDeltaPercent).toBeNull();
    expect(summary.sessionsDeltaPercent).toBeNull();
  });

  it('cuenta semanas consecutivas con sesiones', () => {
    const summary = buildHomeSummary(
      [session(1, 100), session(9, 100), session(16, 100), session(40, 100)],
      [],
      NOW
    );
    expect(summary.streakWeeks).toBe(3);
  });

  it('la racha arranca en la semana anterior si la actual está vacía', () => {
    const summary = buildHomeSummary([session(9, 100), session(16, 100)], [], NOW);
    expect(summary.streakWeeks).toBe(2);
  });

  it('toma el último peso corporal registrado', () => {
    const summary = buildHomeSummary(
      [],
      [
        { measuredAt: NOW - 10 * DAY_MS, value: 81.4 },
        { measuredAt: NOW - 2 * DAY_MS, value: 79.8 },
      ],
      NOW
    );
    expect(summary.latestBodyWeightKg).toBe(79.8);
    expect(summary.latestBodyWeightAt).toBe(NOW - 2 * DAY_MS);
  });

  it('sin datos el panel queda en cero y sin error', () => {
    const summary = buildHomeSummary([], [], NOW);
    expect(summary.current.sessions).toBe(0);
    expect(summary.current.volumeKg).toBe(0);
    expect(summary.streakWeeks).toBe(0);
    expect(summary.latestBodyWeightKg).toBeNull();
  });

  it('ignora sesiones con fecha futura', () => {
    const summary = buildHomeSummary([session(-3, 500)], [], NOW);
    expect(summary.totalSessions).toBe(0);
  });
});
