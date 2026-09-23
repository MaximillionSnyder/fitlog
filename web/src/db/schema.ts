import { integer, real, sqliteTable, text } from 'drizzle-orm/sqlite-core';

export const schemaMigration = sqliteTable('schema_migration', {
  version: integer('version').primaryKey(),
  name: text('name').notNull(),
  appliedAt: integer('applied_at').notNull(),
});

export const muscleGroup = sqliteTable('muscle_group', {
  id: text('id').primaryKey(),
  slug: text('slug').notNull().unique(),
  name: text('name').notNull(),
  bodyRegion: text('body_region').notNull(),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const exercise = sqliteTable('exercise', {
  id: text('id').primaryKey(),
  slug: text('slug').notNull().unique(),
  name: text('name').notNull(),
  muscleGroupId: text('muscle_group_id').notNull(),
  secondaryMuscleGroupId: text('secondary_muscle_group_id'),
  equipment: text('equipment').notNull(),
  kind: text('kind').notNull(),
  isCustom: integer('is_custom').notNull().default(0),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const routine = sqliteTable('routine', {
  id: text('id').primaryKey(),
  name: text('name').notNull(),
  description: text('description'),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const routineExercise = sqliteTable('routine_exercise', {
  id: text('id').primaryKey(),
  routineId: text('routine_id').notNull(),
  exerciseId: text('exercise_id').notNull(),
  position: integer('position').notNull(),
  targetSets: integer('target_sets'),
  targetReps: integer('target_reps'),
  targetWeightKg: real('target_weight_kg'),
  restSeconds: integer('rest_seconds'),
  notes: text('notes'),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const session = sqliteTable('session', {
  id: text('id').primaryKey(),
  routineId: text('routine_id'),
  startedAt: integer('started_at').notNull(),
  finishedAt: integer('finished_at'),
  notes: text('notes'),
  // Metricas de una sesion importada (Huawei Health o GPX): opcionales.
  distanceM: real('distance_m'),
  calories: real('calories'),
  avgHeartRate: real('avg_heart_rate'),
  maxHeartRate: real('max_heart_rate'),
  steps: integer('steps'),
  elevationGainM: real('elevation_gain_m'),
  source: text('source'),
  /** Ruta del recorrido en texto compacto (solo GPX). */
  route: text('route'),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const setEntry = sqliteTable('set_entry', {
  id: text('id').primaryKey(),
  sessionId: text('session_id').notNull(),
  exerciseId: text('exercise_id').notNull(),
  setIndex: integer('set_index').notNull(),
  weightKg: real('weight_kg'),
  reps: integer('reps'),
  rir: integer('rir'),
  rpe: real('rpe'),
  isWarmup: integer('is_warmup').notNull().default(0),
  notes: text('notes'),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const bodyMetric = sqliteTable('body_metric', {
  id: text('id').primaryKey(),
  measuredAt: integer('measured_at').notNull(),
  kind: text('kind').notNull(),
  value: real('value').notNull(),
  unit: text('unit').notNull(),
  notes: text('notes'),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
  deletedAt: integer('deleted_at'),
});

export const appSetting = sqliteTable('app_setting', {
  key: text('key').primaryKey(),
  value: text('value').notNull(),
  updatedAt: integer('updated_at').notNull(),
});

export const schema = {
  schemaMigration,
  muscleGroup,
  exercise,
  routine,
  routineExercise,
  session,
  setEntry,
  bodyMetric,
  appSetting,
};
