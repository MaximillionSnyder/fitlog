import { and, asc, eq, isNull } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import { exercise, muscleGroup, session, setEntry } from '@/db/schema';
import type { ComparisonSetInput } from '@/domain/comparisons';

export async function loadComparisonSets(db: FitLogDb): Promise<ComparisonSetInput[]> {
  const rows = await db
    .select({
      exerciseId: setEntry.exerciseId,
      sessionId: setEntry.sessionId,
      startedAtMs: session.startedAt,
      weightKg: setEntry.weightKg,
      reps: setEntry.reps,
      isWarmup: setEntry.isWarmup,
    })
    .from(setEntry)
    .innerJoin(session, eq(setEntry.sessionId, session.id))
    .where(and(isNull(setEntry.deletedAt), isNull(session.deletedAt)))
    .orderBy(asc(session.startedAt));

  return rows.map((row) => ({
    exerciseId: row.exerciseId,
    sessionId: row.sessionId,
    startedAtMs: row.startedAtMs,
    weightKg: row.weightKg,
    reps: row.reps,
    isWarmup: row.isWarmup === 1,
  }));
}

export async function loadExerciseGroups(db: FitLogDb): Promise<Record<string, string>> {
  const rows = await db
    .select({ exerciseId: exercise.id, groupSlug: muscleGroup.slug })
    .from(exercise)
    .innerJoin(muscleGroup, eq(exercise.muscleGroupId, muscleGroup.id))
    .where(isNull(exercise.deletedAt));

  return Object.fromEntries(rows.map((row) => [row.exerciseId, row.groupSlug]));
}
