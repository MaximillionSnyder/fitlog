import { describe, expect, it } from 'vitest';

import { createFitLogDb } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';
import {
  addRoutineExercise,
  createRoutine,
  deleteRoutine,
  getRoutine,
  listRoutines,
  moveRoutineExercise,
  removeRoutineExercise,
  RoutineError,
  updateRoutine,
} from '@/data/routines';
import { getSessionDetail, listSessions, startSession } from '@/data/workout';

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
const exerciseC = first(catalogSeed.exercises.filter((item) => item.slug === 'sentadilla-barra'));

async function expectRoutineError(promise: Promise<unknown>, code: string): Promise<void> {
  await expect(promise).rejects.toBeInstanceOf(RoutineError);
  await promise.catch((error: unknown) => {
    expect((error as RoutineError).code).toBe(code);
  });
}

function targets(exerciseId: string, sets: number | null = 4, reps: number | null = 8) {
  return {
    exerciseId,
    targetSets: sets,
    targetReps: reps,
    targetWeightKg: null,
    restSeconds: null,
    notes: null,
  };
}

describe('gestion de rutinas', () => {
  it('crea, edita y lista rutinas', async () => {
    const db = createDatabase();
    const created = await createRoutine(
      db,
      { name: '  Día de empuje ', description: 'Pecho, hombros y tríceps' },
      () => 1000
    );

    expect(created.name).toBe('Día de empuje');
    expect(created.description).toBe('Pecho, hombros y tríceps');

    await updateRoutine(db, created.id, { name: 'Empuje A', description: null }, () => 2000);

    const routines = await listRoutines(db);
    expect(routines).toHaveLength(1);
    expect(routines[0]?.name).toBe('Empuje A');
    expect(routines[0]?.description).toBeNull();
  });

  it('rechaza nombres vacios', async () => {
    const db = createDatabase();
    await expectRoutineError(
      createRoutine(db, { name: '   ', description: null }),
      'invalid_input'
    );

    const routine = await createRoutine(db, { name: 'Valida', description: null });
    await expectRoutineError(
      updateRoutine(db, routine.id, { name: '', description: null }),
      'invalid_input'
    );
  });

  it('elimina la rutina con borrado logico sin tocar el historial', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Pierna', description: null });
    await addRoutineExercise(db, routine.id, targets(exerciseC.id));

    const session = await startSession(db, routine.id, () => 5000);
    await deleteRoutine(db, routine.id, () => 6000);

    expect(await listRoutines(db)).toHaveLength(0);

    const history = await listSessions(db);
    expect(history).toHaveLength(1);
    expect(history[0]?.id).toBe(session.id);

    const detail = await getSessionDetail(db, session.id);
    expect(detail.session.routineId).toBe(routine.id);
    expect(detail.session.routineName).toBeNull();
  });
});

describe('ejercicios de la rutina', () => {
  it('agrega ejercicios con posiciones consecutivas y objetivos', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Empuje', description: null });

    const firstExercise = await addRoutineExercise(db, routine.id, targets(exerciseA.id, 4, 8));
    const secondExercise = await addRoutineExercise(db, routine.id, targets(exerciseB.id, 3, 10));

    expect(firstExercise.position).toBe(1);
    expect(secondExercise.position).toBe(2);
    expect(firstExercise.targetSets).toBe(4);
    expect(firstExercise.targetReps).toBe(8);

    const loaded = await getRoutine(db, routine.id);
    expect(loaded.exercises.map((item) => item.exerciseName)).toEqual([
      'Press banca con barra',
      'Remo con barra',
    ]);
  });

  it('rechaza ejercicios duplicados, inexistentes y rutinas inexistentes', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Empuje', description: null });
    await addRoutineExercise(db, routine.id, targets(exerciseA.id));

    await expectRoutineError(
      addRoutineExercise(db, routine.id, targets(exerciseA.id)),
      'exercise_already_in_routine'
    );
    await expectRoutineError(
      addRoutineExercise(db, routine.id, targets('01ARYZ6S41TSV4RRFFQ69G5FAV')),
      'exercise_not_found'
    );
    await expectRoutineError(
      addRoutineExercise(db, '01ARYZ6S41TSV4RRFFQ69G5FAV', targets(exerciseB.id)),
      'routine_not_found'
    );
  });

  it('reordena ejercicios y renumera posiciones', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Full body', description: null });
    const a = await addRoutineExercise(db, routine.id, targets(exerciseA.id));
    const b = await addRoutineExercise(db, routine.id, targets(exerciseB.id));
    const c = await addRoutineExercise(db, routine.id, targets(exerciseC.id));

    await moveRoutineExercise(db, c.id, 'up');
    let loaded = await getRoutine(db, routine.id);
    expect(loaded.exercises.map((item) => item.id)).toEqual([a.id, c.id, b.id]);
    expect(loaded.exercises.map((item) => item.position)).toEqual([1, 2, 3]);

    await moveRoutineExercise(db, c.id, 'up');
    loaded = await getRoutine(db, routine.id);
    expect(loaded.exercises.map((item) => item.id)).toEqual([c.id, a.id, b.id]);

    await moveRoutineExercise(db, a.id, 'down');
    loaded = await getRoutine(db, routine.id);
    expect(loaded.exercises.map((item) => item.id)).toEqual([c.id, b.id, a.id]);
  });

  it('rechaza movimientos fuera de rango', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Full body', description: null });
    const a = await addRoutineExercise(db, routine.id, targets(exerciseA.id));
    await addRoutineExercise(db, routine.id, targets(exerciseB.id));

    await expectRoutineError(moveRoutineExercise(db, a.id, 'up'), 'invalid_move');

    const loaded = await getRoutine(db, routine.id);
    expect(loaded.exercises.map((item) => item.position)).toEqual([1, 2]);
  });

  it('quita un ejercicio y renumera los restantes', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Full body', description: null });
    const a = await addRoutineExercise(db, routine.id, targets(exerciseA.id));
    const b = await addRoutineExercise(db, routine.id, targets(exerciseB.id));
    const c = await addRoutineExercise(db, routine.id, targets(exerciseC.id));

    await removeRoutineExercise(db, b.id, () => 3000);

    const loaded = await getRoutine(db, routine.id);
    expect(loaded.exercises.map((item) => item.id)).toEqual([a.id, c.id]);
    expect(loaded.exercises.map((item) => item.position)).toEqual([1, 2]);
  });
});

describe('entrenar desde una rutina', () => {
  it('crea la sesion con la rutina de origen y la muestra en el detalle', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Día de empuje', description: null });
    await addRoutineExercise(db, routine.id, targets(exerciseA.id));

    const session = await startSession(db, routine.id, () => 5000);
    expect(session.routineId).toBe(routine.id);
    expect(session.routineName).toBe('Día de empuje');

    const detail = await getSessionDetail(db, session.id);
    expect(detail.session.routineName).toBe('Día de empuje');
  });

  it('rechaza rutinas inexistentes sin crear sesion', async () => {
    const db = createDatabase();
    await expect(startSession(db, '01ARYZ6S41TSV4RRFFQ69G5FAV')).rejects.toBeInstanceOf(Error);
    expect(await listSessions(db)).toHaveLength(0);
  });

  it('mantiene una sola sesion activa', async () => {
    const db = createDatabase();
    const routine = await createRoutine(db, { name: 'Empuje', description: null });
    await startSession(db, routine.id, () => 1000);

    await expect(startSession(db, routine.id, () => 2000)).rejects.toMatchObject({
      code: 'session_already_active',
    });
  });
});
