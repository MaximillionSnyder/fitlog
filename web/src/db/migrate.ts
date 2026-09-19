import type { Migration } from '@/db/migrations';

export type SqlValue = string | number | null;

export interface SqlRunner {
  exec(sql: string, params?: readonly SqlValue[]): void;
  all(sql: string, params?: readonly SqlValue[]): SqlValue[][];
}

const SCHEMA_MIGRATION_TABLE = `
  CREATE TABLE IF NOT EXISTS schema_migration (
    version    INTEGER NOT NULL PRIMARY KEY,
    name       TEXT    NOT NULL,
    applied_at INTEGER NOT NULL
  )
`;

export function applyMigrations(
  runner: SqlRunner,
  migrations: readonly Migration[],
  now: () => number = Date.now
): number {
  runner.exec(SCHEMA_MIGRATION_TABLE);

  const appliedRows = runner.all('SELECT version FROM schema_migration');
  const applied = new Set(appliedRows.map((row) => Number(row[0])));

  const pending = [...migrations]
    .filter((migration) => !applied.has(migration.version))
    .sort((a, b) => a.version - b.version);

  let version = applied.size > 0 ? Math.max(...applied) : 0;

  for (const migration of pending) {
    runner.exec('BEGIN');
    try {
      runner.exec(migration.sql);
      runner.exec('INSERT INTO schema_migration (version, name, applied_at) VALUES (?, ?, ?)', [
        migration.version,
        migration.name,
        now(),
      ]);
      runner.exec('COMMIT');
    } catch (error) {
      runner.exec('ROLLBACK');
      throw error;
    }
    version = migration.version;
  }

  return version;
}
