import { inArray, isNull, sql } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import {
  appSetting,
  bodyMetric,
  exercise,
  routine,
  routineExercise,
  session,
  setEntry,
} from '@/db/schema';
import {
  emptySnapshot,
  mergeBackup,
  parseBackup,
  serializeBackup,
  TABLES,
  type BackupRow,
  type BackupSnapshot,
  type MergeSummary,
  type TableName,
} from '@/domain/backup';

const POSITION_OFFSET = 1_000_000;

function toRow(source: Record<string, unknown>): BackupRow {
  const row: BackupRow = {};
  for (const [key, value] of Object.entries(source)) {
    row[key] = value === undefined ? null : (value as BackupRow[string]);
  }
  return row;
}

export interface SnapshotOptions {
  readonly includeBaseExercises?: boolean;
}

async function readTable(
  db: FitLogDb,
  table: TableName,
  options: SnapshotOptions
): Promise<BackupRow[]> {
  switch (table) {
    case 'exercise': {
      const rows = await db.select().from(exercise).where(isNull(exercise.deletedAt));
      return rows
        .filter((row) => options.includeBaseExercises === true || row.isCustom === 1)
        .map((row) =>
          toRow({
            id: row.id,
            slug: row.slug,
            name: row.name,
            muscle_group_id: row.muscleGroupId,
            secondary_muscle_group_id: row.secondaryMuscleGroupId,
            equipment: row.equipment,
            kind: row.kind,
            is_custom: row.isCustom,
            created_at: row.createdAt,
            updated_at: row.updatedAt,
            deleted_at: row.deletedAt,
          })
        );
    }
    case 'routine': {
      const rows = await db.select().from(routine);
      return rows.map((row) =>
        toRow({
          id: row.id,
          name: row.name,
          description: row.description,
          created_at: row.createdAt,
          updated_at: row.updatedAt,
          deleted_at: row.deletedAt,
        })
      );
    }
    case 'routine_exercise': {
      const rows = await db.select().from(routineExercise);
      return rows.map((row) =>
        toRow({
          id: row.id,
          routine_id: row.routineId,
          exercise_id: row.exerciseId,
          position: row.position,
          target_sets: row.targetSets,
          target_reps: row.targetReps,
          target_weight_kg: row.targetWeightKg,
          rest_seconds: row.restSeconds,
          notes: row.notes,
          created_at: row.createdAt,
          updated_at: row.updatedAt,
          deleted_at: row.deletedAt,
        })
      );
    }
    case 'session': {
      const rows = await db.select().from(session);
      return rows.map((row) =>
        toRow({
          id: row.id,
          routine_id: row.routineId,
          started_at: row.startedAt,
          finished_at: row.finishedAt,
          notes: row.notes,
          created_at: row.createdAt,
          updated_at: row.updatedAt,
          deleted_at: row.deletedAt,
        })
      );
    }
    case 'set_entry': {
      const rows = await db.select().from(setEntry);
      return rows.map((row) =>
        toRow({
          id: row.id,
          session_id: row.sessionId,
          exercise_id: row.exerciseId,
          set_index: row.setIndex,
          weight_kg: row.weightKg,
          reps: row.reps,
          rir: row.rir,
          rpe: row.rpe,
          is_warmup: row.isWarmup,
          notes: row.notes,
          created_at: row.createdAt,
          updated_at: row.updatedAt,
          deleted_at: row.deletedAt,
        })
      );
    }
    case 'body_metric': {
      const rows = await db.select().from(bodyMetric);
      return rows.map((row) =>
        toRow({
          id: row.id,
          measured_at: row.measuredAt,
          kind: row.kind,
          value: row.value,
          unit: row.unit,
          notes: row.notes,
          created_at: row.createdAt,
          updated_at: row.updatedAt,
          deleted_at: row.deletedAt,
        })
      );
    }
    case 'app_setting': {
      const rows = await db.select().from(appSetting);
      return rows.map((row) =>
        toRow({
          key: row.key,
          value: row.value,
          updated_at: row.updatedAt,
        })
      );
    }
    default:
      return [];
  }
}

export async function readSnapshot(
  db: FitLogDb,
  tables: readonly TableName[],
  options: SnapshotOptions = {}
): Promise<BackupSnapshot> {
  const snapshot = emptySnapshot();
  for (const table of TABLES) {
    snapshot[table] = tables.includes(table) ? await readTable(db, table, options) : [];
  }
  return snapshot;
}

export async function exportBackup(
  db: FitLogDb,
  tables: readonly TableName[],
  appVersion: string,
  now: () => number = Date.now
): Promise<string> {
  // Se lee tambien la tabla de ejercicios para poder auto-incluir los propios referenciados
  const tablesToRead = tables.includes('exercise')
    ? tables
    : ([...tables, 'exercise'] as TableName[]);
  const snapshot = await readSnapshot(db, tablesToRead);
  return serializeBackup(snapshot, { sections: tables, appVersion, nowMs: now() });
}

function numberOrNull(value: BackupRow[string] | undefined): number | null {
  return typeof value === 'number' ? value : null;
}

function stringOrNull(value: BackupRow[string] | undefined): string | null {
  return typeof value === 'string' ? value : null;
}

export async function importBackup(db: FitLogDb, json: string): Promise<MergeSummary> {
  const file = parseBackup(json);
  const snapshot = await readSnapshot(db, TABLES, { includeBaseExercises: true });
  const merged = mergeBackup(snapshot, file);

  const touchedRoutines = new Set<string>();
  for (const row of merged.snapshot.routine_exercise) {
    touchedRoutines.add(String(row.routine_id));
  }

  await db.transaction(async (tx) => {
    if (touchedRoutines.size > 0) {
      await tx
        .update(routineExercise)
        .set({ position: sql`-1 * (${routineExercise.position} + 1)` })
        .where(inArray(routineExercise.routineId, [...touchedRoutines]));
    }

    for (const row of merged.snapshot.exercise) {
      await tx
        .insert(exercise)
        .values({
          id: String(row.id),
          slug: String(row.slug),
          name: String(row.name),
          muscleGroupId: String(row.muscle_group_id),
          secondaryMuscleGroupId: stringOrNull(row.secondary_muscle_group_id),
          equipment: String(row.equipment ?? ''),
          kind: String(row.kind ?? 'strength'),
          isCustom: Number(row.is_custom ?? 0),
          createdAt: Number(row.created_at ?? row.updated_at ?? 0),
          updatedAt: Number(row.updated_at ?? 0),
          deletedAt: numberOrNull(row.deleted_at),
        })
        .onConflictDoUpdate({
          target: exercise.id,
          set: {
            name: String(row.name),
            muscleGroupId: String(row.muscle_group_id),
            secondaryMuscleGroupId: stringOrNull(row.secondary_muscle_group_id),
            equipment: String(row.equipment ?? ''),
            kind: String(row.kind ?? 'strength'),
            updatedAt: Number(row.updated_at ?? 0),
            deletedAt: numberOrNull(row.deleted_at),
          },
        });
    }

    for (const row of merged.snapshot.routine) {
      await tx
        .insert(routine)
        .values({
          id: String(row.id),
          name: String(row.name),
          description: stringOrNull(row.description),
          createdAt: Number(row.created_at ?? row.updated_at ?? 0),
          updatedAt: Number(row.updated_at ?? 0),
          deletedAt: numberOrNull(row.deleted_at),
        })
        .onConflictDoUpdate({
          target: routine.id,
          set: {
            name: String(row.name),
            description: stringOrNull(row.description),
            updatedAt: Number(row.updated_at ?? 0),
            deletedAt: numberOrNull(row.deleted_at),
          },
        });
    }

    for (const row of merged.snapshot.session) {
      await tx
        .insert(session)
        .values({
          id: String(row.id),
          routineId: stringOrNull(row.routine_id),
          startedAt: Number(row.started_at ?? 0),
          finishedAt: numberOrNull(row.finished_at),
          notes: stringOrNull(row.notes),
          createdAt: Number(row.created_at ?? row.updated_at ?? 0),
          updatedAt: Number(row.updated_at ?? 0),
          deletedAt: numberOrNull(row.deleted_at),
        })
        .onConflictDoUpdate({
          target: session.id,
          set: {
            routineId: stringOrNull(row.routine_id),
            startedAt: Number(row.started_at ?? 0),
            finishedAt: numberOrNull(row.finished_at),
            notes: stringOrNull(row.notes),
            updatedAt: Number(row.updated_at ?? 0),
            deletedAt: numberOrNull(row.deleted_at),
          },
        });
    }

    for (const row of merged.snapshot.set_entry) {
      await tx
        .insert(setEntry)
        .values({
          id: String(row.id),
          sessionId: String(row.session_id),
          exerciseId: String(row.exercise_id),
          setIndex: Number(row.set_index ?? 0),
          weightKg: numberOrNull(row.weight_kg),
          reps: numberOrNull(row.reps),
          rir: numberOrNull(row.rir),
          rpe: numberOrNull(row.rpe),
          isWarmup: Number(row.is_warmup ?? 0),
          notes: stringOrNull(row.notes),
          createdAt: Number(row.created_at ?? row.updated_at ?? 0),
          updatedAt: Number(row.updated_at ?? 0),
          deletedAt: numberOrNull(row.deleted_at),
        })
        .onConflictDoUpdate({
          target: setEntry.id,
          set: {
            sessionId: String(row.session_id),
            exerciseId: String(row.exercise_id),
            setIndex: Number(row.set_index ?? 0),
            weightKg: numberOrNull(row.weight_kg),
            reps: numberOrNull(row.reps),
            rir: numberOrNull(row.rir),
            rpe: numberOrNull(row.rpe),
            isWarmup: Number(row.is_warmup ?? 0),
            notes: stringOrNull(row.notes),
            updatedAt: Number(row.updated_at ?? 0),
            deletedAt: numberOrNull(row.deleted_at),
          },
        });
    }

    for (const row of merged.snapshot.body_metric) {
      await tx
        .insert(bodyMetric)
        .values({
          id: String(row.id),
          measuredAt: Number(row.measured_at ?? 0),
          kind: String(row.kind ?? 'other'),
          value: Number(row.value ?? 0),
          unit: String(row.unit ?? 'cm'),
          notes: stringOrNull(row.notes),
          createdAt: Number(row.created_at ?? row.updated_at ?? 0),
          updatedAt: Number(row.updated_at ?? 0),
          deletedAt: numberOrNull(row.deleted_at),
        })
        .onConflictDoUpdate({
          target: bodyMetric.id,
          set: {
            measuredAt: Number(row.measured_at ?? 0),
            kind: String(row.kind ?? 'other'),
            value: Number(row.value ?? 0),
            unit: String(row.unit ?? 'cm'),
            notes: stringOrNull(row.notes),
            updatedAt: Number(row.updated_at ?? 0),
            deletedAt: numberOrNull(row.deleted_at),
          },
        });
    }

    for (const row of merged.snapshot.app_setting) {
      await tx
        .insert(appSetting)
        .values({
          key: String(row.key),
          value: String(row.value ?? ''),
          updatedAt: Number(row.updated_at ?? 0),
        })
        .onConflictDoUpdate({
          target: appSetting.key,
          set: {
            value: String(row.value ?? ''),
            updatedAt: Number(row.updated_at ?? 0),
          },
        });
    }

    for (const routineId of touchedRoutines) {
      const rows = merged.snapshot.routine_exercise.filter(
        (row) => String(row.routine_id) === routineId
      );
      const active = rows
        .filter((row) => row.deleted_at === null || row.deleted_at === undefined)
        .sort((a, b) => {
          const byPosition = Number(a.position ?? 0) - Number(b.position ?? 0);
          if (byPosition !== 0) return byPosition;
          return String(a.id) < String(b.id) ? -1 : 1;
        });
      const deleted = rows.filter(
        (row) => row.deleted_at !== null && row.deleted_at !== undefined
      );

      const finalPositions = new Map<string, number>();
      active.forEach((row, index) => finalPositions.set(String(row.id), index + 1));
      deleted.forEach((row, index) => finalPositions.set(String(row.id), -(POSITION_OFFSET + index)));

      for (const row of rows) {
        await tx
          .insert(routineExercise)
          .values({
            id: String(row.id),
            routineId,
            exerciseId: String(row.exercise_id),
            position: finalPositions.get(String(row.id)) ?? 1,
            targetSets: numberOrNull(row.target_sets),
            targetReps: numberOrNull(row.target_reps),
            targetWeightKg: numberOrNull(row.target_weight_kg),
            restSeconds: numberOrNull(row.rest_seconds),
            notes: stringOrNull(row.notes),
            createdAt: Number(row.created_at ?? row.updated_at ?? 0),
            updatedAt: Number(row.updated_at ?? 0),
            deletedAt: numberOrNull(row.deleted_at),
          })
          .onConflictDoUpdate({
            target: routineExercise.id,
            set: {
              routineId,
              exerciseId: String(row.exercise_id),
              position: finalPositions.get(String(row.id)) ?? 1,
              targetSets: numberOrNull(row.target_sets),
              targetReps: numberOrNull(row.target_reps),
              targetWeightKg: numberOrNull(row.target_weight_kg),
              restSeconds: numberOrNull(row.rest_seconds),
              notes: stringOrNull(row.notes),
              updatedAt: Number(row.updated_at ?? 0),
              deletedAt: numberOrNull(row.deleted_at),
            },
          });
      }
    }
  });

  return merged.summary;
}
