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

/** Serie con su marca de tiempo, para medir el ritmo de la sesion. */
export interface PaceSetInput {
  readonly createdAtMs: number;
  readonly isWarmup: boolean;
}

/**
 * Ritmo de una sesion: cuanto duro de verdad (de la primera a la ultima serie efectiva), a que
 * velocidad se registraron las series y cuanto se descanso en promedio entre ellas.
 *
 * El descanso promedio es el intervalo entre series consecutivas, asi que solo tiene sentido con
 * dos o mas series; el ritmo necesita un rango mayor a cero.
 */
export interface SessionPace {
  readonly firstSetAtMs: number | null;
  readonly lastSetAtMs: number | null;
  readonly spanMs: number;
  readonly workingSets: number;
  readonly setsPerHour: number | null;
  readonly averageRestMs: number | null;
}

export function buildSessionPace(sets: readonly PaceSetInput[]): SessionPace {
  const effective = sets
    .filter((set) => !set.isWarmup)
    .slice()
    .sort((a, b) => a.createdAtMs - b.createdAtMs);

  if (effective.length === 0) {
    return {
      firstSetAtMs: null,
      lastSetAtMs: null,
      spanMs: 0,
      workingSets: 0,
      setsPerHour: null,
      averageRestMs: null,
    };
  }

  const first = effective[0]!.createdAtMs;
  const last = effective[effective.length - 1]!.createdAtMs;
  const span = Math.max(0, last - first);
  const count = effective.length;

  return {
    firstSetAtMs: first,
    lastSetAtMs: last,
    spanMs: span,
    workingSets: count,
    setsPerHour: span > 0 ? (count * 3_600_000) / span : null,
    averageRestMs: count > 1 ? Math.floor(span / (count - 1)) : null,
  };
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
