import { describe, expect, it } from 'vitest';

import {
  filterExercises,
  normalizeText,
  slugify,
  type CatalogExercise,
  type CatalogFilters,
  type ExerciseKind,
  type MuscleGroup,
} from '@/domain/catalog';
import { isValidUlid } from '@/domain/ulid';

import catalogSeed from '@shared/seed/catalog.json';
import vectors from '@shared/test-vectors/catalog-filters.json';

interface SeedExercise {
  id: string;
  slug: string;
  name: string;
  muscle_group_id: string;
  secondary_muscle_group_id: string | null;
  equipment: string;
  kind: string;
  is_custom: number;
}

const groups: MuscleGroup[] = catalogSeed.muscle_groups.map((group) => ({
  id: group.id,
  slug: group.slug,
  name: group.name,
  bodyRegion: group.body_region,
}));

const exercises: CatalogExercise[] = (catalogSeed.exercises as SeedExercise[]).map((exercise) => ({
  id: exercise.id,
  slug: exercise.slug,
  name: exercise.name,
  muscleGroupId: exercise.muscle_group_id,
  secondaryMuscleGroupId: exercise.secondary_muscle_group_id,
  equipment: exercise.equipment,
  kind: exercise.kind as ExerciseKind,
  isCustom: exercise.is_custom === 1,
}));

describe('integridad del catalogo base', () => {
  it('tiene al menos 10 grupos musculares y 25 ejercicios', () => {
    expect(groups.length).toBeGreaterThanOrEqual(10);
    expect(exercises.length).toBeGreaterThanOrEqual(25);
  });

  it('usa ULID validos en todos los identificadores', () => {
    for (const id of [...groups.map((group) => group.id), ...exercises.map((item) => item.id)]) {
      expect(isValidUlid(id), id).toBe(true);
    }
  });

  it('tiene slugs unicos', () => {
    const slugs = [...groups, ...exercises].map((entity) => entity.slug);
    expect(new Set(slugs).size).toBe(slugs.length);
  });

  it('referencia grupos musculares existentes', () => {
    const groupIds = new Set(groups.map((group) => group.id));
    for (const exercise of exercises) {
      expect(groupIds.has(exercise.muscleGroupId), exercise.slug).toBe(true);
      if (exercise.secondaryMuscleGroupId !== null) {
        expect(groupIds.has(exercise.secondaryMuscleGroupId), exercise.slug).toBe(true);
      }
    }
  });
});

describe('normalizeText', () => {
  it.each(vectors.normalize)('normaliza "$input"', ({ input, expected }) => {
    expect(normalizeText(input)).toBe(expected);
  });
});

describe('slugify', () => {
  it.each(vectors.slugify)('convierte "$input"', ({ input, expected }) => {
    expect(slugify(input)).toBe(expected);
  });
});

describe('filterExercises', () => {
  it.each(vectors.cases)('$name', (testCase) => {
    const filters: CatalogFilters = {
      query: testCase.query,
      muscleGroupSlug: testCase.muscle_group_slug,
      equipment: testCase.equipment,
      kind: testCase.kind as ExerciseKind | null,
    };
    const result = filterExercises(exercises, groups, filters).map((exercise) => exercise.slug);
    expect(result).toEqual(testCase.expected);
  });
});
