import { useCallback, useEffect, useMemo, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { loadComparisonSets, loadExerciseGroups } from '@/data/comparisons';
import { comparisonRanges, type ComparisonSetInput } from '@/domain/comparisons';
import { buildTips, type Tip } from '@/domain/insights';

export type TipsPreset = '30d' | '90d';

export interface TipsState {
  readonly tips: Tip[];
  readonly loading: boolean;
  readonly error: string | null;
  readonly preset: TipsPreset;
  selectPreset(preset: TipsPreset): void;
  reload(): void;
}

interface RawData {
  readonly sets: ComparisonSetInput[];
  readonly groups: Record<string, string>;
  readonly loadedAt: number;
}

export function useTips(db: FitLogDb | undefined): TipsState {
  const [raw, setRaw] = useState<RawData | null>(null);
  const [preset, setPreset] = useState<TipsPreset>('30d');
  const [error, setError] = useState<string | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!db) return;
    let cancelled = false;
    Promise.all([loadComparisonSets(db), loadExerciseGroups(db)]).then(
      ([sets, groups]) => {
        if (cancelled) return;
        setRaw({ sets, groups, loadedAt: Date.now() });
        setError(null);
      },
      (cause: unknown) => {
        if (cancelled) return;
        setError(cause instanceof Error ? cause.message : String(cause));
      }
    );
    return () => {
      cancelled = true;
    };
  }, [db, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);

  const tips = useMemo(() => {
    if (!raw) return [];
    const days = preset === '30d' ? 30 : 90;
    const ranges = comparisonRanges(preset, raw.loadedAt);
    return buildTips({
      sets: raw.sets,
      groupByExercise: raw.groups,
      range: ranges.current,
      periodDays: days,
    });
  }, [raw, preset]);

  return {
    tips,
    loading: raw === null && error === null,
    error,
    preset,
    selectPreset: setPreset,
    reload,
  };
}
