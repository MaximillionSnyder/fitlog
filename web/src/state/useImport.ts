import { useCallback, useMemo, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { importSessions, listSessions, type ImportSessionsResult } from '@/data/workout';
import { parseGpx, looksLikeGpx } from '@/domain/gpx';
import { parseHuaweiExport } from '@/domain/huaweiHealth';
import {
  importedWorkoutNote,
  workoutKey,
  type ImportedWorkout,
} from '@/domain/importedWorkout';

export type ImportStep = 'empty' | 'preview' | 'done';

export interface ImportState {
  readonly step: ImportStep;
  readonly reading: boolean;
  readonly importing: boolean;
  readonly error: string | null;
  readonly filesRead: number;
  readonly filesSkipped: number;
  readonly workouts: readonly ImportedWorkout[];
  readonly alreadyImported: number;
  readonly result: ImportSessionsResult | null;
  readonly pending: number;
  readonly canImport: boolean;
  readonly sports: readonly { name: string; count: number }[];
  readonly firstAtMs: number | null;
  readonly lastAtMs: number | null;
  readFiles(files: readonly { name: string; content: string }[]): Promise<void>;
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
  const [filesSkipped, setFilesSkipped] = useState(0);
  const [workouts, setWorkouts] = useState<readonly ImportedWorkout[]>([]);
  const [alreadyImported, setAlreadyImported] = useState(0);
  const [result, setResult] = useState<ImportSessionsResult | null>(null);

  const readFiles = useCallback(
    async (files: readonly { name: string; content: string }[]) => {
      if (files.length === 0) {
        setError('No se eligió ningún archivo');
        setStep('empty');
        return;
      }

      setReading(true);
      setError(null);
      setResult(null);
      try {
        // Se decide por archivo: un GPX es XML y la exportación es JSON, así que una misma carpeta
        // puede traer los dos formatos.
        const huawei = files.filter((file) => !looksLikeGpx(file.content));
        const gpx = files.filter((file) => looksLikeGpx(file.content));
        const parsed = parseHuaweiExport(huawei.map((file) => file.content));
        const fromGpx = gpx.flatMap((file) => parseGpx(file.content, file.name));
        const workouts = dedupe(
          [...parsed.workouts, ...fromGpx].sort((a, b) => b.startedAtMs - a.startedAtMs)
        );
        const existing = new Set<string>();
        if (db) {
          for (const session of await listSessions(db)) existing.add(String(session.startedAt));
        }
        setFilesRead(parsed.filesRead + gpx.length);
        setFilesSkipped(parsed.filesSkipped);
        setWorkouts(workouts);
        setAlreadyImported(
          workouts.filter((workout) => existing.has(String(workout.startedAtMs))).length
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
          notes: importedWorkoutNote(workout),
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
    setFilesSkipped(0);
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
    filesSkipped,
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

/** Quita los entrenamientos repetidos por su clave natural. */
function dedupe(workouts: readonly ImportedWorkout[]): ImportedWorkout[] {
  const byKey = new Map<string, ImportedWorkout>();
  for (const workout of workouts) {
    const key = workoutKey(workout);
    if (!byKey.has(key)) byKey.set(key, workout);
  }
  return [...byKey.values()];
}
