import { and, desc, eq, isNull } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import { bodyMetric } from '@/db/schema';
import { generateUlid } from '@/domain/ulid';
import {
  unitForKind,
  validateMetric,
  type BodyMetricPoint,
  type MetricKind,
} from '@/domain/body';

export class BodyMetricError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'BodyMetricError';
  }
}

export interface BodyMetricInput {
  readonly kind: string;
  readonly value: number;
  readonly measuredAtMs: number;
  readonly notes: string | null;
}

export interface BodyMetricUpdate {
  readonly value: number;
  readonly measuredAtMs: number;
  readonly notes: string | null;
}

export async function listBodyMetrics(db: FitLogDb): Promise<BodyMetricPoint[]> {
  const rows = await db
    .select()
    .from(bodyMetric)
    .where(isNull(bodyMetric.deletedAt))
    .orderBy(desc(bodyMetric.measuredAt));

  return rows.map((row) => ({
    id: row.id,
    kind: row.kind as MetricKind,
    measuredAtMs: row.measuredAt,
    value: row.value,
    unit: row.unit as BodyMetricPoint['unit'],
    notes: row.notes,
  }));
}

export async function createBodyMetric(
  db: FitLogDb,
  input: BodyMetricInput,
  now: () => number = Date.now
): Promise<BodyMetricPoint> {
  const error = validateMetric(input.kind, input.value);
  if (error !== null) {
    throw new BodyMetricError(error);
  }

  const kind = input.kind as MetricKind;
  const unit = unitForKind(kind);
  const timestamp = now();
  const id = generateUlid(timestamp);

  await db.insert(bodyMetric).values({
    id,
    measuredAt: input.measuredAtMs,
    kind,
    value: input.value,
    unit,
    notes: input.notes?.trim() ? input.notes.trim() : null,
    createdAt: timestamp,
    updatedAt: timestamp,
    deletedAt: null,
  });

  return {
    id,
    kind,
    measuredAtMs: input.measuredAtMs,
    value: input.value,
    unit,
    notes: input.notes?.trim() ? input.notes.trim() : null,
  };
}

export async function updateBodyMetric(
  db: FitLogDb,
  id: string,
  input: BodyMetricUpdate,
  now: () => number = Date.now
): Promise<void> {
  const rows = await db
    .select({ id: bodyMetric.id, kind: bodyMetric.kind })
    .from(bodyMetric)
    .where(and(eq(bodyMetric.id, id), isNull(bodyMetric.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new BodyMetricError('La medida no existe');
  }

  const error = validateMetric(found.kind, input.value);
  if (error !== null) {
    throw new BodyMetricError(error);
  }

  await db
    .update(bodyMetric)
    .set({
      value: input.value,
      measuredAt: input.measuredAtMs,
      notes: input.notes?.trim() ? input.notes.trim() : null,
      updatedAt: now(),
    })
    .where(eq(bodyMetric.id, id));
}

export async function deleteBodyMetric(
  db: FitLogDb,
  id: string,
  now: () => number = Date.now
): Promise<void> {
  const rows = await db
    .select({ id: bodyMetric.id })
    .from(bodyMetric)
    .where(and(eq(bodyMetric.id, id), isNull(bodyMetric.deletedAt)));

  if (rows.length === 0) {
    throw new BodyMetricError('La medida no existe');
  }

  const timestamp = now();
  await db
    .update(bodyMetric)
    .set({ deletedAt: timestamp, updatedAt: timestamp })
    .where(eq(bodyMetric.id, id));
}
