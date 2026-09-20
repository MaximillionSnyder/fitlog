import { useCallback, useMemo, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { importSessions, listSessions, type ImportSessionsResult } from '@/data/workout';
import { huaweiNote, parseHuaweiExport, type HuaweiWorkout } from '@/domain/huaweiHealth';

export type ImportStep = 'empty' | 'preview' | 'done';

export interface ImportState {
  readonly step: ImportStep;
  readonly reading: boolean;
  readonly importing: boolean;
  readonly error: string | null;
  readonly filesRead: number;
  readonly workouts: readonly HuaweiWorkout[];
  readonly alreadyImported: number;
  readonly result: ImportSessionsResult | null;
  readonly pending: number;
  readonly canImport: boolean;
  readonly sports: readonly { name: string; count: number }[];
  readonly firstAtMs: number | null;
  readonly lastAtMs: number | null;
  readFiles(contents: readonly string[]): Promise<void>;
  runImport(): Promise<void>;
  reset(): void;
}

/**
 * Importacion de entrenamientos desde una exportacion de Huawei Health.
 *
 * No se escribe nada hasta que el usuario confirma: primero se lee, se muestra la vista previa y
 * recien despues se importa.
 */
export function useImport(db: FitLogDb | undefined): ImportState {
  const [step, setStep] = useState<ImportStep>('empty');
  const [reading, setReading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filesRead, setFilesRead] = useState(0);
  const [workouts, setWorkouts] = useState<readonly HuaweiWorkout[]>([]);
  const [alreadyImported, setAlreadyImported] = useState(0);
  const [result, setResult] = useState<ImportSessionsResult | null>(null);

  const readFiles = useCallback(
    async (contents: readonly string[]) => {
      if (contents.length === 0) {
        setError('No se eligió ningún archivo');
        setStep('empty');
        return;
      }

      setReading(true);
      setError(null);
      setResult(null);
      try {
        const parsed = parseHuaweiExport(contents);
        const existing = new Set<string>();
        if (db) {
          for (const session of await listSessions(db)) existing.add(String(session.startedAt));
        }
        setFilesRead(parsed.filesRead);
        setWorkouts(parsed.workouts);
        setAlreadyImported(
          parsed.workouts.filter((workout) => existing.has(String(workout.startedAtMs))).length
        );
        setStep('preview');
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : 'No se pudieron leer los archivos');
        setStep('empty');
      } finally {
        setReading(false);
      }
    },
    [db]
  );

  const pending = workouts.length - alreadyImported;
  const canImport = step === 'preview' && pending > 0 && !importing;

  const runImport = useCallback(async () => {
    if (!db || !canImport) return;
    setImporting(true);
    setError(null);
    try {
      const imported = await importSessions(
        db,
        workouts.map((workout) => ({
          startedAtMs: workout.startedAtMs,
          finishedAtMs: workout.finishedAtMs,
          notes: huaweiNote(workout),
        }))
      );
      setResult(imported);
      setStep('done');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo importar');
    } finally {
      setImporting(false);
    }
  }, [canImport, db, workouts]);

  const reset = useCallback(() => {
    setStep('empty');
    setReading(false);
    setImporting(false);
    setError(null);
    setFilesRead(0);
    setWorkouts([]);
    setAlreadyImported(0);
    setResult(null);
  }, []);

  const sports = useMemo(() => {
    const counts = new Map<string, number>();
    for (const workout of workouts) {
      counts.set(workout.sportName, (counts.get(workout.sportName) ?? 0) + 1);
    }
    return [...counts.entries()]
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }, [workouts]);

  const firstAtMs = useMemo(
    () => (workouts.length === 0 ? null : Math.min(...workouts.map((w) => w.startedAtMs))),
    [workouts]
  );
  const lastAtMs = useMemo(
    () => (workouts.length === 0 ? null : Math.max(...workouts.map((w) => w.startedAtMs))),
    [workouts]
  );

  return {
    step,
    reading,
    importing,
    error,
    filesRead,
    workouts,
    alreadyImported,
    result,
    pending,
    canImport,
    sports,
    firstAtMs,
    lastAtMs,
    readFiles,
    runImport,
    reset,
  };
}
