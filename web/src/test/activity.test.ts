import { describe, expect, it } from 'vitest';

import {
  activityTotals,
  activityValue,
  activityValues,
  buildActivitySeries,
  type ActivityInput,
} from '@/domain/activity';
import { rangeFor } from '@/domain/progress';

const now = 1_800_000_000_000;
const day = 86_400_000;

function input(
  daysAgo: number,
  distanceM: number | null = null,
  durationMs = 1_800_000,
  heartRate: number | null = null
): ActivityInput {
  return {
    startedAtMs: now - daysAgo * day,
    finishedAtMs: now - daysAgo * day + durationMs,
    distanceM,
    averageHeartRate: heartRate,
  };
}

describe('buildActivitySeries', () => {
  it('arma la serie en orden cronológico y dentro del rango', () => {
    const series = buildActivitySeries(
      [input(2, 5_000), input(40, 8_000), input(1, 3_000)],
      rangeFor('30d', now)
    );

    expect(series).toHaveLength(2);
    // Cronológico: primero la más vieja del rango.
    expect(series[0]!.distanceM).toBe(5_000);
    expect(series[1]!.distanceM).toBe(3_000);
  });

  it('sin rango entran todas las sesiones', () => {
    expect(buildActivitySeries([input(1, 1_000), input(400, 2_000)], null)).toHaveLength(2);
  });

  it('la duración sale de las fechas de la sesión', () => {
    const series = buildActivitySeries([input(1, null, 2_700_000)], null);
    expect(series[0]!.durationMs).toBe(2_700_000);
  });

  it('solo se grafican los entrenamientos que traen la métrica', () => {
    const series = buildActivitySeries([input(2, 5_000, 1_800_000, 145), input(1)], null);

    expect(activityValues(series, 'distance')).toHaveLength(1);
    expect(activityValues(series, 'heart_rate')).toHaveLength(1);
    // La duración siempre está: sale de las fechas.
    expect(activityValues(series, 'duration')).toHaveLength(2);
    expect(activityValue(series[1]!, 'distance')).toBeNull();
  });

  it('suma distancia y tiempo del periodo', () => {
    const series = buildActivitySeries(
      [input(2, 5_000, 1_800_000), input(1, 3_000, 1_200_000)],
      null
    );

    const totals = activityTotals(series);
    expect(totals.sessions).toBe(2);
    expect(totals.distanceM).toBe(8_000);
    expect(totals.durationMs).toBe(3_000_000);
  });
});
