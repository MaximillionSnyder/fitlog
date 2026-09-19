import { useCallback, useEffect, useMemo, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import {
  createBodyMetric,
  deleteBodyMetric,
  listBodyMetrics,
  updateBodyMetric,
  type BodyMetricInput,
  type BodyMetricUpdate,
} from '@/data/body';
import {
  buildMetricSeries,
  buildMetricStats,
  type BodyMetricPoint,
  type MetricKind,
  type MetricStats,
} from '@/domain/body';
import { rangeFor, type RangePreset } from '@/domain/progress';

export interface BodyMetricsState {
  readonly metrics: BodyMetricPoint[];
  readonly series: BodyMetricPoint[];
  readonly stats: MetricStats;
  readonly kind: MetricKind;
  readonly preset: RangePreset;
  readonly loading: boolean;
  readonly error: string | null;
  selectKind(kind: MetricKind): void;
  selectPreset(preset: RangePreset): void;
  add(input: Omit<BodyMetricInput, 'kind'>): Promise<void>;
  edit(id: string, input: BodyMetricUpdate): Promise<void>;
  remove(id: string): Promise<void>;
}

interface LoadedMetrics {
  readonly metrics: BodyMetricPoint[];
  readonly loadedAt: number;
}

const EMPTY: LoadedMetrics = { metrics: [], loadedAt: 0 };

export function useBodyMetrics(db: FitLogDb | undefined): BodyMetricsState {
  const [loaded, setLoaded] = useState<LoadedMetrics>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [kind, setKind] = useState<MetricKind>('body_weight');
  const [preset, setPreset] = useState<RangePreset>('90d');

  useEffect(() => {
    if (!db) return;
    let cancelled = false;
    listBodyMetrics(db).then(
      (metrics) => {
        if (cancelled) return;
        setLoaded({ metrics, loadedAt: Date.now() });
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
    if (!db) return;
    setLoaded({ metrics: await listBodyMetrics(db), loadedAt: Date.now() });
  }, [db]);

  const guarded = useCallback(async (action: () => Promise<void>) => {
    setError(null);
    try {
      await action();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    }
  }, []);

  const series = useMemo(
    () => buildMetricSeries(loaded.metrics, kind, rangeFor(preset, loaded.loadedAt)),
    [loaded, kind, preset]
  );

  const stats = useMemo(() => buildMetricStats(series), [series]);

  const add = useCallback(
    (input: Omit<BodyMetricInput, 'kind'>) =>
      guarded(async () => {
        if (!db) return;
        await createBodyMetric(db, { ...input, kind });
        await reload();
      }),
    [db, guarded, kind, reload]
  );

  const edit = useCallback(
    (id: string, input: BodyMetricUpdate) =>
      guarded(async () => {
        if (!db) return;
        await updateBodyMetric(db, id, input);
        await reload();
      }),
    [db, guarded, reload]
  );

  const remove = useCallback(
    (id: string) =>
      guarded(async () => {
        if (!db) return;
        await deleteBodyMetric(db, id);
        await reload();
      }),
    [db, guarded, reload]
  );

  return {
    metrics: loaded.metrics,
    series,
    stats,
    kind,
    preset,
    loading,
    error,
    selectKind: setKind,
    selectPreset: setPreset,
    add,
    edit,
    remove,
  };
}
