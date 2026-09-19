import { useEffect } from 'react';

import { createFitLogDb, SqliteWorkerClient, supportsOpfs, type FitLogDb } from '@/db/client';
import { useDbStatusStore } from '@/state/dbStatus';

let cached: { client: SqliteWorkerClient; db: FitLogDb } | undefined;

export function useDatabase(): FitLogDb | undefined {
  const status = useDbStatusStore((state) => state.status);
  const setStatus = useDbStatusStore((state) => state.setStatus);

  useEffect(() => {
    if (cached) {
      return;
    }
    if (!supportsOpfs()) {
      setStatus({ state: 'sin-opfs' });
      return;
    }
    const client = new SqliteWorkerClient();
    cached = { client, db: createFitLogDb(client) };
    client.ready
      .then((info) => {
        setStatus({
          state: 'listo',
          schemaVersion: info.schemaVersion,
          sqliteVersion: info.sqliteVersion,
          persistence: info.persistence,
        });
      })
      .catch((error: unknown) => {
        setStatus({
          state: 'error',
          error: error instanceof Error ? error.message : String(error),
        });
      });
  }, [setStatus]);

  if (status.state !== 'listo' || !cached) {
    return undefined;
  }
  return cached.db;
}
