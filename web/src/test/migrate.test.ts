import { DatabaseSync } from 'node:sqlite';
import { describe, expect, it } from 'vitest';

import { applyMigrations, type SqlRunner, type SqlValue } from '@/db/migrate';
import { migrations, type Migration } from '@/db/migrations';

function createRunner(): { runner: SqlRunner; db: DatabaseSync } {
  const db = new DatabaseSync(':memory:');
  const runner: SqlRunner = {
    exec(sql, params) {
      if (params && params.length > 0) {
        db.prepare(sql).run(...params);
        return;
      }
      db.exec(sql);
    },
    all(sql, params) {
      const statement = db.prepare(sql);
      const rows = params && params.length > 0 ? statement.all(...params) : statement.all();
      return rows.map((row) => Object.values(row) as SqlValue[]);
    },
  };
  return { runner, db };
}

function tableNames(db: DatabaseSync): string[] {
  const rows = db
    .prepare("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
    .all();
  return rows.map((row) => String(row.name));
}

function migrationCount(db: DatabaseSync): number {
  const rows = db.prepare('SELECT COUNT(*) AS total FROM schema_migration').all();
  return Number(rows[0]?.total ?? 0);
}

describe('applyMigrations con el esquema real', () => {
  it('aplica todas las migraciones en una base nueva y registra la version', () => {
    const { runner, db } = createRunner();
    const version = applyMigrations(runner, migrations);

    expect(version).toBe(1);
    expect(migrationCount(db)).toBe(1);
    expect(tableNames(db)).toEqual([
      'app_setting',
      'body_metric',
      'exercise',
      'muscle_group',
      'routine',
      'routine_exercise',
      'schema_migration',
      'session',
      'set_entry',
    ]);
  });

  it('no reaplica migraciones en una base existente', () => {
    const { runner, db } = createRunner();
    applyMigrations(runner, migrations);
    const version = applyMigrations(runner, migrations);

    expect(version).toBe(1);
    expect(migrationCount(db)).toBe(1);
  });
});

describe('applyMigrations incremental', () => {
  const base: Migration[] = [
    { version: 1, name: '001_base', sql: 'CREATE TABLE uno (id TEXT PRIMARY KEY)' },
  ];
  const extended: Migration[] = [
    ...base,
    { version: 2, name: '002_extra', sql: 'CREATE TABLE dos (id TEXT PRIMARY KEY)' },
  ];

  it('aplica solo las pendientes y devuelve la ultima version', () => {
    const { runner, db } = createRunner();
    expect(applyMigrations(runner, base)).toBe(1);
    expect(applyMigrations(runner, extended)).toBe(2);
    expect(migrationCount(db)).toBe(2);
    expect(tableNames(db)).toContain('dos');
  });

  it('revierte la migracion completa si falla', () => {
    const { runner, db } = createRunner();
    const broken: Migration[] = [
      { version: 1, name: '001_base', sql: 'CREATE TABLE uno (id TEXT PRIMARY KEY)' },
      {
        version: 2,
        name: '002_rota',
        sql: 'CREATE TABLE dos (id TEXT PRIMARY KEY); ESTO NO ES SQL;',
      },
    ];
    expect(() => applyMigrations(runner, broken)).toThrow();
    expect(tableNames(db)).not.toContain('dos');
    expect(migrationCount(db)).toBe(1);
  });
});
