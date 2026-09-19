export interface SessionSetInput {
  readonly exerciseId: string;
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly isWarmup: boolean;
}

export interface SessionSummary {
  readonly totalSets: number;
  readonly workingSets: number;
  readonly totalVolumeKg: number;
  readonly volumeByExercise: Record<string, number>;
}

export function summarizeSession(sets: readonly SessionSetInput[]): SessionSummary {
  let workingSets = 0;
  let totalVolumeKg = 0;
  const volumeByExercise: Record<string, number> = {};

  for (const set of sets) {
    if (set.isWarmup) {
      continue;
    }
    workingSets += 1;

    if (set.weightKg === null || set.reps === null) {
      continue;
    }
    const volume = set.weightKg * set.reps;
    totalVolumeKg += volume;
    volumeByExercise[set.exerciseId] = (volumeByExercise[set.exerciseId] ?? 0) + volume;
  }

  return {
    totalSets: sets.length,
    workingSets,
    totalVolumeKg,
    volumeByExercise,
  };
}

export function formatDuration(startedAt: number, finishedAt: number | null): string | null {
  if (finishedAt === null) {
    return null;
  }
  const totalMinutes = Math.max(0, Math.round((finishedAt - startedAt) / 60000));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) {
    return `${minutes} min`;
  }
  return `${hours} h ${minutes} min`;
}
