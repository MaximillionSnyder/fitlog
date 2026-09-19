import { useCallback, useEffect, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import {
  addSet,
  deleteSet,
  finishSession,
  getActiveSession,
  getSessionDetail,
  listSessions,
  startSession,
  updateSet,
  type AddSetInput,
  type SessionDetail,
  type UpdateSetInput,
  type WorkoutSession,
  type WorkoutSet,
} from '@/data/workout';

export interface WorkoutState {
  readonly active: WorkoutSession | null;
  readonly activeSets: WorkoutSet[];
  readonly history: WorkoutSession[];
  readonly detail: SessionDetail | null;
  readonly loading: boolean;
  readonly error: string | null;
  start(): Promise<void>;
  finish(): Promise<void>;
  add(input: Omit<AddSetInput, 'sessionId'>): Promise<void>;
  update(setId: string, input: UpdateSetInput): Promise<void>;
  remove(setId: string): Promise<void>;
  openDetail(sessionId: string): Promise<void>;
  closeDetail(): void;
}

interface LoadedState {
  readonly active: WorkoutSession | null;
  readonly activeSets: WorkoutSet[];
  readonly history: WorkoutSession[];
}

async function loadState(db: FitLogDb): Promise<LoadedState> {
  const [active, history] = await Promise.all([getActiveSession(db), listSessions(db)]);
  const activeSets = active ? (await getSessionDetail(db, active.id)).sets : [];
  return { active, activeSets, history };
}

export function useWorkout(db: FitLogDb | undefined): WorkoutState {
  const [state, setState] = useState<LoadedState>({ active: null, activeSets: [], history: [] });
  const [detail, setDetail] = useState<SessionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!db) {
      return;
    }
    let cancelled = false;
    loadState(db).then(
      (loaded) => {
        if (cancelled) return;
        setState(loaded);
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
    try {
      setState(await loadState(db));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }, [db]);

  const guarded = useCallback(async (action: () => Promise<void>) => {
    setError(null);
    try {
      await action();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }, []);

  const start = useCallback(
    () =>
      guarded(async () => {
        if (!db) return;
        await startSession(db);
        await refresh();
      }),
    [db, guarded, refresh]
  );

  const finish = useCallback(
    () =>
      guarded(async () => {
        if (!db || !state.active) return;
        await finishSession(db, state.active.id);
        setDetail(null);
        await refresh();
      }),
    [db, guarded, refresh, state.active]
  );

  const add = useCallback(
    (input: Omit<AddSetInput, 'sessionId'>) =>
      guarded(async () => {
        if (!db || !state.active) return;
        await addSet(db, { ...input, sessionId: state.active.id });
        await refresh();
      }),
    [db, guarded, refresh, state.active]
  );

  const update = useCallback(
    (setId: string, input: UpdateSetInput) =>
      guarded(async () => {
        if (!db) return;
        await updateSet(db, setId, input);
        await refresh();
        setDetail((current) => {
          if (!current) return current;
          return {
            ...current,
            sets: current.sets.map((set) =>
              set.id === setId
                ? {
                    ...set,
                    weightKg: input.weightKg,
                    reps: input.reps,
                    rir: input.rir,
                    notes: input.notes,
                  }
                : set
            ),
          };
        });
      }),
    [db, guarded, refresh]
  );

  const remove = useCallback(
    (setId: string) =>
      guarded(async () => {
        if (!db) return;
        await deleteSet(db, setId);
        await refresh();
        setDetail((current) =>
          current ? { ...current, sets: current.sets.filter((set) => set.id !== setId) } : current
        );
      }),
    [db, guarded, refresh]
  );

  const openDetail = useCallback(
    async (sessionId: string) => {
      if (!db) return;
      try {
        setDetail(await getSessionDetail(db, sessionId));
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : String(cause));
      }
    },
    [db]
  );

  const closeDetail = useCallback(() => setDetail(null), []);

  return {
    active: state.active,
    activeSets: state.activeSets,
    history: state.history,
    detail,
    loading,
    error,
    start,
    finish,
    add,
    update,
    remove,
    openDetail,
    closeDetail,
  };
}
