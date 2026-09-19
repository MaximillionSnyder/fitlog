import { describe, expect, it } from 'vitest';

import { createFitLogDb } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';
import { addRoutineExercise, createRoutine, listRoutines } from '@/data/routines';
import { addSet, startSession } from '@/data/workout';
import { createBodyMetric, listBodyMetrics } from '@/data/body';
import { exportBackup, importBackup, readSnapshot } from '@/data/backup';
import { TABLES } from '@/domain/backup';

import catalogSeed from '@shared/seed/catalog.json';

import { createNodeSqliteHarness, type NodeSqliteHarness } from './helpers/nodeSqlite';

type Db = ReturnType<typeof createFitLogDb>;

function first<T>(items: readonly T[]): T {
  const value = items[0];
  if (value === undefined) throw new Error('lista vacia');
  return value;
}

function createDatabase(): { db: Db; harness: NodeSqliteHarness } {
  const harness = createNodeSqliteHarness();
  applyMigrations(harness.runner, migrations);
  seedCatalog(harness.runner, catalogSeed, () => 1);
  return { db: createFitLogDb(harness.client), harness };
}

const pressBanca = first(catalogSeed.exercises.filter((item) => item.slug === 'press-banca-barra'));

async function seedWorkout(db: Db, now = () => 1000): Promise<void> {
  const session = await startSession(db, null, now);
  await addSet(
    db,
    {
      sessionId: session.id,
      exerciseId: pressBanca.id,
      weightKg: 100,
      reps: 8,
      rir: null,
      notes: null,
      isWarmup: false,
    },
    now
  );
}

describe('exportBackup', () => {
  it('exporta solo las tablas pedidas y auto-incluye ejercicios propios referenciados', async () => {
    const { db } = createDatabase();
    const routine = await createRoutine(db, { name: 'Empuje', description: null }, () => 1000);
    await addRoutineExercise(
      db,
      routine.id,
      {
        exerciseId: pressBanca.id,
        targetSets: 4,
        targetReps: 8,
        targetWeightKg: null,
        restSeconds: null,
        notes: null,
      },
      () => 1000
    );
    const created = await createBodyMetric(
      db,
      { kind: 'body_weight', value: 80, measuredAtMs: 1000, notes: null },
      () => 1000
    );
    void created;
    await seedWorkout(db);

    const routinesJson = await exportBackup(db, ['routine', 'routine_exercise'], '0.1.7', () => 5000);
    const routinesFile = JSON.parse(routinesJson) as { sections: Record<string, unknown[]> };
    expect(Object.keys(routinesFile.sections).sort()).toEqual(['routine', 'routine_exercise']);

    const metricsJson = await exportBackup(db, ['body_metric'], '0.1.7', () => 5000);
    const metricsFile = JSON.parse(metricsJson) as { sections: Record<string, unknown[]> };
    expect(Object.keys(metricsFile.sections)).toEqual(['body_metric']);
    expect(metricsFile.sections.body_metric).toHaveLength(1);
    expect(routine.exercises).toHaveLength(0);
  });

  it('incluye el ejercicio propio referenciado por una serie', async () => {
    const { db } = createDatabase();
    const custom = await db.insert((await import('@/db/schema')).exercise).values({
      id: 'ex-propio',
      slug: 'remo-maquina',
      name: 'Remo en máquina',
      muscleGroupId: first(catalogSeed.muscle_groups).id,
      secondaryMuscleGroupId: null,
      equipment: 'maquina',
      kind: 'strength',
      isCustom: 1,
      createdAt: 1,
      updatedAt: 1,
      deletedAt: null,
    });
    void custom;

    const session = await startSession(db, null, () => 1000);
    await addSet(
      db,
      {
        sessionId: session.id,
        exerciseId: 'ex-propio',
        weightKg: 50,
        reps: 10,
        rir: null,
        notes: null,
        isWarmup: false,
      },
      () => 1000
    );

    const json = await exportBackup(db, ['session', 'set_entry'], '0.1.7', () => 5000);
    const file = JSON.parse(json) as { sections: Record<string, { id: string }[]> };

    expect(file.sections.exercise?.map((row) => row.id)).toEqual(['ex-propio']);
  });
});

describe('importBackup', () => {
  it('fusiona un respaldo completo en una base vacia', async () => {
    const source = createDatabase();
    await createRoutine(source.db, { name: 'Empuje', description: null }, () => 1000);
    await createBodyMetric(
      source.db,
      { kind: 'body_weight', value: 80, measuredAtMs: 1000, notes: null },
      () => 1000
    );
    await seedWorkout(source.db);

    const json = await exportBackup(source.db, TABLES, '0.1.7', () => 5000);

    const target = createDatabase();
    const summary = await importBackup(target.db, json);

    expect(summary.routine.inserted).toBe(1);
    expect(summary.body_metric.inserted).toBe(1);
    expect(summary.session.inserted).toBe(1);
    expect(summary.set_entry.inserted).toBe(1);
    expect((await listRoutines(target.db)).map((item) => item.name)).toEqual(['Empuje']);
    expect(await listBodyMetrics(target.db)).toHaveLength(1);
  });

  it('no duplica ni pierde filas al reimportar el mismo respaldo', async () => {
    const { db } = createDatabase();
    await createRoutine(db, { name: 'Empuje', description: null }, () => 1000);
    await createBodyMetric(
      db,
      { kind: 'body_weight', value: 80, measuredAtMs: 1000, notes: null },
      () => 1000
    );

    const json = await exportBackup(db, TABLES, '0.1.7', () => 5000);
    const summary = await importBackup(db, json);

    expect(summary.routine.inserted).toBe(0);
    expect(summary.routine.updated).toBe(0);
    expect(summary.routine.ignored).toBe(1);
    expect((await listRoutines(db)).length).toBe(1);
  });

  it('remapea ejercicios propios con el mismo slug y reescribe las series', async () => {
    const { db } = createDatabase();
    const schema = await import('@/db/schema');
    const groupId = first(catalogSeed.muscle_groups).id;

    await db.insert(schema.exercise).values({
      id: 'ex-local',
      slug: 'remo-maquina',
      name: 'Remo en máquina',
      muscleGroupId: groupId,
      secondaryMuscleGroupId: null,
      equipment: 'maquina',
      kind: 'strength',
      isCustom: 1,
      createdAt: 1,
      updatedAt: 500,
      deletedAt: null,
    });

    const backup = {
      format: 'fitlog-backup',
      format_version: 1,
      exported_at_ms: 5000,
      app_version: '0.1.7',
      sections: {
        exercise: [
          {
            id: 'ex-remoto',
            slug: 'remo-maquina',
            name: 'Remo en máquina',
            muscle_group_id: groupId,
            is_custom: 1,
            updated_at: 100,
            deleted_at: null,
          },
        ],
        session: [{ id: 's-remota', started_at: 1000, updated_at: 100, deleted_at: null }],
        set_entry: [
          {
            id: 'set-remoto',
            session_id: 's-remota',
            exercise_id: 'ex-remoto',
            set_index: 1,
            weight_kg: 50,
            reps: 10,
            updated_at: 100,
            deleted_at: null,
          },
        ],
      },
    };

    const summary = await importBackup(db, JSON.stringify(backup));

    expect(summary.exercise.remapped).toBe(1);
    expect(summary.exercise.inserted).toBe(0);
    expect(summary.set_entry.inserted).toBe(1);

    const snapshot = await readSnapshot(db, ['exercise', 'set_entry']);
    expect(snapshot.exercise).toHaveLength(1);
    expect(snapshot.set_entry[0]?.exercise_id).toBe('ex-local');
  });

  it('aborta sin efectos cuando falta una referencia', async () => {
    const { db } = createDatabase();
    await createRoutine(db, { name: 'Empuje', description: null }, () => 1000);

    const backup = {
      format: 'fitlog-backup',
      format_version: 1,
      exported_at_ms: 5000,
      app_version: '0.1.7',
      sections: {
        set_entry: [
          {
            id: 'set-huerfano',
            session_id: 's-fantasma',
            exercise_id: pressBanca.id,
            set_index: 1,
            updated_at: 100,
            deleted_at: null,
          },
        ],
      },
    };

    await expect(importBackup(db, JSON.stringify(backup))).rejects.toMatchObject({
      code: 'missing_reference',
    });

    const snapshot = await readSnapshot(db, ['set_entry']);
    expect(snapshot.set_entry).toHaveLength(0);
    expect((await listRoutines(db)).length).toBe(1);
  });

  it('fusiona rutinas con ejercicios sin romper las posiciones', async () => {
    const source = createDatabase();
    const routine = await createRoutine(source.db, { name: 'Full body', description: null }, () => 1000);
    await addRoutineExercise(
      source.db,
      routine.id,
      {
        exerciseId: pressBanca.id,
        targetSets: 4,
        targetReps: 8,
        targetWeightKg: null,
        restSeconds: null,
        notes: null,
      },
      () => 1000
    );

    const json = await exportBackup(source.db, TABLES, '0.1.7', () => 5000);

    const target = createDatabase();
    const localRoutine = await createRoutine(target.db, { name: 'Full body', description: null }, () => 1000);
    await addRoutineExercise(
      target.db,
      localRoutine.id,
      {
        exerciseId: first(catalogSeed.exercises.filter((item) => item.slug === 'remo-barra')).id,
        targetSets: 3,
        targetReps: 10,
        targetWeightKg: null,
        restSeconds: null,
        notes: null,
      },
      () => 1000
    );

    await importBackup(target.db, json);

    const routines = await listRoutines(target.db);
    expect(routines).toHaveLength(2);
    for (const item of routines) {
      const positions = item.exercises.map((exercise) => exercise.position);
      expect(positions).toEqual(positions.map((_, index) => index + 1));
    }
  });
});
