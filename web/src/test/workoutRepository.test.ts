import { describe, expect, it } from 'vitest';

import { createFitLogDb } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';
import {
  addSet,
  deleteSet,
  finishSession,
  getActiveSession,
  getSessionDetail,
  listSessions,
  startSession,
  updateSet,
  WorkoutError,
} from '@/data/workout';

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

const exerciseA = first(catalogSeed.exercises.filter((item) => item.slug === 'press-banca-barra'));
const exerciseB = first(catalogSeed.exercises.filter((item) => item.slug === 'remo-barra'));

async function expectWorkoutError(promise: Promise<unknown>, code: string): Promise<void> {
  await expect(promise).rejects.toBeInstanceOf(WorkoutError);
  await promise.catch((error: unknown) => {
    expect((error as WorkoutError).code).toBe(code);
  });
}

describe('sesiones', () => {
  it('inicia y finaliza una sesion', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);

    expect(session.startedAt).toBe(1000);
    expect(session.finishedAt).toBeNull();

    const active = await getActiveSession(db);
    expect(active?.id).toBe(session.id);

    await finishSession(db, session.id, () => 2000);
    expect(await getActiveSession(db)).toBeNull();

    const history = await listSessions(db);
    expect(history).toHaveLength(1);
    expect(history[0]?.finishedAt).toBe(2000);
  });

  it('rechaza una segunda sesion activa', async () => {
    const db = createDatabase();
    await startSession(db, () => 1000);
    await expectWorkoutError(startSession(db, () => 2000), 'session_already_active');
  });

  it('rechaza finalizar una sesion inexistente o ya finalizada', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);
    await finishSession(db, session.id, () => 2000);

    await expectWorkoutError(finishSession(db, session.id, () => 3000), 'session_not_active');
    await expectWorkoutError(
      finishSession(db, '01ARYZ6S41TSV4RRFFQ69G5FAV', () => 3000),
      'session_not_found'
    );
  });
});

describe('series', () => {
  it('registra series con indice automatico por ejercicio', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);

    const first = await addSet(
      db,
      {
        sessionId: session.id,
        exerciseId: exerciseA.id,
        weightKg: 100,
        reps: 8,
        rir: 2,
        notes: null,
        isWarmup: false,
      },
      () => 2000
    );
    const second = await addSet(
      db,
      {
        sessionId: session.id,
        exerciseId: exerciseA.id,
        weightKg: 100,
        reps: 6,
        rir: null,
        notes: 'buena',
        isWarmup: false,
      },
      () => 3000
    );
    const otherExercise = await addSet(
      db,
      {
        sessionId: session.id,
        exerciseId: exerciseB.id,
        weightKg: 60,
        reps: 10,
        rir: null,
        notes: null,
        isWarmup: false,
      },
      () => 4000
    );

    expect(first.setIndex).toBe(1);
    expect(second.setIndex).toBe(2);
    expect(otherExercise.setIndex).toBe(1);
  });

  it('valida peso, repeticiones y ejercicio', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);

    await expectWorkoutError(
      addSet(db, {
        sessionId: session.id,
        exerciseId: exerciseA.id,
        weightKg: -5,
        reps: 8,
        rir: null,
        notes: null,
        isWarmup: false,
      }),
      'invalid_input'
    );

    await expectWorkoutError(
      addSet(db, {
        sessionId: session.id,
        exerciseId: exerciseA.id,
        weightKg: 100,
        reps: -1,
        rir: null,
        notes: null,
        isWarmup: false,
      }),
      'invalid_input'
    );

    await expectWorkoutError(
      addSet(db, {
        sessionId: session.id,
        exerciseId: '01ARYZ6S41TSV4RRFFQ69G5FAV',
        weightKg: 100,
        reps: 8,
        rir: null,
        notes: null,
        isWarmup: false,
      }),
      'exercise_not_found'
    );
  });

  it('rechaza series en una sesion finalizada', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);
    await finishSession(db, session.id, () => 2000);

    await expectWorkoutError(
      addSet(db, {
        sessionId: session.id,
        exerciseId: exerciseA.id,
        weightKg: 100,
        reps: 8,
        rir: null,
        notes: null,
        isWarmup: false,
      }),
      'session_not_active'
    );
  });

  it('edita y elimina series con borrado logico', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);
    const set = await addSet(
      db,
      {
        sessionId: session.id,
        exerciseId: exerciseA.id,
        weightKg: 100,
        reps: 8,
        rir: 2,
        notes: null,
        isWarmup: false,
      },
      () => 2000
    );

    await updateSet(db, set.id, { weightKg: 102.5, reps: 8, rir: 1, notes: 'subida' }, () => 3000);

    let detail = await getSessionDetail(db, session.id);
    expect(detail.sets[0]?.weightKg).toBe(102.5);
    expect(detail.sets[0]?.rir).toBe(1);

    await deleteSet(db, set.id, () => 4000);
    detail = await getSessionDetail(db, session.id);
    expect(detail.sets).toHaveLength(0);
    expect(detail.session.summary.totalVolumeKg).toBe(0);
  });

  it('el borrado de una serie no cambia los indices de las demas', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);

    const created = [];
    for (const reps of [10, 8, 6]) {
      created.push(
        await addSet(
          db,
          {
            sessionId: session.id,
            exerciseId: exerciseA.id,
            weightKg: 80,
            reps,
            rir: null,
            notes: null,
            isWarmup: false,
          },
          () => 2000
        )
      );
    }

    await deleteSet(db, first(created.slice(1)).id, () => 3000);
    const detail = await getSessionDetail(db, session.id);

    expect(detail.sets.map((set) => set.setIndex)).toEqual([1, 3]);
  });
});

describe('historial', () => {
  it('lista sesiones con resumen y detalle agrupado por ejercicio', async () => {
    const db = createDatabase();
    const older = await startSession(db, () => 1000);
    await addSet(db, {
      sessionId: older.id,
      exerciseId: exerciseA.id,
      weightKg: 100,
      reps: 8,
      rir: null,
      notes: null,
      isWarmup: false,
    });
    await finishSession(db, older.id, () => 1000 + 45 * 60_000);

    const newer = await startSession(db, () => 5000);
    await addSet(db, {
      sessionId: newer.id,
      exerciseId: exerciseB.id,
      weightKg: 60,
      reps: 10,
      rir: null,
      notes: null,
      isWarmup: false,
    });
    await addSet(db, {
      sessionId: newer.id,
      exerciseId: exerciseB.id,
      weightKg: 40,
      reps: 10,
      rir: null,
      notes: null,
      isWarmup: true,
    });

    const history = await listSessions(db);
    expect(history.map((entry) => entry.id)).toEqual([newer.id, older.id]);
    expect(history[0]?.summary.workingSets).toBe(1);
    expect(history[0]?.summary.totalVolumeKg).toBe(600);
    expect(history[0]?.finishedAt).toBeNull();
    expect(history[1]?.summary.totalVolumeKg).toBe(800);

    const detail = await getSessionDetail(db, newer.id);
    expect(detail.sets).toHaveLength(2);
    expect(detail.sets[0]?.exerciseName).toBe('Remo con barra');
    expect(detail.sets[0]?.isWarmup).toBe(false);
    expect(detail.sets[1]?.isWarmup).toBe(true);
  });

  it('devuelve un detalle vacio para una sesion sin series', async () => {
    const db = createDatabase();
    const session = await startSession(db, () => 1000);
    await finishSession(db, session.id, () => 2000);

    const detail = await getSessionDetail(db, session.id);
    expect(detail.sets).toHaveLength(0);
    expect(detail.session.summary.totalVolumeKg).toBe(0);
  });
});
