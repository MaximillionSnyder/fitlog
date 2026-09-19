import { and, asc, eq, isNull } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import { session, setEntry } from '@/db/schema';
import { buildProgressSeries, type ProgressPoint, type ProgressRange } from '@/domain/progress';

export async function loadProgressSeries(
  db: FitLogDb,
  exerciseId: string,
  range: ProgressRange | null
): Promise<ProgressPoint[]> {
  const rows = await db
    .select({
      sessionId: setEntry.sessionId,
      startedAtMs: session.startedAt,
      weightKg: setEntry.weightKg,
      reps: setEntry.reps,
      isWarmup: setEntry.isWarmup,
    })
    .from(setEntry)
    .innerJoin(session, eq(setEntry.sessionId, session.id))
    .where(
      and(
        eq(setEntry.exerciseId, exerciseId),
        isNull(setEntry.deletedAt),
        isNull(session.deletedAt)
      )
    )
    .orderBy(asc(session.startedAt));

  return buildProgressSeries(
    rows.map((row) => ({
      sessionId: row.sessionId,
      startedAtMs: row.startedAtMs,
      weightKg: row.weightKg,
      reps: row.reps,
      isWarmup: row.isWarmup === 1,
    })),
    range
  );
}
