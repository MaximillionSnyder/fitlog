import { useCallback, useEffect, useMemo, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { loadComparisonSets, loadExerciseGroups } from '@/data/comparisons';
import {
  buildMuscleBalance,
  buildPersonalRecords,
  comparePeriods,
  comparisonRanges,
  type ComparisonSetInput,
  type MuscleBalanceEntry,
  type PeriodComparison,
  type PersonalRecord,
} from '@/domain/comparisons';

export type ComparisonPreset = '30d' | '90d';

export interface ComparisonsState {
  readonly records: PersonalRecord[];
  readonly comparison: PeriodComparison | null;
  readonly balance: MuscleBalanceEntry[];
  readonly groups: Record<string, string>;
  readonly preset: ComparisonPreset;
  readonly loading: boolean;
  readonly error: string | null;
  selectPreset(preset: ComparisonPreset): void;
  reload(): void;
}

interface RawData {
  readonly sets: ComparisonSetInput[];
  readonly groups: Record<string, string>;
  readonly loadedAt: number;
}

export function useComparisons(db: FitLogDb | undefined): ComparisonsState {
  const [raw, setRaw] = useState<RawData | null>(null);
  const [preset, setPreset] = useState<ComparisonPreset>('30d');
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

  const derived = useMemo(() => {
    if (!raw) {
      return { records: [], comparison: null, balance: [] };
    }
    const ranges = comparisonRanges(preset, raw.loadedAt);
    return {
      records: buildPersonalRecords(raw.sets),
      comparison: comparePeriods(raw.sets, ranges.current, ranges.previous),
      balance: buildMuscleBalance(raw.sets, raw.groups, ranges.current),
    };
  }, [raw, preset]);

  return {
    records: derived.records,
    comparison: derived.comparison,
    balance: derived.balance,
    groups: raw?.groups ?? {},
    preset,
    loading: raw === null && error === null,
    error,
    selectPreset: setPreset,
    reload,
  };
}
