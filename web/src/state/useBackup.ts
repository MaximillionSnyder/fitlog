import { useCallback, useState } from 'react';

import type { FitLogDb } from '@/db/client';
import { exportBackup, importBackup } from '@/data/backup';
import { UI_GROUPS, type MergeSummary, type TableName } from '@/domain/backup';

export interface BackupState {
  readonly sections: string[];
  readonly busy: boolean;
  readonly error: string | null;
  readonly summary: MergeSummary | null;
  toggleSection(groupId: string): void;
  exportNow(): Promise<void>;
  importJson(json: string): Promise<void>;
  clearSummary(): void;
}

const APP_VERSION = '0.1.7';

function tablesFor(groups: readonly string[]): TableName[] {
  const tables: TableName[] = [];
  for (const group of UI_GROUPS) {
    if (groups.includes(group.id)) {
      tables.push(...group.tables);
    }
  }
  return tables;
}

export function useBackup(db: FitLogDb | undefined): BackupState {
  const [sections, setSections] = useState<string[]>(['custom_exercises', 'routines', 'workouts', 'body_metrics']);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<MergeSummary | null>(null);

  const toggleSection = useCallback((groupId: string) => {
    setSections((current) =>
      current.includes(groupId) ? current.filter((item) => item !== groupId) : [...current, groupId]
    );
  }, []);

  const exportNow = useCallback(async () => {
    if (!db) return;
    setBusy(true);
    setError(null);
    try {
      const tables = tablesFor(sections);
      if (tables.length === 0) {
        setError('Elegí al menos una sección para exportar');
        return;
      }
      const json = await exportBackup(db, tables, APP_VERSION);
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `fitlog-backup-${new Date().toISOString().slice(0, 10)}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setBusy(false);
    }
  }, [db, sections]);

  const importJson = useCallback(
    async (json: string) => {
      if (!db) return;
      setBusy(true);
      setError(null);
      setSummary(null);
      try {
        setSummary(await importBackup(db, json));
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : String(cause));
      } finally {
        setBusy(false);
      }
    },
    [db]
  );

  const clearSummary = useCallback(() => setSummary(null), []);

  return { sections, busy, error, summary, toggleSection, exportNow, importJson, clearSummary };
}
