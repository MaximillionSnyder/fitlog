import { useCallback, useEffect, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { loadProgressSeries } from '@/data/progress';
import { listSessions } from '@/data/workout';
import {
  activityTotals,
  activityValues,
  buildActivitySeries,
  type ActivityMetric,
  type ActivityPoint,
  type ActivityTotals,
} from '@/domain/activity';
import { rangeFor, type ProgressMetric, type ProgressPoint, type RangePreset } from '@/domain/progress';

export interface ProgressState {
  readonly points: ProgressPoint[];
  readonly loading: boolean;
  readonly error: string | null;
  readonly exerciseId: string | null;
  readonly metric: ProgressMetric;
  readonly preset: RangePreset;
  /** Actividad importada (Huawei Health o GPX) del rango elegido. */
  readonly activityPoints: ActivityPoint[];
  readonly activityMetric: ActivityMetric;
  readonly activityValues: number[];
  readonly activityTotals: ActivityTotals;
  selectExercise(exerciseId: string | null): void;
  selectMetric(metric: ProgressMetric): void;
  selectActivityMetric(metric: ActivityMetric): void;
  selectPreset(preset: RangePreset): void;
  reload(): void;
}

interface LoadedResult {
  readonly exerciseId: string | null;
  readonly preset: RangePreset;
  readonly points: ProgressPoint[];
  readonly error: string | null;
}

const EMPTY: LoadedResult = { exerciseId: null, preset: '90d', points: [], error: null };

export function useProgress(db: FitLogDb | undefined): ProgressState {
  const [result, setResult] = useState<LoadedResult>(EMPTY);
  const [exerciseId, setExerciseId] = useState<string | null>(null);
  const [metric, setMetric] = useState<ProgressMetric>('maxWeightKg');
  const [preset, setPreset] = useState<RangePreset>('90d');
  const [version, setVersion] = useState(0);
  const [activityPoints, setActivityPoints] = useState<ActivityPoint[]>([]);
  const [activityMetric, setActivityMetric] = useState<ActivityMetric>('distance');

  // La actividad importada se recarga con el mismo rango que la serie por ejercicio.
  useEffect(() => {
    if (!db) return;
    let cancelled = false;
    listSessions(db).then(
      (sessions) => {
        if (cancelled) return;
        setActivityPoints(
          buildActivitySeries(
            sessions.map((session) => ({
              startedAtMs: session.startedAt,
              finishedAtMs: session.finishedAt,
              distanceM: session.activity?.distanceM ?? null,
              averageHeartRate: session.activity?.averageHeartRate ?? null,
            })),
            rangeFor(preset, Date.now())
          )
        );
      },
      () => {
        if (!cancelled) setActivityPoints([]);
      }
    );
    return () => {
      cancelled = true;
    };
  }, [db, preset, version]);

  useEffect(() => {
    if (!db || exerciseId === null) {
      return;
    }
    let cancelled = false;
    loadProgressSeries(db, exerciseId, rangeFor(preset, Date.now())).then(
      (points) => {
        if (cancelled) return;
        setResult({ exerciseId, preset, points, error: null });
      },
      (cause: unknown) => {
        if (cancelled) return;
        setResult({
          exerciseId,
          preset,
          points: [],
          error: cause instanceof Error ? cause.message : String(cause),
        });
      }
    );
    return () => {
      cancelled = true;
    };
  }, [db, exerciseId, preset, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);

  const upToDate = result.exerciseId === exerciseId && result.preset === preset;

  return {
    points: exerciseId === null ? [] : result.points,
    loading: exerciseId !== null && !upToDate,
    error: upToDate ? result.error : null,
    exerciseId,
    metric,
    preset,
    activityPoints,
    activityMetric,
    activityValues: activityValues(activityPoints, activityMetric),
    activityTotals: activityTotals(activityPoints),
    selectExercise: setExerciseId,
    selectMetric: setMetric,
    selectActivityMetric: setActivityMetric,
    selectPreset: setPreset,
    reload,
  };
}
