import { useCallback, useEffect, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { loadProgressSeries } from '@/data/progress';
import { rangeFor, type ProgressMetric, type ProgressPoint, type RangePreset } from '@/domain/progress';

export interface ProgressState {
  readonly points: ProgressPoint[];
  readonly loading: boolean;
  readonly error: string | null;
  readonly exerciseId: string | null;
  readonly metric: ProgressMetric;
  readonly preset: RangePreset;
  selectExercise(exerciseId: string | null): void;
  selectMetric(metric: ProgressMetric): void;
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
    selectExercise: setExerciseId,
    selectMetric: setMetric,
    selectPreset: setPreset,
    reload,
  };
}
