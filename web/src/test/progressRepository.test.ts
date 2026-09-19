import { describe, expect, it } from 'vitest';

import { createFitLogDb } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';
import { loadProgressSeries } from '@/data/progress';
import { addSet, deleteSet, finishSession, startSession } from '@/data/workout';
import { rangeFor } from '@/domain/progress';

import catalogSeed from '@shared/seed/catalog.json';

import { createNodeSqliteHarness } from './helpers/nodeSqlite';

type Db = ReturnType<typeof createFitLogDb>;

function first<T>(items: readonly T[]): T {
  const value = items[0];
  if (value === undefined) throw new Error('lista vacia');
  return value;
}

function createDatabase(): Db {
  const harness = createNodeSqliteHarness();
  applyMigrations(harness.runner, migrations);
  seedCatalog(harness.runner, catalogSeed, () => 1);
  return createFitLogDb(harness.client);
}

const exercise = first(catalogSeed.exercises.filter((item) => item.slug === 'press-banca-barra'));
const otherExercise = first(catalogSeed.exercises.filter((item) => item.slug === 'remo-barra'));

async function registerSession(
  db: Db,
  startedAt: number,
  sets: { weightKg: number | null; reps: number | null; isWarmup?: boolean; exerciseId?: string }[]
): Promise<string> {
  const session = await startSession(db, null, () => startedAt);
  for (const set of sets) {
    await addSet(
      db,
      {
        sessionId: session.id,
        exerciseId: set.exerciseId ?? exercise.id,
        weightKg: set.weightKg,
        reps: set.reps,
        rir: null,
        notes: null,
        isWarmup: set.isWarmup ?? false,
      },
      () => startedAt
    );
  }
  await finishSession(db, session.id, () => startedAt + 60_000);
  return session.id;
}

describe('loadProgressSeries', () => {
  it('construye un punto por sesion con las metricas del ejercicio', async () => {
    const db = createDatabase();
    const older = await registerSession(db, 1_000, [
      { weightKg: 100, reps: 8 },
      { weightKg: 40, reps: 10, isWarmup: true },
      { weightKg: 60, reps: 10, exerciseId: otherExercise.id },
    ]);
    const newer = await registerSession(db, 2_000, [{ weightKg: 105, reps: 5 }]);

    const points = await loadProgressSeries(db, exercise.id, null);

    expect(points.map((point) => point.sessionId)).toEqual([older, newer]);
    expect(points[0]?.workingSets).toBe(1);
    expect(points[0]?.maxWeightKg).toBe(100);
    expect(points[0]?.volumeKg).toBe(800);
    expect(points[0]?.bestOneRepMaxKg).toBeCloseTo(126.7, 9);
    expect(points[1]?.maxWeightKg).toBe(105);
    expect(points[1]?.bestOneRepMaxKg).toBeCloseTo(122.5, 9);
  });

  it('excluye series eliminadas', async () => {
    const db = createDatabase();
    const session = await startSession(db, null, () => 1_000);
    const kept = await addSet(db, {
      sessionId: session.id,
      exerciseId: exercise.id,
      weightKg: 100,
      reps: 8,
      rir: null,
      notes: null,
      isWarmup: false,
    });
    const removed = await addSet(db, {
      sessionId: session.id,
      exerciseId: exercise.id,
      weightKg: 120,
      reps: 1,
      rir: null,
      notes: null,
      isWarmup: false,
    });

    let points = await loadProgressSeries(db, exercise.id, null);
    expect(points[0]?.maxWeightKg).toBe(120);

    await deleteSet(db, removed.id, () => 2_000);
    points = await loadProgressSeries(db, exercise.id, null);
    expect(points[0]?.maxWeightKg).toBe(100);
    expect(points[0]?.workingSets).toBe(1);
    expect(kept.setIndex).toBe(1);
  });

  it('filtra por rango temporal', async () => {
    const db = createDatabase();
    await registerSession(db, 1_000, [{ weightKg: 100, reps: 8 }]);
    const recent = await registerSession(db, 2_000, [{ weightKg: 105, reps: 5 }]);

    const filtered = await loadProgressSeries(db, exercise.id, { fromMs: 1_500, toMs: 3_000 });
    expect(filtered.map((point) => point.sessionId)).toEqual([recent]);

    const all = await loadProgressSeries(db, exercise.id, rangeFor('all', 10_000));
    expect(all).toHaveLength(2);
  });

  it('devuelve lista vacia para un ejercicio sin series', async () => {
    const db = createDatabase();
    await registerSession(db, 1_000, [{ weightKg: 100, reps: 8 }]);

    const points = await loadProgressSeries(db, otherExercise.id, null);
    expect(points).toEqual([]);
  });
});
