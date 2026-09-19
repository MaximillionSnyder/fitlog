import type { SqlRunner } from '@/db/migrate';

export interface CatalogSeedGroup {
  readonly id: string;
  readonly slug: string;
  readonly name: string;
  readonly body_region: string;
  readonly created_at: number;
  readonly updated_at: number;
  readonly deleted_at: number | null;
}

export interface CatalogSeedExercise {
  readonly id: string;
  readonly slug: string;
  readonly name: string;
  readonly muscle_group_id: string;
  readonly secondary_muscle_group_id: string | null;
  readonly equipment: string;
  readonly kind: string;
  readonly is_custom: number;
  readonly created_at: number;
  readonly updated_at: number;
  readonly deleted_at: number | null;
}

export interface CatalogSeed {
  readonly version: number;
  readonly generated_at_ms: number;
  readonly muscle_groups: readonly CatalogSeedGroup[];
  readonly exercises: readonly CatalogSeedExercise[];
}

export interface SeedResult {
  readonly seeded: boolean;
  readonly muscleGroups: number;
  readonly exercises: number;
}

export function seedCatalog(
  runner: SqlRunner,
  seed: CatalogSeed,
  now: () => number = Date.now
): SeedResult {
  const countRows = runner.all('SELECT COUNT(*) FROM muscle_group');
  const existing = Number(countRows[0]?.[0] ?? 0);

  if (existing > 0) {
    return { seeded: false, muscleGroups: 0, exercises: 0 };
  }

  runner.exec('BEGIN');
  try {
    for (const group of seed.muscle_groups) {
      runner.exec(
        `INSERT INTO muscle_group (id, slug, name, body_region, created_at, updated_at, deleted_at)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [
          group.id,
          group.slug,
          group.name,
          group.body_region,
          group.created_at,
          group.updated_at,
          group.deleted_at,
        ]
      );
    }

    for (const exercise of seed.exercises) {
      runner.exec(
        `INSERT INTO exercise
           (id, slug, name, muscle_group_id, secondary_muscle_group_id, equipment, kind, is_custom, created_at, updated_at, deleted_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          exercise.id,
          exercise.slug,
          exercise.name,
          exercise.muscle_group_id,
          exercise.secondary_muscle_group_id,
          exercise.equipment,
          exercise.kind,
          exercise.is_custom,
          exercise.created_at,
          exercise.updated_at,
          exercise.deleted_at,
        ]
      );
    }

    runner.exec('INSERT INTO app_setting (key, value, updated_at) VALUES (?, ?, ?)', [
      'catalog_seeded_at',
      String(now()),
      now(),
    ]);
    runner.exec('COMMIT');
  } catch (error) {
    runner.exec('ROLLBACK');
    throw error;
  }

  return {
    seeded: true,
    muscleGroups: seed.muscle_groups.length,
    exercises: seed.exercises.length,
  };
}
