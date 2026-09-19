import { useCallback, useEffect, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import {
  createCustomExercise,
  deleteCustomExercise,
  loadCatalog,
  type CatalogSnapshot,
  type CreateCustomExerciseInput,
} from '@/data/catalog';

export interface CatalogState {
  readonly snapshot: CatalogSnapshot;
  readonly loading: boolean;
  readonly error: string | null;
  create(input: CreateCustomExerciseInput): Promise<void>;
  remove(id: string): Promise<void>;
  reload(): Promise<void>;
}

const EMPTY: CatalogSnapshot = { groups: [], exercises: [] };

export function useCatalog(db: FitLogDb | undefined): CatalogState {
  const [snapshot, setSnapshot] = useState<CatalogSnapshot>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!db) {
      return;
    }
    let cancelled = false;
    loadCatalog(db).then(
      (data) => {
        if (cancelled) return;
        setSnapshot(data);
        setError(null);
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

  const reload = useCallback(async () => {
    if (!db) {
      return;
    }
    setLoading(true);
    try {
      setSnapshot(await loadCatalog(db));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setLoading(false);
    }
  }, [db]);

  const create = useCallback(
    async (input: CreateCustomExerciseInput) => {
      if (!db) {
        return;
      }
      await createCustomExercise(db, input);
      await reload();
    },
    [db, reload]
  );

  const remove = useCallback(
    async (id: string) => {
      if (!db) {
        return;
      }
      await deleteCustomExercise(db, id);
      await reload();
    },
    [db, reload]
  );

  return { snapshot, loading, error, create, remove, reload };
}
