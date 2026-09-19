import { and, eq, isNull } from 'drizzle-orm';

import type { FitLogDb } from '@/db/client';
import { exercise, muscleGroup } from '@/db/schema';
import { generateUlid } from '@/domain/ulid';
import { slugify, type CatalogExercise, type ExerciseKind, type MuscleGroup } from '@/domain/catalog';

export type CatalogErrorCode =
  | 'invalid_input'
  | 'duplicate_slug'
  | 'muscle_group_not_found'
  | 'exercise_not_found'
  | 'base_catalog_protected';

export class CatalogError extends Error {
  readonly code: CatalogErrorCode;

  constructor(code: CatalogErrorCode, message: string) {
    super(message);
    this.name = 'CatalogError';
    this.code = code;
  }
}

export interface CatalogSnapshot {
  readonly groups: MuscleGroup[];
  readonly exercises: CatalogExercise[];
}

export interface CreateCustomExerciseInput {
  readonly name: string;
  readonly muscleGroupId: string;
  readonly equipment: string;
  readonly kind: ExerciseKind;
}

export async function loadCatalog(db: FitLogDb): Promise<CatalogSnapshot> {
  const groupRows = await db
    .select()
    .from(muscleGroup)
    .where(isNull(muscleGroup.deletedAt))
    .orderBy(muscleGroup.name);

  const exerciseRows = await db
    .select()
    .from(exercise)
    .where(isNull(exercise.deletedAt))
    .orderBy(exercise.name);

  return {
    groups: groupRows.map((row) => ({
      id: row.id,
      slug: row.slug,
      name: row.name,
      bodyRegion: row.bodyRegion,
    })),
    exercises: exerciseRows.map((row) => ({
      id: row.id,
      slug: row.slug,
      name: row.name,
      muscleGroupId: row.muscleGroupId,
      secondaryMuscleGroupId: row.secondaryMuscleGroupId ?? null,
      equipment: row.equipment,
      kind: row.kind as ExerciseKind,
      isCustom: row.isCustom === 1,
    })),
  };
}

export async function createCustomExercise(
  db: FitLogDb,
  input: CreateCustomExerciseInput,
  now: () => number = Date.now
): Promise<CatalogExercise> {
  const name = input.name.trim();
  if (name === '') {
    throw new CatalogError('invalid_input', 'El nombre del ejercicio no puede estar vacío');
  }

  const group = await db
    .select({ id: muscleGroup.id })
    .from(muscleGroup)
    .where(and(eq(muscleGroup.id, input.muscleGroupId), isNull(muscleGroup.deletedAt)));

  if (group.length === 0) {
    throw new CatalogError('muscle_group_not_found', 'El grupo muscular no existe');
  }

  const slug = slugify(name);
  const existing = await db
    .select({ id: exercise.id })
    .from(exercise)
    .where(eq(exercise.slug, slug));

  if (existing.length > 0) {
    throw new CatalogError('duplicate_slug', 'Ya existe un ejercicio con ese nombre');
  }

  const timestamp = now();
  const created: CatalogExercise = {
    id: generateUlid(timestamp),
    slug,
    name,
    muscleGroupId: input.muscleGroupId,
    secondaryMuscleGroupId: null,
    equipment: input.equipment,
    kind: input.kind,
    isCustom: true,
  };

  try {
    await db.insert(exercise).values({
      id: created.id,
      slug: created.slug,
      name: created.name,
      muscleGroupId: created.muscleGroupId,
      secondaryMuscleGroupId: null,
      equipment: created.equipment,
      kind: created.kind,
      isCustom: 1,
      createdAt: timestamp,
      updatedAt: timestamp,
      deletedAt: null,
    });
  } catch (error) {
    if (error instanceof Error && error.message.includes('UNIQUE')) {
      throw new CatalogError('duplicate_slug', 'Ya existe un ejercicio con ese nombre');
    }
    throw error;
  }

  return created;
}

export async function deleteCustomExercise(
  db: FitLogDb,
  id: string,
  now: () => number = Date.now
): Promise<void> {
  const rows = await db
    .select({ id: exercise.id, isCustom: exercise.isCustom })
    .from(exercise)
    .where(and(eq(exercise.id, id), isNull(exercise.deletedAt)));

  const found = rows[0];
  if (!found) {
    throw new CatalogError('exercise_not_found', 'El ejercicio no existe');
  }
  if (found.isCustom !== 1) {
    throw new CatalogError(
      'base_catalog_protected',
      'Los ejercicios del catálogo base no se pueden eliminar'
    );
  }

  const timestamp = now();
  await db
    .update(exercise)
    .set({ deletedAt: timestamp, updatedAt: timestamp })
    .where(eq(exercise.id, id));
}
