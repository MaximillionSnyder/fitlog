import { and, asc, eq, isNull, sql } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import { exercise, routine, routineExercise } from '@/db/schema';
import { generateUlid } from '@/domain/ulid';
import { assignPositions, moveItem, RoutineOrderError } from '@/domain/routines';

export type RoutineErrorCode =
  | 'invalid_input'
  | 'routine_not_found'
  | 'exercise_not_found'
  | 'exercise_already_in_routine'
  | 'routine_exercise_not_found'
  | 'invalid_move';

export class RoutineError extends Error {
  readonly code: RoutineErrorCode;

  constructor(code: RoutineErrorCode, message: string) {
    super(message);
    this.name = 'RoutineError';
    this.code = code;
  }
}

export interface RoutineExercise {
  readonly id: string;
  readonly routineId: string;
  readonly exerciseId: string;
  readonly exerciseName: string;
  readonly position: number;
  readonly targetSets: number | null;
  readonly targetReps: number | null;
  readonly targetWeightKg: number | null;
  readonly restSeconds: number | null;
  readonly notes: string | null;
}

export interface Routine {
  readonly id: string;
  readonly name: string;
  readonly description: string | null;
  readonly exercises: RoutineExercise[];
}

export interface RoutineExerciseInput {
  readonly exerciseId: string;
  readonly targetSets: number | null;
  readonly targetReps: number | null;
  readonly targetWeightKg: number | null;
  readonly restSeconds: number | null;
  readonly notes: string | null;
}

interface RoutineExerciseRow {
  id: string;
  routineId: string;
  exerciseId: string;
  exerciseName: string;
  position: number;
  targetSets: number | null;
  targetReps: number | null;
  targetWeightKg: number | null;
  restSeconds: number | null;
  notes: string | null;
}

function toRoutineExercise(row: RoutineExerciseRow): RoutineExercise {
  return {
    id: row.id,
    routineId: row.routineId,
    exerciseId: row.exerciseId,
    exerciseName: row.exerciseName,
    position: row.position,
    targetSets: row.targetSets,
    targetReps: row.targetReps,
    targetWeightKg: row.targetWeightKg,
    restSeconds: row.restSeconds,
    notes: row.notes,
  };
}

async function loadRoutineExercises(db: FitLogDb, routineId: string): Promise<RoutineExercise[]> {
  const rows = await db
    .select({
      id: routineExercise.id,
      routineId: routineExercise.routineId,
      exerciseId: routineExercise.exerciseId,
      exerciseName: exercise.name,
      position: routineExercise.position,
      targetSets: routineExercise.targetSets,
      targetReps: routineExercise.targetReps,
      targetWeightKg: routineExercise.targetWeightKg,
      restSeconds: routineExercise.restSeconds,
      notes: routineExercise.notes,
    })
    .from(routineExercise)
    .innerJoin(exercise, eq(routineExercise.exerciseId, exercise.id))
    .where(and(eq(routineExercise.routineId, routineId), isNull(routineExercise.deletedAt)))
    .orderBy(asc(routineExercise.position));

  return rows.map(toRoutineExercise);
}

async function requireRoutine(
  db: FitLogDb,
  routineId: string
): Promise<{ id: string; name: string; description: string | null }> {
  const rows = await db
    .select({ id: routine.id, name: routine.name, description: routine.description })
    .from(routine)
    .where(and(eq(routine.id, routineId), isNull(routine.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new RoutineError('routine_not_found', 'La rutina no existe');
  }
  return found;
}

function validateName(name: string): string {
  const trimmed = name.trim();
  if (trimmed === '') {
    throw new RoutineError('invalid_input', 'El nombre de la rutina no puede estar vacío');
  }
  return trimmed;
}

export async function listRoutines(db: FitLogDb): Promise<Routine[]> {
  const routines = await db
    .select()
    .from(routine)
    .where(isNull(routine.deletedAt))
    .orderBy(asc(routine.name));

  const result: Routine[] = [];
  for (const item of routines) {
    result.push({
      id: item.id,
      name: item.name,
      description: item.description,
      exercises: await loadRoutineExercises(db, item.id),
    });
  }
  return result;
}

export async function getRoutine(db: FitLogDb, routineId: string): Promise<Routine> {
  const found = await requireRoutine(db, routineId);
  return {
    ...found,
    exercises: await loadRoutineExercises(db, routineId),
  };
}

export async function createRoutine(
  db: FitLogDb,
  input: { name: string; description: string | null },
  now: () => number = Date.now
): Promise<Routine> {
  const name = validateName(input.name);
  const timestamp = now();
  const id = generateUlid(timestamp);

  await db.insert(routine).values({
    id,
    name,
    description: input.description?.trim() || null,
    createdAt: timestamp,
    updatedAt: timestamp,
    deletedAt: null,
  });

  return { id, name, description: input.description?.trim() || null, exercises: [] };
}

export async function updateRoutine(
  db: FitLogDb,
  routineId: string,
  input: { name: string; description: string | null },
  now: () => number = Date.now
): Promise<void> {
  await requireRoutine(db, routineId);
  const name = validateName(input.name);

  await db
    .update(routine)
    .set({
      name,
      description: input.description?.trim() || null,
      updatedAt: now(),
    })
    .where(eq(routine.id, routineId));
}

export async function deleteRoutine(
  db: FitLogDb,
  routineId: string,
  now: () => number = Date.now
): Promise<void> {
  await requireRoutine(db, routineId);
  const timestamp = now();

  await db.transaction(async (tx) => {
    await tx
      .update(routineExercise)
      .set({ deletedAt: timestamp, updatedAt: timestamp })
      .where(and(eq(routineExercise.routineId, routineId), isNull(routineExercise.deletedAt)))
      .run();
    await tx
      .update(routine)
      .set({ deletedAt: timestamp, updatedAt: timestamp })
      .where(eq(routine.id, routineId))
      .run();
  });
}

export async function addRoutineExercise(
  db: FitLogDb,
  routineId: string,
  input: RoutineExerciseInput,
  now: () => number = Date.now
): Promise<RoutineExercise> {
  await requireRoutine(db, routineId);

  const exercises = await db
    .select({ id: exercise.id, name: exercise.name })
    .from(exercise)
    .where(and(eq(exercise.id, input.exerciseId), isNull(exercise.deletedAt)));

  const target = exercises[0];
  if (!target) {
    throw new RoutineError('exercise_not_found', 'El ejercicio no está disponible');
  }

  const existing = await db
    .select({ id: routineExercise.id })
    .from(routineExercise)
    .where(
      and(
        eq(routineExercise.routineId, routineId),
        eq(routineExercise.exerciseId, input.exerciseId),
        isNull(routineExercise.deletedAt)
      )
    );

  if (existing.length > 0) {
    throw new RoutineError(
      'exercise_already_in_routine',
      'El ejercicio ya está en la rutina'
    );
  }

  const current = await loadRoutineExercises(db, routineId);
  const timestamp = now();
  const id = generateUlid(timestamp);
  const position = current.length + 1;

  await db.insert(routineExercise).values({
    id,
    routineId,
    exerciseId: input.exerciseId,
    position,
    targetSets: input.targetSets,
    targetReps: input.targetReps,
    targetWeightKg: input.targetWeightKg,
    restSeconds: input.restSeconds,
    notes: input.notes,
    createdAt: timestamp,
    updatedAt: timestamp,
    deletedAt: null,
  });

  return {
    id,
    routineId,
    exerciseId: input.exerciseId,
    exerciseName: target.name,
    position,
    targetSets: input.targetSets,
    targetReps: input.targetReps,
    targetWeightKg: input.targetWeightKg,
    restSeconds: input.restSeconds,
    notes: input.notes,
  };
}

async function renumber(
  db: FitLogDb,
  orderedIds: readonly string[],
  now: () => number
): Promise<void> {
  const timestamp = now();
  const positions = assignPositions(orderedIds);

  await db.transaction(async (tx) => {
    for (const entry of positions) {
      await tx
        .update(routineExercise)
        .set({ position: -(1_000_000 + entry.position), updatedAt: timestamp })
        .where(eq(routineExercise.id, entry.id))
        .run();
    }
    for (const entry of positions) {
      await tx
        .update(routineExercise)
        .set({ position: entry.position, updatedAt: timestamp })
        .where(eq(routineExercise.id, entry.id))
        .run();
    }
  });
}

export async function removeRoutineExercise(
  db: FitLogDb,
  routineExerciseId: string,
  now: () => number = Date.now
): Promise<void> {
  const rows = await db
    .select({ id: routineExercise.id, routineId: routineExercise.routineId })
    .from(routineExercise)
    .where(and(eq(routineExercise.id, routineExerciseId), isNull(routineExercise.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new RoutineError('routine_exercise_not_found', 'El ejercicio no está en la rutina');
  }

  const timestamp = now();
  await db.run(
    sql`UPDATE routine_exercise
        SET position = (SELECT COALESCE(MIN(position), 0) - 1 FROM routine_exercise WHERE routine_id = ${found.routineId}),
            deleted_at = ${timestamp},
            updated_at = ${timestamp}
        WHERE id = ${routineExerciseId}`
  );

  const remaining = await loadRoutineExercises(db, found.routineId);
  await renumber(db, remaining.map((item) => item.id), now);
}

export async function moveRoutineExercise(
  db: FitLogDb,
  routineExerciseId: string,
  direction: 'up' | 'down',
  now: () => number = Date.now
): Promise<void> {
  const rows = await db
    .select({ id: routineExercise.id, routineId: routineExercise.routineId })
    .from(routineExercise)
    .where(and(eq(routineExercise.id, routineExerciseId), isNull(routineExercise.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new RoutineError('routine_exercise_not_found', 'El ejercicio no está en la rutina');
  }

  const current = await loadRoutineExercises(db, found.routineId);
  const orderedIds = current.map((item) => item.id);
  const fromIndex = orderedIds.indexOf(routineExerciseId);
  const toIndex = direction === 'up' ? fromIndex - 1 : fromIndex + 1;

  let moved: string[];
  try {
    moved = moveItem(orderedIds, fromIndex, toIndex);
  } catch (error) {
    if (error instanceof RoutineOrderError) {
      throw new RoutineError('invalid_move', 'No se puede mover el ejercicio en esa dirección');
    }
    throw error;
  }

  await renumber(db, moved, now);
}
