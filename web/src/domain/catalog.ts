export interface MuscleGroup {
  readonly id: string;
  readonly slug: string;
  readonly name: string;
  readonly bodyRegion: string;
}

export interface CatalogExercise {
  readonly id: string;
  readonly slug: string;
  readonly name: string;
  readonly muscleGroupId: string;
  readonly secondaryMuscleGroupId: string | null;
  readonly equipment: string;
  readonly kind: ExerciseKind;
  readonly isCustom: boolean;
}

export const EXERCISE_KINDS = ['strength', 'cardio', 'mobility'] as const;

export type ExerciseKind = (typeof EXERCISE_KINDS)[number];

export interface CatalogFilters {
  readonly query: string;
  readonly muscleGroupSlug: string | null;
  readonly equipment: string | null;
  readonly kind: ExerciseKind | null;
}

export function normalizeText(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

export function slugify(value: string): string {
  return normalizeText(value)
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

function compareCodeUnits(a: string, b: string): number {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
}

export function filterExercises(
  exercises: readonly CatalogExercise[],
  muscleGroups: readonly MuscleGroup[],
  filters: CatalogFilters
): CatalogExercise[] {
  const slugById = new Map(muscleGroups.map((group) => [group.id, group.slug]));
  const query = normalizeText(filters.query);

  return exercises
    .filter((exercise) => {
      if (query !== '' && !normalizeText(exercise.name).includes(query)) {
        return false;
      }
      if (filters.muscleGroupSlug !== null) {
        const primary = slugById.get(exercise.muscleGroupId);
        const secondary =
          exercise.secondaryMuscleGroupId === null
            ? undefined
            : slugById.get(exercise.secondaryMuscleGroupId);
        if (primary !== filters.muscleGroupSlug && secondary !== filters.muscleGroupSlug) {
          return false;
        }
      }
      if (filters.equipment !== null && exercise.equipment !== filters.equipment) {
        return false;
      }
      if (filters.kind !== null && exercise.kind !== filters.kind) {
        return false;
      }
      return true;
    })
    .sort((a, b) => {
      const byName = compareCodeUnits(normalizeText(a.name), normalizeText(b.name));
      if (byName !== 0) return byName;
      return compareCodeUnits(a.slug, b.slug);
    });
}
