import { and, asc, desc, eq, inArray, isNull, sql } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import { exercise, routine, session, setEntry } from '@/db/schema';
import { generateUlid } from '@/domain/ulid';
import { summarizeSession, type SessionSummary } from '@/domain/workout';

export type WorkoutErrorCode =
  | 'invalid_input'
  | 'exercise_not_found'
  | 'session_not_found'
  | 'session_not_active'
  | 'session_already_active'
  | 'set_not_found'
  | 'routine_not_found';

export class WorkoutError extends Error {
  readonly code: WorkoutErrorCode;

  constructor(code: WorkoutErrorCode, message: string) {
    super(message);
    this.name = 'WorkoutError';
    this.code = code;
  }
}

export interface WorkoutSet {
  readonly id: string;
  readonly sessionId: string;
  readonly exerciseId: string;
  readonly exerciseName: string;
  readonly setIndex: number;
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly rir: number | null;
  readonly isWarmup: boolean;
  readonly notes: string | null;
  /** Marca de tiempo del registro: sirve para el tiempo de descanso desde la ultima serie. */
  readonly createdAtMs: number;
}

export interface WorkoutSession {
  readonly id: string;
  readonly startedAt: number;
  readonly finishedAt: number | null;
  readonly notes: string | null;
  readonly routineId: string | null;
  readonly routineName: string | null;
  readonly summary: SessionSummary;
  /** Metricas de una sesion importada; en una sesion propia queda en `null`. */
  readonly activity: ImportedActivity | null;
}

export interface SessionDetail {
  readonly session: WorkoutSession;
  readonly sets: WorkoutSet[];
}

export interface AddSetInput {
  readonly sessionId: string;
  readonly exerciseId: string;
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly rir: number | null;
  readonly notes: string | null;
  readonly isWarmup: boolean;
}

export interface UpdateSetInput {
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly rir: number | null;
  readonly notes: string | null;
}

type SetRow = {
  id: string;
  sessionId: string;
  exerciseId: string;
  exerciseName: string;
  setIndex: number;
  weightKg: number | null;
  reps: number | null;
  rir: number | null;
  isWarmup: number;
  notes: string | null;
  createdAt: number;
};

function toWorkoutSet(row: SetRow): WorkoutSet {
  return {
    id: row.id,
    sessionId: row.sessionId,
    exerciseId: row.exerciseId,
    exerciseName: row.exerciseName,
    setIndex: row.setIndex,
    weightKg: row.weightKg,
    reps: row.reps,
    rir: row.rir,
    isWarmup: row.isWarmup === 1,
    notes: row.notes,
    createdAtMs: row.createdAt,
  };
}

async function loadSets(db: FitLogDb, sessionId: string): Promise<SetRow[]> {
  return db
    .select({
      id: setEntry.id,
      sessionId: setEntry.sessionId,
      exerciseId: setEntry.exerciseId,
      exerciseName: exercise.name,
      setIndex: setEntry.setIndex,
      weightKg: setEntry.weightKg,
      reps: setEntry.reps,
      rir: setEntry.rir,
      isWarmup: setEntry.isWarmup,
      notes: setEntry.notes,
      createdAt: setEntry.createdAt,
    })
    .from(setEntry)
    .innerJoin(exercise, eq(setEntry.exerciseId, exercise.id))
    .where(and(eq(setEntry.sessionId, sessionId), isNull(setEntry.deletedAt)))
    .orderBy(asc(exercise.name), asc(setEntry.setIndex));
}

async function routineNamesByIds(
  db: FitLogDb,
  ids: readonly string[]
): Promise<Map<string, string>> {
  const unique = [...new Set(ids)];
  if (unique.length === 0) {
    return new Map();
  }
  const rows = await db
    .select({ id: routine.id, name: routine.name })
    .from(routine)
    .where(and(inArray(routine.id, unique), isNull(routine.deletedAt)));
  return new Map(rows.map((row) => [row.id, row.name]));
}

function toSession(
  row: {
    id: string;
    startedAt: number;
    finishedAt: number | null;
    notes: string | null;
    routineId: string | null;
    distanceM?: number | null;
    calories?: number | null;
    avgHeartRate?: number | null;
    maxHeartRate?: number | null;
    steps?: number | null;
    elevationGainM?: number | null;
    source?: string | null;
  },
  sets: readonly SetRow[],
  routineName: string | null = null
): WorkoutSession {
  return {
    id: row.id,
    startedAt: row.startedAt,
    finishedAt: row.finishedAt,
    notes: row.notes,
    routineId: row.routineId,
    routineName,
    activity: toActivity(row),
    summary: summarizeSession(
      sets.map((set) => ({
        exerciseId: set.exerciseId,
        weightKg: set.weightKg,
        reps: set.reps,
        isWarmup: set.isWarmup === 1,
      }))
    ),
  };
}

export async function getActiveSession(db: FitLogDb): Promise<WorkoutSession | null> {
  const rows = await db
    .select()
    .from(session)
    .where(and(isNull(session.finishedAt), isNull(session.deletedAt)))
    .orderBy(desc(session.startedAt));

  const active = rows[0];
  if (!active) {
    return null;
  }
  const names = await routineNamesByIds(db, active.routineId ? [active.routineId] : []);
  return toSession(
    active,
    await loadSets(db, active.id),
    active.routineId ? names.get(active.routineId) ?? null : null
  );
}

export async function startSession(
  db: FitLogDb,
  routineId: string | null = null,
  now: () => number = Date.now
): Promise<WorkoutSession> {
  const active = await getActiveSession(db);
  if (active) {
    throw new WorkoutError('session_already_active', 'Ya hay una sesión de entrenamiento en curso');
  }

  let routineName: string | null = null;
  if (routineId !== null) {
    const names = await routineNamesByIds(db, [routineId]);
    routineName = names.get(routineId) ?? null;
    if (routineName === null) {
      throw new WorkoutError('routine_not_found', 'La rutina no existe');
    }
  }

  const timestamp = now();
  const created = {
    id: generateUlid(timestamp),
    startedAt: timestamp,
    finishedAt: null,
    notes: null,
    routineId,
  };

  await db.insert(session).values({
    ...created,
    createdAt: timestamp,
    updatedAt: timestamp,
    deletedAt: null,
  });

  return toSession(created, [], routineName);
}

export async function finishSession(
  db: FitLogDb,
  sessionId: string,
  now: () => number = Date.now
): Promise<void> {
  const rows = await db
    .select({ id: session.id, finishedAt: session.finishedAt })
    .from(session)
    .where(and(eq(session.id, sessionId), isNull(session.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new WorkoutError('session_not_found', 'La sesión no existe');
  }
  if (found.finishedAt !== null) {
    throw new WorkoutError('session_not_active', 'La sesión ya está finalizada');
  }

  const timestamp = now();
  await db
    .update(session)
    .set({ finishedAt: timestamp, updatedAt: timestamp })
    .where(eq(session.id, sessionId));
}

function validateSetInput(input: {
  weightKg: number | null;
  reps: number | null;
  rir: number | null;
}): void {
  if (input.weightKg !== null && (!Number.isFinite(input.weightKg) || input.weightKg < 0)) {
    throw new WorkoutError('invalid_input', 'El peso debe ser un número mayor o igual a 0');
  }
  if (input.reps !== null && (!Number.isInteger(input.reps) || input.reps < 0)) {
    throw new WorkoutError('invalid_input', 'Las repeticiones deben ser un entero mayor o igual a 0');
  }
  if (input.rir !== null && (!Number.isInteger(input.rir) || input.rir < 0 || input.rir > 10)) {
    throw new WorkoutError('invalid_input', 'El RIR debe ser un entero entre 0 y 10');
  }
}

export async function addSet(
  db: FitLogDb,
  input: AddSetInput,
  now: () => number = Date.now
): Promise<WorkoutSet> {
  validateSetInput(input);

  const sessions = await db
    .select({ id: session.id, finishedAt: session.finishedAt })
    .from(session)
    .where(and(eq(session.id, input.sessionId), isNull(session.deletedAt)));

  const targetSession = sessions[0];
  if (!targetSession) {
    throw new WorkoutError('session_not_found', 'La sesión no existe');
  }
  if (targetSession.finishedAt !== null) {
    throw new WorkoutError('session_not_active', 'La sesión ya está finalizada');
  }

  const exercises = await db
    .select({ id: exercise.id, name: exercise.name })
    .from(exercise)
    .where(and(eq(exercise.id, input.exerciseId), isNull(exercise.deletedAt)));

  const targetExercise = exercises[0];
  if (!targetExercise) {
    throw new WorkoutError('exercise_not_found', 'El ejercicio no está disponible');
  }

  const maxRows = await db
    .select({ maxIndex: sql<number>`coalesce(max(${setEntry.setIndex}), 0)` })
    .from(setEntry)
    .where(
      and(
        eq(setEntry.sessionId, input.sessionId),
        eq(setEntry.exerciseId, input.exerciseId),
        isNull(setEntry.deletedAt)
      )
    );

  const setIndex = Number(maxRows[0]?.maxIndex ?? 0) + 1;
  const timestamp = now();
  const id = generateUlid(timestamp);

  await db.insert(setEntry).values({
    id,
    sessionId: input.sessionId,
    exerciseId: input.exerciseId,
    setIndex,
    weightKg: input.weightKg,
    reps: input.reps,
    rir: input.rir,
    rpe: null,
    isWarmup: input.isWarmup ? 1 : 0,
    notes: input.notes,
    createdAt: timestamp,
    updatedAt: timestamp,
    deletedAt: null,
  });

  return toWorkoutSet({
    id,
    sessionId: input.sessionId,
    exerciseId: input.exerciseId,
    exerciseName: targetExercise.name,
    setIndex,
    weightKg: input.weightKg,
    reps: input.reps,
    createdAt: timestamp,
    rir: input.rir,
    isWarmup: input.isWarmup ? 1 : 0,
    notes: input.notes,
  });
}

export async function updateSet(
  db: FitLogDb,
  setId: string,
  input: UpdateSetInput,
  now: () => number = Date.now
): Promise<void> {
  validateSetInput(input);

  const rows = await db
    .select({ id: setEntry.id })
    .from(setEntry)
    .where(and(eq(setEntry.id, setId), isNull(setEntry.deletedAt)));

  if (rows.length === 0) {
    throw new WorkoutError('set_not_found', 'La serie no existe');
  }

  await db
    .update(setEntry)
    .set({
      weightKg: input.weightKg,
      reps: input.reps,
      rir: input.rir,
      notes: input.notes,
      updatedAt: now(),
    })
    .where(eq(setEntry.id, setId));
}

export async function deleteSet(db: FitLogDb, setId: string, now: () => number = Date.now): Promise<void> {
  const rows = await db
    .select({ id: setEntry.id })
    .from(setEntry)
    .where(and(eq(setEntry.id, setId), isNull(setEntry.deletedAt)));

  if (rows.length === 0) {
    throw new WorkoutError('set_not_found', 'La serie no existe');
  }

  const timestamp = now();
  await db
    .update(setEntry)
    .set({ deletedAt: timestamp, updatedAt: timestamp })
    .where(eq(setEntry.id, setId));
}

/** Metricas que trae una sesion importada de otra app (Huawei Health o GPX). */
export interface ImportedActivity {
  readonly distanceM: number | null;
  readonly calories: number | null;
  readonly averageHeartRate: number | null;
  readonly maxHeartRate: number | null;
  readonly steps: number | null;
  readonly elevationGainM: number | null;
  readonly source: string | null;
}

/** Entrenamiento a importar desde otra app: fechas, nota y metricas ya resueltas. */
export interface ImportedSession {
  readonly startedAtMs: number;
  readonly finishedAtMs: number | null;
  readonly notes: string | null;
  readonly activity?: ImportedActivity | null;
}

export interface ImportSessionsResult {
  readonly imported: number;
  readonly skipped: number;
}

/**
 * Importa entrenamientos de otra app como sesiones, salteando los que ya existen.
 *
 * La clave es la fecha de inicio: dos entrenamientos no empiezan en el mismo milisegundo, asi que
 * repetir la importacion no duplica nada y no hace falta guardar el id de la app de origen.
 */
export async function importSessions(
  db: FitLogDb,
  sessions: readonly ImportedSession[],
  now: () => number = Date.now
): Promise<ImportSessionsResult> {
  if (sessions.length === 0) return { imported: 0, skipped: 0 };

  const existingRows = await db
    .select({ startedAt: session.startedAt })
    .from(session)
    .where(isNull(session.deletedAt));
  const existing = new Set(existingRows.map((row) => row.startedAt));

  const timestamp = now();
  let imported = 0;
  let skipped = 0;

  for (const draft of [...sessions].sort((a, b) => a.startedAtMs - b.startedAtMs)) {
    if (existing.has(draft.startedAtMs)) {
      skipped += 1;
      continue;
    }
    existing.add(draft.startedAtMs);
    await db.insert(session).values({
      id: generateUlid(timestamp),
      routineId: null,
      startedAt: draft.startedAtMs,
      finishedAt: draft.finishedAtMs,
      notes: draft.notes,
      distanceM: draft.activity?.distanceM ?? null,
      calories: draft.activity?.calories ?? null,
      avgHeartRate: draft.activity?.averageHeartRate ?? null,
      maxHeartRate: draft.activity?.maxHeartRate ?? null,
      steps: draft.activity?.steps ?? null,
      elevationGainM: draft.activity?.elevationGainM ?? null,
      source: draft.activity?.source ?? null,
      createdAt: timestamp,
      updatedAt: timestamp,
      deletedAt: null,
    });
    imported += 1;
  }

  return { imported, skipped };
}

/** Metricas de actividad de una fila de sesion, o `null` si no tiene ninguna. */
function toActivity(row: {
  distanceM?: number | null;
  calories?: number | null;
  avgHeartRate?: number | null;
  maxHeartRate?: number | null;
  steps?: number | null;
  elevationGainM?: number | null;
  source?: string | null;
}): ImportedActivity | null {
  const activity: ImportedActivity = {
    distanceM: row.distanceM ?? null,
    calories: row.calories ?? null,
    averageHeartRate: row.avgHeartRate ?? null,
    maxHeartRate: row.maxHeartRate ?? null,
    steps: row.steps ?? null,
    elevationGainM: row.elevationGainM ?? null,
    source: row.source ?? null,
  };
  const empty =
    activity.distanceM === null &&
    activity.calories === null &&
    activity.averageHeartRate === null &&
    activity.maxHeartRate === null &&
    activity.steps === null &&
    activity.elevationGainM === null;
  return empty ? null : activity;
}

export async function listSessions(db: FitLogDb): Promise<WorkoutSession[]> {
  const sessionRows = await db
    .select()
    .from(session)
    .where(isNull(session.deletedAt))
    .orderBy(desc(session.startedAt));

  if (sessionRows.length === 0) {
    return [];
  }

  const setRows = await db
    .select({
      id: setEntry.id,
      sessionId: setEntry.sessionId,
      exerciseId: setEntry.exerciseId,
      exerciseName: exercise.name,
      setIndex: setEntry.setIndex,
      weightKg: setEntry.weightKg,
      reps: setEntry.reps,
      rir: setEntry.rir,
      isWarmup: setEntry.isWarmup,
      notes: setEntry.notes,
      createdAt: setEntry.createdAt,
    })
    .from(setEntry)
    .innerJoin(exercise, eq(setEntry.exerciseId, exercise.id))
    .where(isNull(setEntry.deletedAt))
    .orderBy(asc(exercise.name), asc(setEntry.setIndex));

  const bySession = new Map<string, SetRow[]>();
  for (const row of setRows) {
    const bucket = bySession.get(row.sessionId);
    if (bucket) {
      bucket.push(row);
    } else {
      bySession.set(row.sessionId, [row]);
    }
  }

  const names = await routineNamesByIds(
    db,
    sessionRows.map((row) => row.routineId).filter((id): id is string => id !== null)
  );

  return sessionRows.map((row) =>
    toSession(
      row,
      bySession.get(row.id) ?? [],
      row.routineId ? names.get(row.routineId) ?? null : null
    )
  );
}

export async function getSessionDetail(db: FitLogDb, sessionId: string): Promise<SessionDetail> {
  const rows = await db
    .select()
    .from(session)
    .where(and(eq(session.id, sessionId), isNull(session.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new WorkoutError('session_not_found', 'La sesión no existe');
  }

  const names = await routineNamesByIds(db, found.routineId ? [found.routineId] : []);
  const sets = await loadSets(db, sessionId);
  return {
    session: toSession(
      found,
      sets,
      found.routineId ? names.get(found.routineId) ?? null : null
    ),
    sets: sets.map(toWorkoutSet),
  };
}
