import { describe, expect, it } from 'vitest';

import { applyMigrations, type SqlRunner } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';

import catalogSeed from '@shared/seed/catalog.json';

import { createNodeSqliteHarness } from './helpers/nodeSqlite';

function createSeededRunner(): { runner: SqlRunner; countAll: (sql: string) => number } {
  const harness = createNodeSqliteHarness();
  applyMigrations(harness.runner, migrations);

  return {
    runner: harness.runner,
    countAll: (sql: string) => {
      const rows = harness.db.prepare(sql).all();
      return Number(rows[0]?.total ?? 0);
    },
  };
}

describe('seedCatalog', () => {
  it('siembra un catalogo vacio y registra la siembra en app_setting', () => {
    const { runner, countAll } = createSeededRunner();

    const result = seedCatalog(runner, catalogSeed, () => 1234);

    expect(result.seeded).toBe(true);
    expect(countAll('SELECT COUNT(*) AS total FROM muscle_group')).toBe(
      catalogSeed.muscle_groups.length
    );
    expect(countAll('SELECT COUNT(*) AS total FROM exercise')).toBe(catalogSeed.exercises.length);
    expect(countAll("SELECT COUNT(*) AS total FROM app_setting WHERE key = 'catalog_seeded_at'")).toBe(1);
  });

  it('no duplica filas al sembrar dos veces', () => {
    const { runner, countAll } = createSeededRunner();

    seedCatalog(runner, catalogSeed, () => 1);
    const second = seedCatalog(runner, catalogSeed, () => 2);

    expect(second.seeded).toBe(false);
    expect(countAll('SELECT COUNT(*) AS total FROM muscle_group')).toBe(
      catalogSeed.muscle_groups.length
    );
    expect(countAll('SELECT COUNT(*) AS total FROM exercise')).toBe(catalogSeed.exercises.length);
    expect(countAll('SELECT COUNT(*) AS total FROM app_setting')).toBe(1);
  });

  it('no toca los datos existentes del usuario', () => {
    const { runner, countAll } = createSeededRunner();
    seedCatalog(runner, catalogSeed, () => 1);

    runner.exec(
      `INSERT INTO exercise (id, slug, name, muscle_group_id, secondary_muscle_group_id, equipment, kind, is_custom, created_at, updated_at, deleted_at)
       VALUES (?, ?, ?, ?, NULL, ?, ?, 1, ?, ?, NULL)`,
      [
        '01ARYZ6S41TSV4RRFFQ69G5FAV',
        'mi-ejercicio',
        'Mi ejercicio',
        catalogSeed.muscle_groups[0]?.id ?? '',
        'banda',
        'strength',
        10,
        10,
      ]
    );
    const before = countAll('SELECT COUNT(*) AS total FROM exercise');

    const result = seedCatalog(runner, catalogSeed, () => 99);

    expect(result.seeded).toBe(false);
    expect(countAll('SELECT COUNT(*) AS total FROM exercise')).toBe(before);
    const custom = runner.all("SELECT is_custom FROM exercise WHERE slug = 'mi-ejercicio'");
    expect(custom).toHaveLength(1);
    expect(custom[0]?.[0]).toBe(1);
  });
});
