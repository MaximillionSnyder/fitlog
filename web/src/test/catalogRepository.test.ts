import { describe, expect, it } from 'vitest';

import { createFitLogDb, type SqlClient } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import {
  CatalogError,
  createCustomExercise,
  deleteCustomExercise,
  loadCatalog,
} from '@/data/catalog';
import { seedCatalog } from '@/data/catalogSeed';

import catalogSeed from '@shared/seed/catalog.json';

import { createNodeSqliteHarness } from './helpers/nodeSqlite';

function first<T>(items: readonly T[]): T {
  const value = items[0];
  if (value === undefined) throw new Error('lista vacia');
  return value;
}

function createDatabase(): { db: ReturnType<typeof createFitLogDb>; client: SqlClient } {
  const harness = createNodeSqliteHarness();
  applyMigrations(harness.runner, migrations);
  seedCatalog(harness.runner, catalogSeed, () => 1);
  return { db: createFitLogDb(harness.client), client: harness.client };
}

describe('loadCatalog', () => {
  it('devuelve los grupos y ejercicios sembrados', async () => {
    const { db } = createDatabase();
    const snapshot = await loadCatalog(db);

    expect(snapshot.groups.length).toBe(catalogSeed.muscle_groups.length);
    expect(snapshot.exercises.length).toBe(catalogSeed.exercises.length);
    expect(snapshot.exercises.every((exercise) => exercise.isCustom === false)).toBe(true);
  });
});

describe('createCustomExercise', () => {
  it('crea un ejercicio propio con slug derivado y ULID', async () => {
    const { db } = createDatabase();
    const group = first(catalogSeed.muscle_groups);

    const created = await createCustomExercise(
      db,
      { name: '  Remo en máquina ', muscleGroupId: group.id, equipment: 'maquina', kind: 'strength' },
      () => 1700000000000
    );

    expect(created.slug).toBe('remo-en-maquina');
    expect(created.name).toBe('Remo en máquina');
    expect(created.isCustom).toBe(true);
    expect(created.id).toHaveLength(26);

    const snapshot = await loadCatalog(db);
    expect(snapshot.exercises.filter((exercise) => exercise.isCustom)).toHaveLength(1);
  });

  it('rechaza nombres vacios', async () => {
    const { db } = createDatabase();
    await expect(
      createCustomExercise(db, {
        name: '   ',
        muscleGroupId: first(catalogSeed.muscle_groups).id,
        equipment: 'barra',
        kind: 'strength',
      })
    ).rejects.toMatchObject({ code: 'invalid_input' });
  });

  it('rechaza grupos musculares inexistentes', async () => {
    const { db } = createDatabase();
    await expect(
      createCustomExercise(db, {
        name: 'Ejercicio raro',
        muscleGroupId: '01ARYZ6S41TSV4RRFFQ69G5FAV',
        equipment: 'barra',
        kind: 'strength',
      })
    ).rejects.toMatchObject({ code: 'muscle_group_not_found' });
  });

  it('rechaza slugs duplicados contra el catalogo base', async () => {
    const { db } = createDatabase();
    const base = first(catalogSeed.exercises.filter((item) => item.slug === 'dominadas'));
    await expect(
      createCustomExercise(db, {
        name: base.name.toUpperCase(),
        muscleGroupId: base.muscle_group_id,
        equipment: 'peso-corporal',
        kind: 'strength',
      })
    ).rejects.toBeInstanceOf(CatalogError);
  });
});

describe('deleteCustomExercise', () => {
  it('aplica borrado logico a ejercicios propios', async () => {
    const { db } = createDatabase();
    const created = await createCustomExercise(
      db,
      {
        name: 'Ejercicio temporal',
        muscleGroupId: first(catalogSeed.muscle_groups).id,
        equipment: 'banda',
        kind: 'strength',
      },
      () => 1700000000000
    );

    await deleteCustomExercise(db, created.id, () => 1700000001000);

    const snapshot = await loadCatalog(db);
    expect(snapshot.exercises.some((exercise) => exercise.id === created.id)).toBe(false);
  });

  it('protege el catalogo base', async () => {
    const { db } = createDatabase();
    await expect(deleteCustomExercise(db, first(catalogSeed.exercises).id)).rejects.toMatchObject({
      code: 'base_catalog_protected',
    });
  });

  it('falla si el ejercicio no existe', async () => {
    const { db } = createDatabase();
    await expect(
      deleteCustomExercise(db, '01ARYZ6S41TSV4RRFFQ69G5FAV')
    ).rejects.toMatchObject({ code: 'exercise_not_found' });
  });
});
