import { useCallback, useEffect, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import {
  addRoutineExercise,
  createRoutine,
  deleteRoutine,
  listRoutines,
  moveRoutineExercise,
  removeRoutineExercise,
  updateRoutine,
  type Routine,
  type RoutineExerciseInput,
} from '@/data/routines';

export interface RoutinesState {
  readonly routines: Routine[];
  readonly loading: boolean;
  readonly error: string | null;
  create(name: string, description: string | null): Promise<void>;
  update(routineId: string, name: string, description: string | null): Promise<void>;
  remove(routineId: string): Promise<void>;
  addExercise(routineId: string, input: RoutineExerciseInput): Promise<void>;
  removeExercise(routineExerciseId: string): Promise<void>;
  moveExercise(routineExerciseId: string, direction: 'up' | 'down'): Promise<void>;
}

export function useRoutines(db: FitLogDb | undefined): RoutinesState {
  const [routines, setRoutines] = useState<Routine[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!db) return;
    let cancelled = false;
    listRoutines(db).then(
      (loaded) => {
        if (cancelled) return;
        setRoutines(loaded);
        setLoading(false);
      },
      (cause: unknown) => {
        if (cancelled) return;
        setError(cause instanceof Error ? cause.message : String(cause));
        setLoading(false);
      }
    );
    return () => {
      cancelled = true;
    };
  }, [db]);

  const refresh = useCallback(async () => {
    if (!db) return;
    setRoutines(await listRoutines(db));
  }, [db]);

  const guarded = useCallback(
    async (action: () => Promise<void>) => {
      setError(null);
      try {
        await action();
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : String(cause));
      }
    },
    []
  );

  const create = useCallback(
    (name: string, description: string | null) =>
      guarded(async () => {
        if (!db) return;
        await createRoutine(db, { name, description });
        await refresh();
      }),
    [db, guarded, refresh]
  );

  const update = useCallback(
    (routineId: string, name: string, description: string | null) =>
      guarded(async () => {
        if (!db) return;
        await updateRoutine(db, routineId, { name, description });
        await refresh();
      }),
    [db, guarded, refresh]
  );

  const remove = useCallback(
    (routineId: string) =>
      guarded(async () => {
        if (!db) return;
        await deleteRoutine(db, routineId);
        await refresh();
      }),
    [db, guarded, refresh]
  );

  const addExercise = useCallback(
    (routineId: string, input: RoutineExerciseInput) =>
      guarded(async () => {
        if (!db) return;
        await addRoutineExercise(db, routineId, input);
        await refresh();
      }),
    [db, guarded, refresh]
  );

  const removeExercise = useCallback(
    (routineExerciseId: string) =>
      guarded(async () => {
        if (!db) return;
        await removeRoutineExercise(db, routineExerciseId);
        await refresh();
      }),
    [db, guarded, refresh]
  );

  const moveExercise = useCallback(
    (routineExerciseId: string, direction: 'up' | 'down') =>
      guarded(async () => {
        if (!db) return;
        await moveRoutineExercise(db, routineExerciseId, direction);
        await refresh();
      }),
    [db, guarded, refresh]
  );

  return { routines, loading, error, create, update, remove, addExercise, removeExercise, moveExercise };
}
